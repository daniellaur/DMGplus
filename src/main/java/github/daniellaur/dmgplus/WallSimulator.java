package github.daniellaur.dmgplus;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WallSimulator {

    private static final double CATCHUP = 1.0;

    private static final Map<UUID, Boolean> walls      = new ConcurrentHashMap<>();
    private static final Set<UUID>          pending    = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Double>  simulatedX = new ConcurrentHashMap<>();

    private static volatile double pendingBdSpeed   = 0;
    private static volatile double pendingMgBdSpeed = 0;

    private static double bdSpeed   = 0;
    private static double mgBdSpeed = 0;

    public static void register(UUID uuid, boolean isMgBd) {
        walls.put(uuid, isMgBd);
        pending.add(uuid);
    }

    public static void unregister(UUID uuid) {
        walls.remove(uuid);
        pending.remove(uuid);
        simulatedX.remove(uuid);
    }

    public static Double getSimulatedX(UUID uuid) {
        return simulatedX.get(uuid);
    }

    public static void clear() {
        walls.clear();
        pending.clear();
        simulatedX.clear();
        bdSpeed          = 0;
        mgBdSpeed        = 0;
        pendingBdSpeed   = 0;
        pendingMgBdSpeed = 0;
    }

    public static void setVelocity(double bd, double mgBd) {
        pendingBdSpeed   = bd;
        pendingMgBdSpeed = mgBd;
    }

    public static void registerTick() {
        ClientTickEvents.END_CLIENT_TICK.register(WallSimulator::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) return;

        double newBd   = pendingBdSpeed;
        double newMgBd = pendingMgBdSpeed;
        if (Double.compare(newBd, bdSpeed) != 0 || Double.compare(newMgBd, mgBdSpeed) != 0) {
            bdSpeed   = newBd;
            mgBdSpeed = newMgBd;
            WallCollisionHandler.recentKnockbackTicks =
                    Math.max(WallCollisionHandler.recentKnockbackTicks, 40);
        }

        for (Entity entity : client.world.getEntities()) {
            UUID uuid = entity.getUuid();
            Boolean isMgBd = walls.get(uuid);
            if (isMgBd == null) continue;

            Box box = entity.getBoundingBox();
            double serverX = (box.minX + box.maxX) / 2.0;

            double simX;
            if (pending.remove(uuid)) {
                simX = serverX;
            } else {
                double speed     = isMgBd ? mgBdSpeed : bdSpeed;
                double prev       = simulatedX.getOrDefault(uuid, serverX);
                double predicted  = prev + speed;
                simX = predicted + (serverX - predicted) * CATCHUP;
            }

            simulatedX.put(uuid, simX);
            entity.setPos(simX, entity.getY(), entity.getZ());

            Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                vehicle.setPos(simX, vehicle.getY(), vehicle.getZ());
                for (Entity passenger : vehicle.getPassengerList()) {
                    if (passenger != entity) {
                        passenger.setPos(simX, passenger.getY(), passenger.getZ());
                    }
                }
            }
        }
    }
}
