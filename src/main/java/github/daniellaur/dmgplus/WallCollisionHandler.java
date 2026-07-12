package github.daniellaur.dmgplus;

import github.daniellaur.dmgplus.client.DmgplusClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class WallCollisionHandler {

    private static final Map<UUID, Integer> cooldowns = new HashMap<>();

    public static int     suppressTicks           = 0;
    public static int     recentKnockbackTicks     = 0;
    public static int     recentNearWallTicks      = 0;
    public static int     authorizedTeleportTicks  = 0;
    public static int     joinGraceTicks           = 0;
    public static boolean nearWall                 = false;

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(WallCollisionHandler::tick);
    }

    private static Box inflate(Box raw, WallConfig cfg) {
        return new Box(
                raw.minX - cfg.inflateX,
                raw.minY + cfg.inflateYmin,
                raw.minZ - cfg.inflateZ,
                raw.maxX + cfg.inflateX,
                raw.maxY - cfg.inflateYmax,
                raw.maxZ + cfg.inflateZ
        );
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.player.isSpectator()) return;
        if (suppressTicks > 0) suppressTicks--;
        if (recentKnockbackTicks > 0) recentKnockbackTicks--;
        if (recentNearWallTicks > 0) recentNearWallTicks--;
        if (authorizedTeleportTicks > 0) authorizedTeleportTicks--;
        if (joinGraceTicks > 0) joinGraceTicks--;

        Iterator<Map.Entry<UUID, Integer>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            if (e.getValue() <= 1) it.remove();
            else e.setValue(e.getValue() - 1);
        }

        WallConfig cfg = ConfigManager.get();
        ClientPlayerEntity player = client.player;
        Box playerBox = inflate(player.getBoundingBox(), cfg);

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

            Box rawBox = entity.getBoundingBox();
            double boxCenterX = (rawBox.minX + rawBox.maxX) / 2.0;
            Box wallBox = inflate(rawBox.offset(wallX - boxCenterX, 0, 0), cfg);
            handleWall(player, playerBox, wallBox, entity.getUuid(), cfg);
        }

        nearWall = foundNearWall;
        if (foundNearWall) recentNearWallTicks = 100;

        if (cfg.suppressRubberband && authorizedTeleportTicks == 0 && joinGraceTicks == 0
                && (foundNearWall || recentKnockbackTicks > 0)) {
            if (ClientPlayNetworking.canSend(DmgplusClient.PLAYER_KB_ID)) {
                ClientPlayNetworking.send(new DmgplusClient.KbPayload(
                        player.getX(), player.getY(), player.getZ()));
            }
        }
    }

    private static void handleWall(ClientPlayerEntity player, Box playerBox, Box wallBox,
                                   UUID wallId, WallConfig cfg) {
        if (!cfg.knockbackEnabled) return;
        if (!playerBox.intersects(wallBox)) return;

        if (cooldowns.containsKey(wallId)) return;

        Vec3d vel = player.getVelocity();

        double newX = Math.min(Math.max(vel.x, cfg.pushBackMin), cfg.pushBackMax);
        double newY = (player.isOnGround() || Math.abs(vel.y) < 0.08)
                ? Math.max(vel.y, cfg.verticalBoost)
                : vel.y;

        double newZ = vel.z * 0.35;

        player.setVelocity(newX, newY, newZ);

        if (cfg.cooldownTicks > 0) {
            cooldowns.put(wallId, cfg.cooldownTicks);
        }

        suppressTicks        = Math.max(suppressTicks, cfg.cooldownTicks + 20);
        recentKnockbackTicks = Math.max(recentKnockbackTicks, 100);
    }
}
