package github.daniellaur.dmgplus;

public record SpeedPadConfig(
        String blockId,
        int amplifier,
        String soundId,
        float soundVolume,
        float soundPitch
) {}
