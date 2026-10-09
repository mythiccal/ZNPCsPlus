package lol.pyr.znpcsplus.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import lol.pyr.znpcsplus.npc.NpcImpl;
import lol.pyr.znpcsplus.reflection.Reflections;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reserves entity ids from the server's counter so packet-based entities do not
 * collide with real entities. Supports Paper's per-world API (26.2+), Paper's
 * NMS helper, and legacy Spigot field reflection.
 */
public final class EntityIdAllocator {
    private static volatile boolean resolved;
    private static Method getUnsafeMethod;
    private static Method unsafeNextEntityIdWorld;
    private static Method unsafeNextEntityId;
    private static Method nmsNextEntityId;

    private EntityIdAllocator() {}

    public static int nextEntityId(Viewable viewable) {
        return nextEntityId(resolveWorld(viewable));
    }

    public static World resolveWorld(Viewable viewable) {
        if (viewable instanceof NpcImpl) {
            World world = ((NpcImpl) viewable).getBukkitWorld();
            if (world != null) return world;
        }
        List<World> worlds = Bukkit.getWorlds();
        return worlds.isEmpty() ? null : worlds.get(0);
    }

    public static int nextEntityId(World world) {
        resolvePaperMethods();

        if (world != null && unsafeNextEntityIdWorld != null) {
            try {
                return (int) unsafeNextEntityIdWorld.invoke(getUnsafe(), world);
            } catch (ReflectiveOperationException ignored) {}
        }

        if (unsafeNextEntityId != null) {
            try {
                return (int) unsafeNextEntityId.invoke(getUnsafe());
            } catch (ReflectiveOperationException ignored) {}
        }

        if (nmsNextEntityId != null) {
            try {
                return (int) nmsNextEntityId.invoke(null);
            } catch (ReflectiveOperationException ignored) {}
        }

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14)) {
            AtomicInteger counter = Reflections.ATOMIC_ENTITY_ID_FIELD.get();
            if (counter != null) return counter.incrementAndGet();
        } else {
            int id = Reflections.ENTITY_ID_MODIFIER.get();
            Reflections.ENTITY_ID_MODIFIER.set(id + 1);
            return id;
        }

        throw new IllegalStateException("Failed to reserve an entity id on this server version");
    }

    private static void resolvePaperMethods() {
        if (resolved) return;
        synchronized (EntityIdAllocator.class) {
            if (resolved) return;
            try {
                getUnsafeMethod = Bukkit.class.getMethod("getUnsafe");
                Object unsafe = getUnsafeMethod.invoke(null);
                if (unsafe != null) {
                    Class<?> unsafeClass = unsafe.getClass();
                    try {
                        unsafeNextEntityIdWorld = unsafeClass.getMethod("nextEntityId", World.class);
                    } catch (NoSuchMethodException ignored) {}
                    try {
                        unsafeNextEntityId = unsafeClass.getMethod("nextEntityId");
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Throwable ignored) {}

            try {
                nmsNextEntityId = Class.forName("net.minecraft.world.entity.Entity")
                        .getMethod("nextEntityId");
            } catch (Throwable ignored) {}

            resolved = true;
        }
    }

    private static Object getUnsafe() throws ReflectiveOperationException {
        if (getUnsafeMethod == null) throw new NoSuchMethodException("getUnsafe");
        return getUnsafeMethod.invoke(null);
    }
}
