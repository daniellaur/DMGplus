package github.daniellaur.dmgplus.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CowStrikeHitscan {

    private static final double RANGE = 100.0;
    private static final double PARTICLE_STEP = 0.3;

    private static boolean wasAttackDown = false;

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(CowStrikeHitscan::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        boolean attackDown = client.options.attackKey.isPressed();
        boolean justPressed = attackDown && !wasAttackDown;
        wasAttackDown = attackDown;

        if (!justPressed) return;
        if (!CowStrikeState.isInContext()) return;
        if (client.player.isSpectator()) return;
        if (client.player.getMainHandStack().getItem() != Items.CROSSBOW) return;
        if (client.player.getItemCooldownManager().isCoolingDown(client.player.getMainHandStack())) return;
        if (CowStrikeState.isReloading()) return;
        if (CowStrikeState.getAmmo() <= 0) return;
        if (!ClientPlayNetworking.canSend(DmgplusClient.COWSTRIKE_SHOOT_ID)) return;

        fire(client);
    }

    private static void fire(MinecraftClient client) {
        AbstractClientPlayerEntity player = client.player;
        Vec3d eye = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d farEnd = eye.add(dir.multiply(RANGE));

        BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                eye, farEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        double beamLength = blockHit.getType() == HitResult.Type.BLOCK
                ? eye.distanceTo(blockHit.getPos())
                : RANGE;

        Vec3d beamEnd = eye.add(dir.multiply(beamLength));

        List<UUID> hits = new ArrayList<>();
        for (AbstractClientPlayerEntity other : client.world.getPlayers()) {
            if (other == player) continue;
            if (other.isSpectator()) continue;
            Box box = other.getBoundingBox();
            if (box.raycast(eye, beamEnd).isPresent()) {
                hits.add(other.getUuid());
            }
        }

        drawBeam(client, eye, dir, beamLength);
        playShotSound(client, player);
        CowStrikeState.predictLocalShotFired();

        ClientPlayNetworking.send(new DmgplusClient.ShootPayload(
                eye.x, eye.y, eye.z, dir.x, dir.y, dir.z, hits));
    }

    private static void drawBeam(MinecraftClient client, Vec3d eye, Vec3d dir, double length) {
        for (double d = 0; d <= length; d += PARTICLE_STEP) {
            Vec3d p = eye.add(dir.multiply(d));
            client.world.addParticleClient(ParticleTypes.CRIT, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private static void playShotSound(MinecraftClient client, AbstractClientPlayerEntity player) {
        client.getSoundManager().play(new PositionedSoundInstance(
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.AMBIENT,
                3.0f, 2.0f, SoundInstance.createRandom(),
                player.getX(), player.getY(), player.getZ()
        ));
    }
}
