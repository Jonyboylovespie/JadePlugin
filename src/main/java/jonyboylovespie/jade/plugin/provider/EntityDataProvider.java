package jonyboylovespie.jade.plugin.provider;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import jonyboylovespie.jade.plugin.NbtCompound;

@FunctionalInterface
public interface EntityDataProvider {
    void appendServerData(Player player, Entity entity, NbtCompound tag);
}
