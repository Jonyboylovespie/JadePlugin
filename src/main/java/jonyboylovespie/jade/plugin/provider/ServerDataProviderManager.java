package jonyboylovespie.jade.plugin.provider;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import jonyboylovespie.jade.plugin.NbtCompound;
import jonyboylovespie.jade.plugin.JadePlugin;

import java.util.ArrayList;
import java.util.List;

public final class ServerDataProviderManager {

    private static final List<String> blockProviderIds = new ArrayList<>();
    private static final List<BlockDataProvider> blockProviders = new ArrayList<>();

    private static final List<String> entityProviderIds = new ArrayList<>();
    private static final List<EntityDataProvider> entityProviders = new ArrayList<>();

    public static void init() {
    }

    public static void registerBlockProvider(String id, BlockDataProvider provider) {
        blockProviderIds.add(id);
        blockProviders.add(provider);
    }

    public static void registerEntityProvider(String id, EntityDataProvider provider) {
        entityProviderIds.add(id);
        entityProviders.add(provider);
    }

    public static List<String> blockProviderIds() {
        return blockProviderIds;
    }

    public static List<String> entityProviderIds() {
        return entityProviderIds;
    }

    public static void handleBlockRequest(Player player, Block block, NbtCompound tag, int[] requestedIds) {
        for (int requestedId : requestedIds) {
            if (requestedId >= 0 && requestedId < blockProviders.size()) {
                try {
                    blockProviders.get(requestedId).appendServerData(player, block, tag);
                } catch (Exception e) {
                    JadePlugin.instance().getLogger().warning(
                            "Error from block provider " + blockProviderIds.get(requestedId) + ": " + e.getMessage());
                }
            }
        }
    }

    public static void handleEntityRequest(Player player, Entity entity, NbtCompound tag, int[] requestedIds) {
        for (int requestedId : requestedIds) {
            if (requestedId >= 0 && requestedId < entityProviders.size()) {
                try {
                    entityProviders.get(requestedId).appendServerData(player, entity, tag);
                } catch (Exception e) {
                    JadePlugin.instance().getLogger().warning(
                            "Error from entity provider " + entityProviderIds.get(requestedId) + ": " + e.getMessage());
                }
            }
        }
    }
}
