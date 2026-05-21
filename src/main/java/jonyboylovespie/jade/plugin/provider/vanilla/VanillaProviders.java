package jonyboylovespie.jade.plugin.provider.vanilla;

import org.bukkit.block.Block;
import org.bukkit.entity.*;
import jonyboylovespie.jade.plugin.PacketBuffer;
import jonyboylovespie.jade.plugin.NbtCompound;
import jonyboylovespie.jade.plugin.provider.ServerDataProviderManager;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class VanillaProviders {

    public static void register() {
        ServerDataProviderManager.registerBlockProvider("minecraft:brewing_stand", VanillaProviders::handleBrewingStand);
        ServerDataProviderManager.registerBlockProvider("minecraft:beehive", VanillaProviders::handleBeehive);
        ServerDataProviderManager.registerBlockProvider("minecraft:command_block", VanillaProviders::handleCommandBlock);
        ServerDataProviderManager.registerBlockProvider("minecraft:hopper_lock", VanillaProviders::handleHopperLock);
        ServerDataProviderManager.registerBlockProvider("minecraft:redstone", VanillaProviders::handleRedstone);
        ServerDataProviderManager.registerBlockProvider("minecraft:mob_spawner.cooldown", VanillaProviders::handleMobSpawnerCooldown);

        ServerDataProviderManager.registerEntityProvider("minecraft:animal_owner", VanillaProviders::handleAnimalOwner);
        ServerDataProviderManager.registerEntityProvider("minecraft:entity_health", VanillaProviders::handleEntityHealth);
        ServerDataProviderManager.registerEntityProvider("minecraft:mob_growth", VanillaProviders::handleMobGrowth);
        ServerDataProviderManager.registerEntityProvider("minecraft:mob_breeding", VanillaProviders::handleMobBreeding);
        ServerDataProviderManager.registerEntityProvider("minecraft:waxed", VanillaProviders::handleWaxed);
        ServerDataProviderManager.registerEntityProvider("minecraft:zombie_villager", VanillaProviders::handleZombieVillager);
    }

    private static Object getNmsBlockEntity(Block block) {
        try {
            Object craftWorld = block.getWorld();
            Method getHandle = craftWorld.getClass().getMethod("getHandle");
            Object nmsLevel = getHandle.invoke(craftWorld);
            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            Object blockPos = blockPosClass.getConstructor(int.class, int.class, int.class)
                .newInstance(block.getX(), block.getY(), block.getZ());
            Method getBlockEntity = nmsLevel.getClass().getMethod("getBlockEntity", blockPosClass);
            return getBlockEntity.invoke(nmsLevel, blockPos);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getNmsEntity(Entity entity) {
        try {
            Method getHandle = entity.getClass().getMethod("getHandle");
            return getHandle.invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private static int getIntField(Object obj, String... names) {
        for (String name : names) {
            try { Field f = obj.getClass().getField(name); return (int) f.get(obj); } catch (Exception ignored) {}
            try { Field f = obj.getClass().getDeclaredField(name); f.setAccessible(true); return (int) f.get(obj); } catch (Exception ignored) {}
            String m = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            try { return (int) obj.getClass().getMethod(m).invoke(obj); } catch (Exception ignored) {}
            try { return (int) obj.getClass().getMethod(name).invoke(obj); } catch (Exception ignored) {}
        }
        throw new RuntimeException("Not found: " + String.join(", ", names));
    }

    private static boolean getBoolField(Object obj, String... names) {
        for (String name : names) {
            try { Field f = obj.getClass().getField(name); return (boolean) f.get(obj); } catch (Exception ignored) {}
            try { Field f = obj.getClass().getDeclaredField(name); f.setAccessible(true); return (boolean) f.get(obj); } catch (Exception ignored) {}
            String m = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            try { return (boolean) obj.getClass().getMethod(m).invoke(obj); } catch (Exception ignored) {}
            m = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            try { return (boolean) obj.getClass().getMethod(m).invoke(obj); } catch (Exception ignored) {}
            try { return (boolean) obj.getClass().getMethod(name).invoke(obj); } catch (Exception ignored) {}
        }
        throw new RuntimeException("Not found: " + String.join(", ", names));
    }

    private static Object getField(Object obj, String... names) {
        for (String name : names) {
            try { Field f = obj.getClass().getField(name); return f.get(obj); } catch (Exception ignored) {}
            try { Field f = obj.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(obj); } catch (Exception ignored) {}
            String m = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            try { return obj.getClass().getMethod(m).invoke(obj); } catch (Exception ignored) {}
            try { return obj.getClass().getMethod(name).invoke(obj); } catch (Exception ignored) {}
        }
        throw new RuntimeException("Not found: " + String.join(", ", names));
    }

    private static long getLongField(Object obj, String name) {
        try { Field f = obj.getClass().getField(name); return (long) f.get(obj); } catch (Exception e) {
            try { Field f = obj.getClass().getDeclaredField(name); f.setAccessible(true); return (long) f.get(obj); } catch (Exception e2) {
                throw new RuntimeException("Not found: " + name, e2);
            }
        }
    }

    private static long getStaticLongField(Class<?> clazz, String name) {
        try { Field f = clazz.getField(name); return (long) f.get(null); } catch (Exception e) {
            try { Field f = clazz.getDeclaredField(name); f.setAccessible(true); return (long) f.get(null); } catch (Exception e2) {
                throw new RuntimeException("Not found: " + name, e2);
            }
        }
    }

    private static void writeFloatLE(ByteArrayOutputStream out, float value) {
        int bits = Float.floatToRawIntBits(value);
        out.write(bits & 0xFF);
        out.write((bits >> 8) & 0xFF);
        out.write((bits >> 16) & 0xFF);
        out.write((bits >> 24) & 0xFF);
    }

    // === Block Providers ===

    private static void handleBrewingStand(Player player, Block block, NbtCompound tag) {
        try {
            Object nms = getNmsBlockEntity(block);
            if (nms == null) return;
            int fuel = getIntField(nms, "fuel");
            int time = getIntField(nms, "brewTime");
            var out = new ByteArrayOutputStream();
            PacketBuffer.writeVarInt(out, fuel);
            PacketBuffer.writeVarInt(out, time);
            tag.putByteArray("minecraft:brewing_stand", out.toByteArray());
        } catch (Exception ignored) {}
    }

    private static void handleBeehive(Player player, Block block, NbtCompound tag) {
        try {
            Object nms = getNmsBlockEntity(block);
            if (nms == null) return;
            int bees = getIntField(nms, "occupantCount");
            boolean full = getBoolField(nms, "isFull");
            tag.putByteArray("minecraft:beehive", new byte[] { (byte) (full ? bees : -bees) });
        } catch (Exception ignored) {}
    }

    private static void handleCommandBlock(Player player, Block block, NbtCompound tag) {
        try {
            Object nms = getNmsBlockEntity(block);
            if (nms == null) return;
            Object commandBlock = getField(nms, "commandBlock");
            String command = (String) commandBlock.getClass().getMethod("getCommand").invoke(commandBlock);
            if (command != null && !command.isEmpty()) {
                if (command.length() > 40) command = command.substring(0, 37) + "...";
                var out = new ByteArrayOutputStream();
                PacketBuffer.writeUtf(out, command);
                tag.putByteArray("minecraft:command_block", out.toByteArray());
            }
        } catch (Exception ignored) {}
    }

    private static void handleHopperLock(Player player, Block block, NbtCompound tag) {
        try {
            boolean locked = false;
            if (block.getBlockData() instanceof org.bukkit.block.data.type.Hopper hopper) {
                locked = !hopper.isEnabled();
            }
            tag.putByteArray("minecraft:hopper_lock", new byte[] { (byte) (locked ? 1 : 0) });
        } catch (Exception ignored) {}
    }

    private static void handleRedstone(Player player, Block block, NbtCompound tag) {
        try {
            Object nms = getNmsBlockEntity(block);
            if (nms == null) return;
            int signal = getIntField(nms, "output");
            var out = new ByteArrayOutputStream();
            PacketBuffer.writeVarInt(out, signal);
            tag.putByteArray("minecraft:redstone", out.toByteArray());
        } catch (Exception ignored) {}
    }

    private static void handleMobSpawnerCooldown(Player player, Block block, NbtCompound tag) {
        try {
            Object nms = getNmsBlockEntity(block);
            if (nms == null) return;
            Object trialSpawner = nms.getClass().getMethod("getTrialSpawner").invoke(nms);
            Object stateData = trialSpawner.getClass().getMethod("getStateData").invoke(trialSpawner);
            long end = (long) stateData.getClass().getMethod("cooldownEnd").invoke(stateData);
            long now = player.getWorld().getFullTime();
            int remaining = (int) Math.max(0, end - now);
            var out = new ByteArrayOutputStream();
            PacketBuffer.writeVarInt(out, remaining);
            tag.putByteArray("minecraft:mob_spawner.cooldown", out.toByteArray());
        } catch (Exception ignored) {}
    }

    // === Entity Providers ===

    private static void handleAnimalOwner(Player player, Entity entity, NbtCompound tag) {
        try {
            Object nms = getNmsEntity(entity);
            if (nms == null) return;
            for (Method m : nms.getClass().getMethods()) {
                if (m.getName().equals("getOwnerUUID") && m.getParameterCount() == 0) {
                    Object uuid = m.invoke(nms);
                    if (uuid instanceof java.util.UUID ownerUuid) {
                        org.bukkit.OfflinePlayer owner = org.bukkit.Bukkit.getOfflinePlayer(ownerUuid);
                        String ownerName = owner.getName();
                        if (ownerName == null) ownerName = ownerUuid.toString();
                        String json = "{\"text\":\"" + ownerName.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
                        var out = new ByteArrayOutputStream();
                        PacketBuffer.writeUtf(out, json);
                        tag.putByteArray("minecraft:animal_owner", out.toByteArray());
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void handleEntityHealth(Player player, Entity entity, NbtCompound tag) {
        if (entity instanceof LivingEntity living) {
            float absorption = (float) living.getAbsorptionAmount();
            var out = new ByteArrayOutputStream();
            writeFloatLE(out, absorption);
            tag.putByteArray("minecraft:entity_health", out.toByteArray());
        }
    }

    private static void handleMobGrowth(Player player, Entity entity, NbtCompound tag) {
        if (entity instanceof Ageable ageable) {
            if (ageable.getAge() >= 0) return;
            int time = -ageable.getAge();
            if (time > 0) {
                var out = new ByteArrayOutputStream();
                PacketBuffer.writeVarInt(out, time);
                tag.putByteArray("minecraft:mob_growth", out.toByteArray());
            }
        }
    }

    private static void handleMobBreeding(Player player, Entity entity, NbtCompound tag) {
        if (entity instanceof Animals animal) {
            if (animal.getLoveModeTicks() > 0) {
                var out = new ByteArrayOutputStream();
                PacketBuffer.writeVarInt(out, -1);
                tag.putByteArray("minecraft:mob_breeding", out.toByteArray());
            } else {
                int age = animal.getAge();
                if (age > 0) {
                    var out = new ByteArrayOutputStream();
                    PacketBuffer.writeVarInt(out, age);
                    tag.putByteArray("minecraft:mob_breeding", out.toByteArray());
                }
            }
        }
    }

    private static void handleWaxed(Player player, Entity entity, NbtCompound tag) {
        try {
            Object nms = getNmsEntity(entity);
            if (nms == null) return;
            long nextTick = getLongField(nms, "nextWeatheringTick");
            long ignore = getStaticLongField(nms.getClass(), "IGNORE_WEATHERING_TICK");
            if (nextTick == ignore) {
                tag.putByteArray("minecraft:waxed", new byte[0]);
            }
        } catch (Exception ignored) {}
    }

    private static void handleZombieVillager(Player player, Entity entity, NbtCompound tag) {
        if (entity instanceof ZombieVillager zv) {
            int conversionTime = zv.getConversionTime();
            if (conversionTime > 0) {
                var out = new ByteArrayOutputStream();
                PacketBuffer.writeVarInt(out, conversionTime);
                tag.putByteArray("minecraft:zombie_villager", out.toByteArray());
            }
        }
    }

    private VanillaProviders() {}
}
