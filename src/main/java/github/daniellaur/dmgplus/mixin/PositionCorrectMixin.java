package github.daniellaur.dmgplus.mixin;

import github.daniellaur.dmgplus.ConfigManager;
import github.daniellaur.dmgplus.WallCollisionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class PositionCorrectMixin {

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"), cancellable = true, require = 0)
    private void suppressAfterKnockback(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (!ConfigManager.get().suppressRubberband) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        boolean suppress = WallCollisionHandler.nearWall
                || WallCollisionHandler.suppressTicks > 0
                || WallCollisionHandler.recentKnockbackTicks > 0
                || WallCollisionHandler.recentNearWallTicks > 0;
        if (!suppress) return;

        Vec3d pos = packet.change().position();
        double targetY = packet.relatives().contains(PositionFlag.Y)
                ? client.player.getY() + pos.y : pos.y;
        if (targetY > client.player.getY() + 10.0) return;

        ack(packet, client);
        ci.cancel();
    }

    private static void ack(PlayerPositionLookS2CPacket packet, MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler != null) {
            handler.sendPacket(new TeleportConfirmC2SPacket(packet.teleportId()));
        }
    }
}
