package github.daniellaur.dmgplus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoostPadRegistry {

    private static final Map<String, BoostPadConfig> pads = new HashMap<>();

    public static void set(List<BoostPadConfig> configs) {
        pads.clear();
        for (BoostPadConfig cfg : configs) {
            pads.put(cfg.blockId(), cfg);
        }
    }

    public static BoostPadConfig get(String blockId) {
        return pads.get(blockId);
    }

    public static void clear() {
        pads.clear();
    }
}
