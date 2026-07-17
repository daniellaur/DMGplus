package github.daniellaur.dmgplus;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WallSimulator {

    private static final class WallData {
        final boolean isMgBd;
        volatile List<UUID> members = List.of();
        volatile double  authX    = 0;
        volatile boolean hasAuth  = false;
        double  simX     = 0;
        boolean hasSim   = false;
        boolean anchored = false;

        WallData(boolean isMgBd) { this.isMgBd = isMgBd; }
    }

    private static final Map<UUID, WallData> walls = new ConcurrentHashMap<>();

    private static volatile double pendingBdSpeed   = 0;
    private static volatile double pendingMgBdSpeed = 0;

    private static double bdSpeed   = 0;
    private static double mgBdSpeed = 0;

    public static void register(UUID uuid, boolean isMgBd) {
        walls.computeIfAbsent(uuid, k -> new WallData(isMgBd));
    }

    public static void setMembers(UUID uuid, List<UUID> members) {
        WallData d = walls.get(uuid);
        if (d != null) d.members = members;
    }

    public static void anchor(UUID uuid, double x) {
        if (!Double.isFinite(x)) return;
        WallData d = walls.get(uuid);
        if (d != null) {
            d.authX   = x;
            d.hasAuth = true;
        }
    }

    public static void unregister(UUID uuid) {
        walls.remove(uuid);
    }

    public static Double getSimulatedX(UUID uuid) {
        WallData d = walls.get(uuid);
        return (d == null || !d.hasSim) ? null : d.simX;
    }

    public static void clear() {
        walls.clear();
        bdSpeed          = 0;
        mgBdSpeed        = 0;
        pendingBdSpeed   = 0;
        pendingMgBdSpeed = 0;
    }

    public static void setVelocity(double bd, double mgBd) {
        pendingBdSpeed   = Double.isFinite(bd) ? bd : 0;
        pendingMgBdSpeed = Double.isFinite(mgBd) ? mgBd : 0;
    }

    public static void registerTick() {
        ClientTickEvents.END_CLIENT_TICK.register(WallSimulator::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) return;
        if (walls.isEmpty()) return;

        bdSpeed   = pendingBdSpeed;
        mgBdSpeed = pendingMgBdSpeed;

        Map<UUID, Entity> byUuid = new HashMap<>();
        for (Entity e : client.world.getEntities()) {
            byUuid.put(e.getUuid(), e);
        }

        for (Map.Entry<UUID, WallData> entry : walls.entrySet()) {
            WallData d = entry.getValue();
            Entity shulker = byUuid.get(entry.getKey());
            if (shulker == null) continue;

            double speed = d.isMgBd ? mgBdSpeed : bdSpeed;

            if (!d.anchored && d.hasAuth) {
                d.simX     = d.authX;
                d.anchored = true;
            } else if (d.anchored && speed > 0) {
                d.simX += speed;
            } else {
                Box box = shulker.getBoundingBox();
                double serverX = (box.minX + box.maxX) / 2.0;
                d.simX = d.hasSim ? Math.max(d.simX, serverX) : serverX;
            }

            d.hasSim = true;

            shulker.setPos(d.simX, shulker.getY(), shulker.getZ());
            for (UUID memberUuid : d.members) {
                Entity m = byUuid.get(memberUuid);
                if (m != null) {
                    m.setPos(d.simX, m.getY(), m.getZ());
                }
            }
        }
    }
}
