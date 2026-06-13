package github.daniellaur.dmgplus.client;

import github.daniellaur.dmgplus.BoostPadConfig;
import github.daniellaur.dmgplus.BoostPadRegistry;
import github.daniellaur.dmgplus.ConfigManager;
import github.daniellaur.dmgplus.SpeedPadHandler;
import github.daniellaur.dmgplus.SpeedPadRegistry;
import github.daniellaur.dmgplus.WallCollisionHandler;
import github.daniellaur.dmgplus.WallConfig;
import github.daniellaur.dmgplus.WallRegistry;
import github.daniellaur.dmgplus.WallSimulator;
import github.daniellaur.dmgplus.SpeedPadConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public class DmgplusClient implements ClientModInitializer {

    public static final CustomPayload.Id<WallRegisterPayload> WALL_REGISTER_ID =
            new CustomPayload.Id<>(Identifier.of("blockdash", "wall_register"));
    public static final CustomPayload.Id<RawPayload> WALL_UNREGISTER_ID =
            new CustomPayload.Id<>(Identifier.of("blockdash", "wall_unregister"));
    public static final CustomPayload.Id<ConfigPayload> WALL_CONFIG_ID =
            new CustomPayload.Id<>(Identifier.of("blockdash", "config"));
    public static final CustomPayload.Id<KbPayload> PLAYER_KB_ID =
            new CustomPayload.Id<>(Identifier.of("blockdash", "player_kb"));
    public static final CustomPayload.Id<HelloPayload> HELLO_ID =
            new CustomPayload.Id<>(Identifier.of("blockdash", "hello"));
    public static final CustomPayload.Id<WallVelocityPayload> WALL_VELOCITY_ID =
            new CustomPayload.Id<>(Identifier.of("blockdash", "wall_velocity"));

    public static final CustomPayload.Id<BoostPadConfigPayload> BOOSTPAD_CONFIG_ID =
            new CustomPayload.Id<>(Identifier.of("boostpads", "config"));
    public static final CustomPayload.Id<SpeedPadConfigPayload> SPEEDPAD_CONFIG_ID =
            new CustomPayload.Id<>(Identifier.of("boostpads", "speedpad_config"));
    public static final CustomPayload.Id<SpeedPadActivatePayload> SPEEDPAD_ACTIVATE_ID =
            new CustomPayload.Id<>(Identifier.of("boostpads", "speedpad_activate"));

    public record WallRegisterPayload(UUID uuid, boolean isMgBd) implements CustomPayload {
        @Override public CustomPayload.Id<WallRegisterPayload> getId() { return WALL_REGISTER_ID; }
    }

    public record RawPayload(UUID uuid, CustomPayload.Id<RawPayload> id) implements CustomPayload {
        @Override public CustomPayload.Id<RawPayload> getId() { return id; }
    }

    public record ConfigPayload(WallConfig config) implements CustomPayload {
        @Override public CustomPayload.Id<ConfigPayload> getId() { return WALL_CONFIG_ID; }
    }

    public record KbPayload(double x, double y, double z) implements CustomPayload {
        @Override public CustomPayload.Id<KbPayload> getId() { return PLAYER_KB_ID; }
    }

    public record HelloPayload(String version) implements CustomPayload {
        @Override public CustomPayload.Id<HelloPayload> getId() { return HELLO_ID; }
    }

    public record WallVelocityPayload(double bdSpeed, double mgBdSpeed) implements CustomPayload {
        @Override public CustomPayload.Id<WallVelocityPayload> getId() { return WALL_VELOCITY_ID; }
    }

    public record BoostPadConfigPayload(List<BoostPadConfig> configs) implements CustomPayload {
        @Override public CustomPayload.Id<BoostPadConfigPayload> getId() { return BOOSTPAD_CONFIG_ID; }
    }

    public record SpeedPadConfigPayload(List<SpeedPadConfig> configs) implements CustomPayload {
        @Override public CustomPayload.Id<SpeedPadConfigPayload> getId() { return SPEEDPAD_CONFIG_ID; }
    }

    public record SpeedPadActivatePayload() implements CustomPayload {
        @Override public CustomPayload.Id<SpeedPadActivatePayload> getId() { return SPEEDPAD_ACTIVATE_ID; }
    }

    @Override
    public void onInitializeClient() {
        WallCollisionHandler.register();
        WallSimulator.registerTick();
        SpeedPadHandler.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(HELLO_ID)) {
                String version = FabricLoader.getInstance()
                        .getModContainer("dmgplus")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown");
                ClientPlayNetworking.send(new HelloPayload(version));
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(WALL_REGISTER_ID,
                (payload, context) -> {
                    WallRegistry.register(payload.uuid());
                    WallSimulator.register(payload.uuid(), payload.isMgBd());
                });

        ClientPlayNetworking.registerGlobalReceiver(WALL_UNREGISTER_ID,
                (payload, context) -> {
                    WallRegistry.unregister(payload.uuid());
                    WallSimulator.unregister(payload.uuid());
                });

        ClientPlayNetworking.registerGlobalReceiver(WALL_CONFIG_ID,
                (payload, context) -> ConfigManager.setConfig(payload.config()));

        ClientPlayNetworking.registerGlobalReceiver(WALL_VELOCITY_ID,
                (payload, context) -> WallSimulator.setVelocity(payload.bdSpeed(), payload.mgBdSpeed()));

        ClientPlayNetworking.registerGlobalReceiver(BOOSTPAD_CONFIG_ID,
                (payload, context) -> BoostPadRegistry.set(payload.configs()));

        ClientPlayNetworking.registerGlobalReceiver(SPEEDPAD_CONFIG_ID,
                (payload, context) -> SpeedPadRegistry.set(payload.configs()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            WallRegistry.clear();
            WallSimulator.clear();
            BoostPadRegistry.clear();
            SpeedPadRegistry.clear();
            SpeedPadHandler.reset();
        });
    }
}
