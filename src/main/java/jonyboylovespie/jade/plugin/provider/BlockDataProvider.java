package jonyboylovespie.jade.plugin.provider;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import jonyboylovespie.jade.plugin.NbtCompound;

@FunctionalInterface
public interface BlockDataProvider {
    void appendServerData(Player player, Block block, NbtCompound tag);
}
