package github.daniellaur.dmgplus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedPadRegistry {

    private static final int   MAX_AMPLIFIER = 10;
    private static final float MAX_VOLUME    = 10.0f;
    private static final float MAX_PITCH     = 2.0f;

    private static final Map<String, SpeedPadConfig> DEFAULTS = Map.of(
            "minecraft:magenta_glazed_terracotta",
            new SpeedPadConfig("minecraft:magenta_glazed_terracotta", 2, "", 1.0f, 1.0f)
    );

    private static final Map<String, SpeedPadConfig> pads = new HashMap<>(DEFAULTS);

    public static void set(List<SpeedPadConfig> configs) {
        pads.clear();
        pads.putAll(DEFAULTS);
        for (SpeedPadConfig cfg : configs) {
            if (cfg.blockId() == null || cfg.soundId() == null) continue;
            pads.put(cfg.blockId(), new SpeedPadConfig(
                    cfg.blockId(),
                    Math.max(0, Math.min(MAX_AMPLIFIER, cfg.amplifier())),
                    cfg.soundId(),
                    clampFloat(cfg.soundVolume(), 0.0f, MAX_VOLUME, 1.0f),
                    clampFloat(cfg.soundPitch(), 0.5f, MAX_PITCH, 1.0f)
            ));
        }
    }

    public static SpeedPadConfig get(String blockId) {
        return pads.get(blockId);
    }

    public static void clear() {
        pads.clear();
        pads.putAll(DEFAULTS);
    }

    private static float clampFloat(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
