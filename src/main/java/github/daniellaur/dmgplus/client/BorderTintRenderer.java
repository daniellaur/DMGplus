package github.daniellaur.dmgplus.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public final class BorderTintRenderer {

    private static final Identifier LAYER_ID = Identifier.of("roundworldborder", "border_tint");
    private static final int STRONG = 0x60FF0000;
    private static final int CLEAR = 0x00FF0000;
    private static final int HORIZONTAL_FADE_STEPS = 16;

    private static volatile boolean outside = false;

    private BorderTintRenderer() {
    }

    public static void setOutside(boolean value) {
        outside = value;
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, LAYER_ID, BorderTintRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!outside) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int edge = Math.max(20, Math.min(width, height) / 8);
        context.fillGradient(0, 0, width, edge, STRONG, CLEAR);
        context.fillGradient(0, height - edge, width, height, CLEAR, STRONG);
        int stripWidth = Math.max(1, edge / HORIZONTAL_FADE_STEPS);
        int maxAlpha = (STRONG >>> 24) & 0xFF;
        int rgb = STRONG & 0x00FFFFFF;
        for (int i = 0; i < HORIZONTAL_FADE_STEPS; i++) {
            int alpha = Math.round(maxAlpha * (1f - (float) i / HORIZONTAL_FADE_STEPS));
            int color = (alpha << 24) | rgb;

            int leftX1 = i * stripWidth;
            context.fill(leftX1, 0, leftX1 + stripWidth, height, color);

            int rightX2 = width - i * stripWidth;
            context.fill(rightX2 - stripWidth, 0, rightX2, height, color);
        }
    }
}
