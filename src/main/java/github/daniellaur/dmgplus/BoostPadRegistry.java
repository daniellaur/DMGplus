package github.daniellaur.dmgplus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoostPadRegistry {

    private static final double MAX_SPEED  = 100.0;
    private static final float  MAX_VOLUME = 10.0f;
    private static final float  MAX_PITCH  = 2.0f;

    private static final Map<String, BoostPadConfig> pads = new HashMap<>();

    public static void set(List<BoostPadConfig> configs) {
        pads.clear();
        for (BoostPadConfig cfg : configs) {
            if (cfg.blockId() == null || cfg.soundId() == null) continue;
            if (!Double.isFinite(cfg.horizontalSpeed()) || !Double.isFinite(cfg.verticalSpeed())) continue;
            pads.put(cfg.blockId(), new BoostPadConfig(
                    cfg.blockId(),
                    clamp(cfg.horizontalSpeed(), MAX_SPEED),
                    clamp(cfg.verticalSpeed(), MAX_SPEED),
                    cfg.soundId(),
                    clampFloat(cfg.soundVolume(), 0.0f, MAX_VOLUME, 1.0f),
                    clampFloat(cfg.soundPitch(), 0.5f, MAX_PITCH, 1.0f)
            ));
        }
    }

    public static BoostPadConfig get(String blockId) {
        return pads.get(blockId);
    }

    public static void clear() {
        pads.clear();
    }

    private static double clamp(double value, double max) {
        return Math.max(-max, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
