package github.daniellaur.dmgplus.mixin;

import github.daniellaur.dmgplus.BoostPadConfig;
import github.daniellaur.dmgplus.BoostPadRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class BoostPadMixin {

    @Inject(method = "jump", at = @At("RETURN"))
    private void onJump(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if ((Object) this != client.player) return;

        ClientPlayerEntity player = client.player;

        BoostPadConfig cfg = getBoostPadUnder(client, player);
        if (cfg == null) return;

        double yawRad = Math.toRadians(player.getYaw());
        double vx = -Math.sin(yawRad) * cfg.horizontalSpeed();
        double vz = Math.cos(yawRad) * cfg.horizontalSpeed();
        player.setVelocity(vx, cfg.verticalSpeed(), vz);

        if (!cfg.soundId().isEmpty()) {
            Identifier soundId = Identifier.tryParse(cfg.soundId());
            if (soundId != null) {
                client.getSoundManager().play(new PositionedSoundInstance(
                        SoundEvent.of(soundId), SoundCategory.AMBIENT,
                        cfg.soundVolume(), cfg.soundPitch(),
                        SoundInstance.createRandom(),
                        player.getX(), player.getY(), player.getZ()
                ));
            }
        }
    }

    private static BoostPadConfig getBoostPadUnder(MinecraftClient client, ClientPlayerEntity player) {
        int y = BlockPos.ofFloored(player.getX(), player.getY() - 0.2, player.getZ()).getY();
        double r = 0.3;
        for (double dx : new double[]{-r, r}) {
            for (double dz : new double[]{-r, r}) {
                int bx = (int) Math.floor(player.getX() + dx);
                int bz = (int) Math.floor(player.getZ() + dz);
                String blockId = Registries.BLOCK.getId(
                        client.world.getBlockState(new BlockPos(bx, y, bz)).getBlock()).toString();
                BoostPadConfig cfg = BoostPadRegistry.get(blockId);
                if (cfg != null) return cfg;
            }
        }
        return null;
    }
}
