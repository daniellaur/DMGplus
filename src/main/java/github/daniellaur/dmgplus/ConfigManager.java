package github.daniellaur.dmgplus;

public class ConfigManager {

    private static volatile WallConfig instance = new WallConfig();

    public static WallConfig get() {
        return instance;
    }

    public static void setConfig(WallConfig cfg) {
        instance = cfg;
    }
}
