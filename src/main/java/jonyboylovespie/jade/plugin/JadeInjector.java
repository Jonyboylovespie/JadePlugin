package jonyboylovespie.jade.plugin;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

public class JadeInjector {

    private static final Map<Player, Object> playerConnections = new HashMap<>();

    public static void inject(Player player, JadePlugin plugin) {
        try {
            Object craftPlayer = player;
            Method getHandle = craftPlayer.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(craftPlayer);

            Field connectionField = getField(serverPlayer.getClass(), "connection", "c");
            Object listener = connectionField.get(serverPlayer);

            Field networkManagerField = getField(listener.getClass(), "connection", "c", "h");
            Object connection = networkManagerField.get(listener);

            playerConnections.put(player, connection);
            plugin.getLogger().info("Jade Netty hook installed for " + player.getName());

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to hook Jade Netty for " + player.getName(), e);
        }
    }

    public static void remove(Player player) {
        playerConnections.remove(player);
    }

    public static Object getConnection(Player player) {
        return playerConnections.get(player);
    }

    /** Create a RegistryFriendlyByteBuf using the player's world context */
    public static Object createBuffer(Player player) {
        try {
            // Get registry access from the world
            Object craftWorld = player.getWorld();
            Method getHandle = craftWorld.getClass().getMethod("getHandle");
            Object nmsLevel = getHandle.invoke(craftWorld);
            Method registryAccess = nmsLevel.getClass().getMethod("registryAccess");
            Object regAccess = registryAccess.invoke(nmsLevel);

            // Create RegistryFriendlyByteBuf
            Class<?> rfbClass = Class.forName("net.minecraft.network.RegistryFriendlyByteBuf");
            Class<?> byteBufClass = Class.forName("io.netty.buffer.ByteBuf");
            Class<?> unpooledClass = Class.forName("io.netty.buffer.Unpooled");
            Object buf = unpooledClass.getMethod("buffer").invoke(null);

            // Find constructor
            java.lang.reflect.Constructor<?> ctor = null;
            for (java.lang.reflect.Constructor<?> c : rfbClass.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 2 && params[0].isAssignableFrom(byteBufClass)
                    && params[1].isAssignableFrom(regAccess.getClass())) {
                    ctor = c;
                    break;
                }
            }
            if (ctor == null) return null;
            return ctor.newInstance(buf, regAccess);
        } catch (Exception e) {
            JadePlugin.instance().getLogger().log(Level.WARNING, "Failed to create buffer: " + e.getMessage());
            return null;
        }
    }

    private static Field getField(Class<?> clazz, String... names) {
        for (String name : names) {
            try { Field f = clazz.getField(name); f.setAccessible(true); return f; } catch (Exception ignored) {}
            try { Field f = clazz.getDeclaredField(name); f.setAccessible(true); return f; } catch (Exception ignored) {}
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return getField(superclass, names);
        }
        throw new RuntimeException("Could not find field in " + clazz.getName());
    }
}
