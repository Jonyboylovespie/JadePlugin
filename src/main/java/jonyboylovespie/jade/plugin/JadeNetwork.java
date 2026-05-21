package jonyboylovespie.jade.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import jonyboylovespie.jade.plugin.provider.ServerDataProviderManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.logging.Level;

public class JadeNetwork {

    private static final String PROTOCOL_VERSION = "9";
    private final JadePlugin plugin;
    private final Map<Player, Boolean> connectedPlayers = new HashMap<>();

    public JadeNetwork(JadePlugin plugin) {
        this.plugin = plugin;
    }

    public void handlePacket(String channel, Player player, byte[] data) {
        try {
            switch (channel) {
                case "jade:client_handshake" -> handleHandshake(player, data);
                case "jade:request_block" -> handleBlockRequest(player, data);
                case "jade:request_entity" -> handleEntityRequest(player, data);
                default -> plugin.getLogger().log(Level.FINE, "Unknown Jade channel: {0}", channel);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error on " + channel + ": " + e.getMessage());
        }
    }

    private void handleHandshake(Player player, byte[] data) {
        var in = new ByteArrayInputStream(data);
        String version = PacketBuffer.readUtf(in);
        if (!PROTOCOL_VERSION.equals(version)) {
            plugin.getLogger().warning("Version mismatch from " + player.getName() + ": " + version);
            return;
        }
        try {
            java.lang.reflect.Method addChannel = player.getClass().getMethod("addChannel", String.class);
            addChannel.invoke(player, "jade:server_handshake");
            addChannel.invoke(player, "jade:receive_data");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to force-register channels: " + e.getMessage());
        }
        connectedPlayers.put(player, true);
        sendServerHandshake(player);
    }

    private void sendServerHandshake(Player player) {
        var out = new ByteArrayOutputStream();
        PacketBuffer.writeVarInt(out, 0); // config map (empty)
        PacketBuffer.writeVarInt(out, 0); // shearable blocks (empty)

        List<String> blockIds = List.of(
            "minecraft:brewing_stand", "minecraft:beehive", "minecraft:command_block",
            "minecraft:hopper_lock", "minecraft:redstone",
            "minecraft:mob_spawner.cooldown"
        );
        PacketBuffer.writeVarInt(out, blockIds.size());
        for (String id : blockIds) PacketBuffer.writeUtf(out, id);

        List<String> entityIds = List.of(
            "minecraft:animal_owner", "minecraft:entity_health",
            "minecraft:mob_growth", "minecraft:mob_breeding", "minecraft:waxed",
            "minecraft:zombie_villager"
        );
        PacketBuffer.writeVarInt(out, entityIds.size());
        for (String id : entityIds) PacketBuffer.writeUtf(out, id);

        player.sendPluginMessage(plugin, "jade:server_handshake", out.toByteArray());
        plugin.getLogger().info("Jade handshake OK with " + player.getName());
    }

    private void handleBlockRequest(Player player, byte[] data) {
        if (!connectedPlayers.getOrDefault(player, false)) return;
        if (data.length < 9) return;
        try {
            long blockPos = 0;
            for (int i = 0; i < 8; i++) blockPos = (blockPos << 8) | (data[i + 1] & 0xFF);
            int bx = (int) (blockPos >> 38);
            int by = (int) (blockPos << 52 >> 52);
            int bz = (int) (blockPos << 26 >> 38);

            int[] requestedIds = new int[0];
            NbtCompound requestData = new NbtCompound();

            // Try fixed offsets 20-30, NBT starts after: showDetails(1) + blockPos(8) + direction(1) + floats(12) + isMiss(1) + itemStack(1+N)
            for (int offset = 20; offset <= 32 && offset < data.length; offset++) {
                if (data[offset] == 0x0A || data[offset] == 0x00) {
                    try {
                        NbtCompound candidate = PacketBuffer.readNBT(Arrays.copyOfRange(data, offset, data.length));
                        int nbtSize = countNbtLeadingBytes(data, offset);
                        if (nbtSize < 2 || nbtSize > data.length - offset - 2) continue; // NBT too small or too big
                        int arrayStart = offset + nbtSize;
                        if (arrayStart >= data.length) continue;
                        var idStream = new ByteArrayInputStream(data, arrayStart, data.length - arrayStart);
                        int[] ids = PacketBuffer.readVarIntArray(idStream);
                        // Validate: IDs should be in range [0, 20]
                        boolean valid = true;
                        for (int id : ids) { if (id < 0 || id > 20) { valid = false; break; } }
                        if (!valid) continue;
                        requestData = candidate;
                        requestedIds = ids;
                        break;
                    } catch (Exception ignored) {}
                }
            }

            org.bukkit.block.Block block = player.getWorld().getBlockAt(bx, by, bz);
            requestData.putInt("x", block.getX());
            requestData.putInt("y", block.getY());
            requestData.putInt("z", block.getZ());
            requestData.putString("BlockId", block.getType().getKey().toString());
            ServerDataProviderManager.handleBlockRequest(player, block, requestData, requestedIds);
            sendReceiveData(player, requestData);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Block request error: " + e.getMessage());
        }
    }

    private int countNbtLeadingBytes(byte[] data, int offset) {
        try {
            var dis = new java.io.DataInputStream(new ByteArrayInputStream(data, offset, data.length - offset));
            int type = dis.readUnsignedByte();
            if (type == 0) return 1;
            for (int i = 0; i < 10000; i++) {
                int t = dis.readUnsignedByte();
                if (t == 0) break;
                dis.readUTF();
                skipPayload(dis, t);
            }
            return data.length - offset - dis.available();
        } catch (Exception e) {
            return data.length - offset;
        }
    }

    private void skipPayload(java.io.DataInputStream in, int type) {
        try {
            switch (type) {
                case 1: in.readByte(); break;
                case 2: in.readShort(); break;
                case 3: in.readInt(); break;
                case 4: in.readLong(); break;
                case 5: in.readFloat(); break;
                case 6: in.readDouble(); break;
                case 7: in.readFully(new byte[in.readInt()]); break;
                case 8: in.readUTF(); break;
                case 9: int lt = in.readByte(); int sz = in.readInt(); for (int i = 0; i < sz; i++) skipPayload(in, lt); break;
                case 10: for (int i = 0; i < 10000; i++) { int t = in.readUnsignedByte(); if (t == 0) break; in.readUTF(); skipPayload(in, t); } break;
                case 11: in.readFully(new byte[in.readInt() * 4]); break;
                case 12: in.readFully(new byte[in.readInt() * 8]); break;
            }
        } catch (Exception ignored) {}
    }

    private void handleEntityRequest(Player player, byte[] data) {
        if (!connectedPlayers.getOrDefault(player, false)) return;
        try {
            var in = new ByteArrayInputStream(data);

            in.read();
            int entityId = PacketBuffer.readVarInt(in);
            PacketBuffer.readVarInt(in);
            in.skip(12);

            NbtCompound requestData = PacketBuffer.readNBT(in);
            int[] requestedIds = PacketBuffer.readVarIntArray(in);

            org.bukkit.entity.Entity entity = null;
            for (org.bukkit.entity.Entity e : player.getWorld().getEntities()) {
                if (e.getEntityId() == entityId) { entity = e; break; }
            }
            if (entity == null) return;

            requestData.putInt("EntityId", entityId);
            ServerDataProviderManager.handleEntityRequest(player, entity, requestData, requestedIds);
            sendReceiveData(player, requestData);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Entity request error: " + e.getMessage());
        }
    }

    public void sendReceiveData(Player player, NbtCompound tag) {
        byte[] bytes = PacketBuffer.writeNBT(tag);
        player.sendPluginMessage(plugin, "jade:receive_data", bytes);
    }

    public void removePlayer(Player player) {
        connectedPlayers.remove(player);
    }
}
