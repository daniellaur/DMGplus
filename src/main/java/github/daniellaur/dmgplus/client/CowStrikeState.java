package github.daniellaur.dmgplus.client;

public class CowStrikeState {

    private static volatile boolean inContext = false;
    private static volatile int ammo = 0;
    private static volatile boolean reloading = false;

    public static void update(boolean newInContext, int newAmmo, boolean newReloading) {
        inContext = newInContext;
        ammo = newAmmo;
        reloading = newReloading;
    }

    public static void reset() {
        inContext = false;
        ammo = 0;
        reloading = false;
    }

    public static boolean isInContext() {
        return inContext;
    }

    public static int getAmmo() {
        return ammo;
    }

    public static boolean isReloading() {
        return reloading;
    }

    public static void predictLocalShotFired() {
        if (ammo > 0) ammo--;
    }
}
