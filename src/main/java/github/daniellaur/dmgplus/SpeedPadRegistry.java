package github.daniellaur.dmgplus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedPadRegistry {

    private static final Map<String, SpeedPadConfig> DEFAULTS = Map.of(
            "minecraft:magenta_glazed_terracotta",
            new SpeedPadConfig("minecraft:magenta_glazed_terracotta", 2, "", 1.0f, 1.0f)
    );

    private static final Map<String, SpeedPadConfig> pads = new HashMap<>(DEFAULTS);

    public static void set(List<SpeedPadConfig> configs) {
        pads.clear();
        pads.putAll(DEFAULTS);
        for (SpeedPadConfig cfg : configs) {
            pads.put(cfg.blockId(), cfg);
        }
    }

    public static SpeedPadConfig get(String blockId) {
        return pads.get(blockId);
    }

    public static void clear() {
        pads.clear();
        pads.putAll(DEFAULTS);
    }
}
