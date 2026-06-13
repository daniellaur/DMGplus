package github.daniellaur.dmgplus;

public record BoostPadConfig(
        String blockId,
        double horizontalSpeed,
        double verticalSpeed,
        String soundId,
        float soundVolume,
        float soundPitch
) {}
