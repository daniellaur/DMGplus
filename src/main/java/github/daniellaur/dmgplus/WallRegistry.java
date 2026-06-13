package github.daniellaur.dmgplus;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WallRegistry {

    private static final Set<UUID> wallUuids =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void register(UUID uuid) {
        wallUuids.add(uuid);
    }

    public static void unregister(UUID uuid) {
        wallUuids.remove(uuid);
    }

    public static void clear() {
        wallUuids.clear();
    }

    public static boolean isWall(UUID uuid) {
        return wallUuids.contains(uuid);
    }
}