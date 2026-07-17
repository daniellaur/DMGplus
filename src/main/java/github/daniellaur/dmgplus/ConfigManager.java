package github.daniellaur.dmgplus;

public class ConfigManager {

    private static volatile WallConfig instance = new WallConfig();

    public static WallConfig get() {
        return instance;
    }

    public static void setConfig(WallConfig cfg) {
        instance = sanitize(cfg);
    }

    public static void reset() {
        instance = new WallConfig();
    }

    private static WallConfig sanitize(WallConfig cfg) {
        WallConfig def = new WallConfig();
        cfg.pushBackMin    = finite(cfg.pushBackMin,    def.pushBackMin);
        cfg.pushBackMax    = finite(cfg.pushBackMax,    def.pushBackMax);
        cfg.verticalBoost  = finite(cfg.verticalBoost,  def.verticalBoost);
        cfg.inflateX       = finite(cfg.inflateX,       def.inflateX);
        cfg.inflateYmin    = finite(cfg.inflateYmin,    def.inflateYmin);
        cfg.inflateYmax    = finite(cfg.inflateYmax,    def.inflateYmax);
        cfg.inflateZ       = finite(cfg.inflateZ,       def.inflateZ);
        cfg.suppressRadius = finite(cfg.suppressRadius, def.suppressRadius);
        if (cfg.cooldownTicks < 0) cfg.cooldownTicks = def.cooldownTicks;
        return cfg;
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}
