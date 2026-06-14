package github.daniellaur.dmgplus;

import github.daniellaur.dmgplus.client.DmgplusClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class WallCollisionHandler {

    private static final Map<UUID, Integer> cooldowns     = new HashMap<>();
    private static final Map<UUID, Vec3d>   prevPositions = new HashMap<>();

    public static int     suppressTicks         = 0;
    public static int     recentKnockbackTicks  = 0;
    public static int     recentNearWallTicks   = 0;
    public static int     authorizedTeleportTicks = 0;
    public static boolean nearWall              = false;

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(WallCollisionHandler::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.player.isSpectator()) return;
        if (suppressTicks > 0) suppressTicks--;
        if (recentKnockbackTicks > 0) recentKnockbackTicks--;
        if (recentNearWallTicks > 0) recentNearWallTicks--;
        if (authorizedTeleportTicks > 0) authorizedTeleportTicks--;

        Iterator<Map.Entry<UUID, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            if (e.getValue() <= 1) it.remove();
            else e.setValue(e.getValue() - 1);
        }

        WallConfig cfg = ConfigManager.get();
        ClientPlayerEntity player = client.player;
        Box playerBox = player.getBoundingBox().expand(cfg.inflateX, cfg.inflateY, cfg.inflateZ);
        int pingTicks = 0;
        if (cfg.lagCompensation && client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) {
                int pingMs = entry.getLatency();
                pingTicks = Math.min(cfg.maxCompensationTicks, Math.max(0, (pingMs + 25) / 50));
            }
        }

        double suppressRadiusSq = cfg.suppressRadius * cfg.suppressRadius;
        boolean foundNearWall = false;

        for (Entity entity : client.world.getEntities()) {
            if (!WallRegistry.isWall(entity.getUuid())) continue;

            Double simX = WallSimulator.getSimulatedX(entity.getUuid());
            double wallX = simX != null ? simX : entity.getX();

            double dx = player.getX() - wallX;
            double dy = player.getY() - entity.getY();
            double dz = player.getZ() - entity.getZ();
            if (dx * dx + dy * dy + dz * dz <= suppressRadiusSq) foundNearWall = true;

            Vec3d pos = new Vec3d(wallX, entity.getY(), entity.getZ());
            Vec3d prev = prevPositions.put(entity.getUuid(), pos);
            double wallDx = 0;
            if (prev != null) {
                wallDx = MathHelper.clamp(pos.x - prev.x, -cfg.maxDxPerTick, cfg.maxDxPerTick);
            }

            Box rawBox = entity.getBoundingBox();
            double boxCenterX = (rawBox.minX + rawBox.maxX) / 2.0;
            Box wallBox = rawBox
                    .offset(wallX - boxCenterX, 0, 0)
                    .expand(cfg.inflateX, cfg.inflateY, cfg.inflateZ);
            handleWall(player, playerBox, wallBox, entity.getUuid(), wallDx, pingTicks, cfg);
        }

        nearWall = foundNearWall;
        if (foundNearWall) recentNearWallTicks = 100;

        if (cfg.suppressRubberband && authorizedTeleportTicks == 0
                && (foundNearWall || recentKnockbackTicks > 0)) {
            if (ClientPlayNetworking.canSend(DmgplusClient.PLAYER_KB_ID)) {
                ClientPlayNetworking.send(new DmgplusClient.KbPayload(
                        player.getX(), player.getY(), player.getZ()));
            }
        }
    }

    private static void handleWall(ClientPlayerEntity player, Box playerBox, Box wallBox,
                                   UUID wallId, double wallDx, int pingTicks, WallConfig cfg) {
        if (cooldowns.containsKey(wallId)) return;

        double spd = Math.max(0, wallDx);
        if (!playerBox.intersects(wallBox)) return;
        double overlapX = Math.min(playerBox.maxX, wallBox.maxX) - Math.max(playerBox.minX, wallBox.minX);
        if (overlapX < cfg.minPenetration) return;

        if (!cfg.knockbackEnabled) return;

        double headStart = spd * pingTicks * cfg.resolveAheadFactor;
        double targetMinX = wallBox.maxX + headStart + cfg.epsilon;
        double shiftX = targetMinX - playerBox.minX;
        if (shiftX > 0) {
            player.setPos(player.getX() + shiftX, player.getY(), player.getZ());
        }

        Vec3d vel = player.getVelocity();

        double newX = Math.max(vel.x, Math.abs(cfg.pushBack));
        double newY = (player.isOnGround() || Math.abs(vel.y) < 0.08)
                ? Math.max(vel.y, cfg.verticalBoost)
                : vel.y;

        double newZ = vel.z * 0.35;

        player.setVelocity(newX, newY, newZ);

        if (cfg.cooldownTicks > 0) {
            cooldowns.put(wallId, cfg.cooldownTicks);
        }

        suppressTicks        = Math.max(suppressTicks, cfg.cooldownTicks + pingTicks * 2 + 20);
        recentKnockbackTicks = Math.max(recentKnockbackTicks, 100);
    }
}
