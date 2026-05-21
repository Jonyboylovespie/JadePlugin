package jonyboylovespie.jade.plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import jonyboylovespie.jade.plugin.provider.ServerDataProviderManager;
import jonyboylovespie.jade.plugin.provider.vanilla.VanillaProviders;

public final class JadePlugin extends JavaPlugin implements Listener {

    private static JadePlugin instance;
    private JadeNetwork network;

    @Override
    public void onEnable() {
        instance = this;
        this.network = new JadeNetwork(this);

        ServerDataProviderManager.init();
        VanillaProviders.register();

        getServer().getMessenger().registerIncomingPluginChannel(this, "jade:client_handshake",
                (channel, player, message) -> network.handlePacket(channel, player, message));
        getServer().getMessenger().registerIncomingPluginChannel(this, "jade:request_block",
                (channel, player, message) -> network.handlePacket(channel, player, message));
        getServer().getMessenger().registerIncomingPluginChannel(this, "jade:request_entity",
                (channel, player, message) -> network.handlePacket(channel, player, message));

        getServer().getMessenger().registerOutgoingPluginChannel(this, "jade:server_handshake");
        getServer().getMessenger().registerOutgoingPluginChannel(this, "jade:receive_data");

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Jade Plugin enabled - accepting connections via plugin messaging");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "jade:client_handshake");
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "jade:request_block");
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "jade:request_entity");

        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "jade:server_handshake");
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "jade:receive_data");

        instance = null;
        getLogger().info("Jade Plugin disabled");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        network.removePlayer(event.getPlayer());
    }

    public static JadePlugin instance() {
        return instance;
    }

    public JadeNetwork network() {
        return network;
    }
}
