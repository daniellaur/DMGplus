package github.daniellaur.dmgplus;

import github.daniellaur.dmgplus.client.DmgplusClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class SpeedPadHandler {

    private static final int Y_REACH = 1;

    private static boolean wasOnPad     = false;
    private static int     sendCooldown = 0;

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(SpeedPadHandler::tick);
    }

    public static void reset() {
        wasOnPad     = false;
        sendCooldown = 0;
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (client.player.isSpectator()) return;
        if (!ClientPlayNetworking.canSend(DmgplusClient.SPEEDPAD_ACTIVATE_ID)) return;

        ClientPlayerEntity player = client.player;
        SpeedPadConfig cfg = getSpeedPadUnder(client, player);
        boolean onPad = cfg != null;

        if (onPad) {
            if (!wasOnPad && !cfg.soundId().isEmpty()) {
                client.getSoundManager().play(new PositionedSoundInstance(
                        SoundEvent.of(Identifier.of(cfg.soundId())), SoundCategory.AMBIENT,
                        cfg.soundVolume(), cfg.soundPitch(),
                        SoundInstance.createRandom(),
                        player.getX(), player.getY(), player.getZ()
                ));
            }

            if (sendCooldown <= 0) {
                if (ClientPlayNetworking.canSend(DmgplusClient.SPEEDPAD_ACTIVATE_ID)) {
                    ClientPlayNetworking.send(new DmgplusClient.SpeedPadActivatePayload());
                }
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SPEED, 40, cfg.amplifier(), true, false, false));
                sendCooldown = 20;
            } else {
                sendCooldown--;
            }
        } else {
            sendCooldown = 0;
        }

        wasOnPad = onPad;
    }

    private static SpeedPadConfig getSpeedPadUnder(MinecraftClient client, ClientPlayerEntity player) {
        int feetY = (int) Math.floor(player.getY() - 0.2);
        double r = 0.3;
        for (int dy = 0; dy <= Y_REACH; dy++) {
            int y = feetY - dy;
            for (double dx : new double[]{-r, r}) {
                for (double dz : new double[]{-r, r}) {
                    int bx = (int) Math.floor(player.getX() + dx);
                    int bz = (int) Math.floor(player.getZ() + dz);
                    String blockId = Registries.BLOCK.getId(
                            client.world.getBlockState(new BlockPos(bx, y, bz)).getBlock()).toString();
                    SpeedPadConfig cfg = SpeedPadRegistry.get(blockId);
                    if (cfg != null) return cfg;
                }
            }
        }
        return null;
    }
}
