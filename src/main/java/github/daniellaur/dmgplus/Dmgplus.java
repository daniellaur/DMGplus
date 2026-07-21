package github.daniellaur.dmgplus;

import github.daniellaur.dmgplus.client.DmgplusClient;
import io.netty.handler.codec.DecoderException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Dmgplus implements ModInitializer {

    private static final int UUID_BYTES            = 16;
    private static final int MIN_WALL_ENTRY_BYTES  = 29;
    private static final int MIN_BOOSTPAD_BYTES    = 26;
    private static final int MIN_SPEEDPAD_BYTES    = 11;

    private static final PacketCodec<PacketByteBuf, UUID> UUID_CODEC = PacketCodec.of(
            (uuid, buf) -> {
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
            },
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.WallRegisterPayload> WALL_REGISTER_CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeLong(payload.uuid().getMostSignificantBits());
                        buf.writeLong(payload.uuid().getLeastSignificantBits());
                        buf.writeBoolean(payload.isMgBd());
                    },
                    buf -> {
                        UUID uuid   = new UUID(buf.readLong(), buf.readLong());
                        boolean mgBd = buf.readBoolean();
                        return new DmgplusClient.WallRegisterPayload(uuid, mgBd);
                    }
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.WallVelocityPayload> WALL_VELOCITY_CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeDouble(payload.bdSpeed());
                        buf.writeDouble(payload.mgBdSpeed());
                    },
                    buf -> new DmgplusClient.WallVelocityPayload(buf.readDouble(), buf.readDouble())
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.WallSnapshotPayload> WALL_SNAPSHOT_CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeInt(payload.entries().size());
                        for (DmgplusClient.WallEntry e : payload.entries()) {
                            buf.writeLong(e.uuid().getMostSignificantBits());
                            buf.writeLong(e.uuid().getLeastSignificantBits());
                            buf.writeBoolean(e.isMgBd());
                            buf.writeDouble(e.x());
                            buf.writeInt(e.members().size());
                            for (UUID m : e.members()) {
                                buf.writeLong(m.getMostSignificantBits());
                                buf.writeLong(m.getLeastSignificantBits());
                            }
                        }
                    },
                    buf -> {
                        int count = buf.readInt();
                        if (count < 0 || count > buf.readableBytes() / MIN_WALL_ENTRY_BYTES) {
                            throw new DecoderException("Invalid wall snapshot count: " + count);
                        }
                        List<DmgplusClient.WallEntry> entries = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            UUID uuid = new UUID(buf.readLong(), buf.readLong());
                            boolean isMgBd = buf.readBoolean();
                            double x = buf.readDouble();
                            int memberCount = buf.readInt();
                            if (memberCount < 0 || memberCount > buf.readableBytes() / UUID_BYTES) {
                                throw new DecoderException("Invalid wall member count: " + memberCount);
                            }
                            List<UUID> members = new ArrayList<>();
                            for (int j = 0; j < memberCount; j++) {
                                members.add(new UUID(buf.readLong(), buf.readLong()));
                            }
                            entries.add(new DmgplusClient.WallEntry(uuid, isMgBd, members, x));
                        }
                        return new DmgplusClient.WallSnapshotPayload(entries);
                    }
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.AuthorizeTpPayload> AUTHORIZE_TP_CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeByte(1),
                    buf -> {
                        buf.readByte();
                        return new DmgplusClient.AuthorizeTpPayload();
                    }
            );

    private static final PacketCodec<PacketByteBuf, WallConfig> WALL_CONFIG_CODEC = PacketCodec.of(
            (cfg, buf) -> {
                buf.writeBoolean(cfg.knockbackEnabled);
                buf.writeDouble(cfg.pushBackMin);
                buf.writeDouble(cfg.pushBackMax);
                buf.writeDouble(cfg.verticalBoost);
                buf.writeInt(cfg.cooldownTicks);
                buf.writeDouble(cfg.inflateX);
                buf.writeDouble(cfg.inflateYmin);
                buf.writeDouble(cfg.inflateYmax);
                buf.writeDouble(cfg.inflateZ);
                buf.writeBoolean(cfg.suppressRubberband);
                buf.writeDouble(cfg.suppressRadius);
            },
            buf -> {
                WallConfig cfg = new WallConfig();
                cfg.knockbackEnabled     = buf.readBoolean();
                cfg.pushBackMin          = buf.readDouble();
                cfg.pushBackMax          = buf.readDouble();
                cfg.verticalBoost        = buf.readDouble();
                cfg.cooldownTicks        = buf.readInt();
                cfg.inflateX             = buf.readDouble();
                cfg.inflateYmin          = buf.readDouble();
                cfg.inflateYmax          = buf.readDouble();
                cfg.inflateZ             = buf.readDouble();
                cfg.suppressRubberband   = buf.readBoolean();
                cfg.suppressRadius       = buf.readDouble();
                return cfg;
            }
    );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.KbPayload> KB_CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.y());
                buf.writeDouble(payload.z());
            },
            buf -> new DmgplusClient.KbPayload(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.HelloPayload> HELLO_CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeString(payload.version()),
                    buf -> new DmgplusClient.HelloPayload(buf.readString())
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.BoostPadConfigPayload> BOOSTPAD_CONFIG_CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        List<BoostPadConfig> configs = payload.configs();
                        buf.writeVarInt(configs.size());
                        for (BoostPadConfig cfg : configs) {
                            buf.writeString(cfg.blockId());
                            buf.writeDouble(cfg.horizontalSpeed());
                            buf.writeDouble(cfg.verticalSpeed());
                            buf.writeString(cfg.soundId());
                            buf.writeFloat(cfg.soundVolume());
                            buf.writeFloat(cfg.soundPitch());
                        }
                    },
                    buf -> {
                        int count = buf.readVarInt();
                        if (count < 0 || count > buf.readableBytes() / MIN_BOOSTPAD_BYTES) {
                            throw new DecoderException("Invalid boostpad config count: " + count);
                        }
                        List<BoostPadConfig> configs = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            configs.add(new BoostPadConfig(
                                    buf.readString(),
                                    buf.readDouble(),
                                    buf.readDouble(),
                                    buf.readString(),
                                    buf.readFloat(),
                                    buf.readFloat()
                            ));
                        }
                        return new DmgplusClient.BoostPadConfigPayload(configs);
                    }
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.SpeedPadConfigPayload> SPEEDPAD_CONFIG_CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        List<SpeedPadConfig> configs = payload.configs();
                        buf.writeVarInt(configs.size());
                        for (SpeedPadConfig cfg : configs) {
                            buf.writeString(cfg.blockId());
                            buf.writeVarInt(cfg.amplifier());
                            buf.writeString(cfg.soundId());
                            buf.writeFloat(cfg.soundVolume());
                            buf.writeFloat(cfg.soundPitch());
                        }
                    },
                    buf -> {
                        int count = buf.readVarInt();
                        if (count < 0 || count > buf.readableBytes() / MIN_SPEEDPAD_BYTES) {
                            throw new DecoderException("Invalid speedpad config count: " + count);
                        }
                        List<SpeedPadConfig> configs = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            configs.add(new SpeedPadConfig(
                                    buf.readString(),
                                    buf.readVarInt(),
                                    buf.readString(),
                                    buf.readFloat(),
                                    buf.readFloat()
                            ));
                        }
                        return new DmgplusClient.SpeedPadConfigPayload(configs);
                    }
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.SpeedPadActivatePayload> SPEEDPAD_ACTIVATE_CODEC =
            PacketCodec.of((payload, buf) -> {}, buf -> new DmgplusClient.SpeedPadActivatePayload());

    private static final PacketCodec<PacketByteBuf, DmgplusClient.BorderTintPayload> BORDER_TINT_CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeBoolean(payload.outside()),
                    buf -> new DmgplusClient.BorderTintPayload(buf.readBoolean())
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.ShootPayload> COWSTRIKE_SHOOT_CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeDouble(payload.ox());
                        buf.writeDouble(payload.oy());
                        buf.writeDouble(payload.oz());
                        buf.writeDouble(payload.dx());
                        buf.writeDouble(payload.dy());
                        buf.writeDouble(payload.dz());
                        buf.writeInt(payload.hits().size());
                        for (UUID id : payload.hits()) {
                            buf.writeLong(id.getMostSignificantBits());
                            buf.writeLong(id.getLeastSignificantBits());
                        }
                    },
                    buf -> {
                        double ox = buf.readDouble();
                        double oy = buf.readDouble();
                        double oz = buf.readDouble();
                        double dx = buf.readDouble();
                        double dy = buf.readDouble();
                        double dz = buf.readDouble();
                        int count = buf.readInt();
                        if (count < 0 || count > buf.readableBytes() / UUID_BYTES) {
                            throw new DecoderException("Invalid cowstrike hit count: " + count);
                        }
                        List<UUID> hits = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            hits.add(new UUID(buf.readLong(), buf.readLong()));
                        }
                        return new DmgplusClient.ShootPayload(ox, oy, oz, dx, dy, dz, hits);
                    }
            );

    private static final PacketCodec<PacketByteBuf, DmgplusClient.StatePayload> COWSTRIKE_STATE_CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeBoolean(payload.inContext());
                        buf.writeInt(payload.ammo());
                        buf.writeBoolean(payload.reloading());
                        buf.writeBoolean(payload.noThrow());
                    },
                    buf -> new DmgplusClient.StatePayload(
                            buf.readBoolean(), buf.readInt(), buf.readBoolean(), buf.readBoolean())
            );

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(DmgplusClient.PLAYER_KB_ID, KB_CODEC);
        PayloadTypeRegistry.playC2S().register(DmgplusClient.HELLO_ID, HELLO_CODEC);
        PayloadTypeRegistry.playC2S().register(DmgplusClient.SPEEDPAD_ACTIVATE_ID, SPEEDPAD_ACTIVATE_CODEC);

        PayloadTypeRegistry.playS2C().register(DmgplusClient.WALL_REGISTER_ID, WALL_REGISTER_CODEC);

        PayloadTypeRegistry.playS2C().register(
                DmgplusClient.WALL_UNREGISTER_ID,
                UUID_CODEC.xmap(
                        uuid -> new DmgplusClient.RawPayload(uuid, DmgplusClient.WALL_UNREGISTER_ID),
                        DmgplusClient.RawPayload::uuid
                )
        );

        PayloadTypeRegistry.playS2C().register(
                DmgplusClient.WALL_CONFIG_ID,
                WALL_CONFIG_CODEC.xmap(
                        DmgplusClient.ConfigPayload::new,
                        DmgplusClient.ConfigPayload::config
                )
        );

        PayloadTypeRegistry.playS2C().register(DmgplusClient.WALL_VELOCITY_ID, WALL_VELOCITY_CODEC);
        PayloadTypeRegistry.playS2C().register(DmgplusClient.WALL_SNAPSHOT_ID, WALL_SNAPSHOT_CODEC);
        PayloadTypeRegistry.playS2C().register(DmgplusClient.AUTHORIZE_TP_ID, AUTHORIZE_TP_CODEC);
        PayloadTypeRegistry.playS2C().register(DmgplusClient.BOOSTPAD_CONFIG_ID, BOOSTPAD_CONFIG_CODEC);
        PayloadTypeRegistry.playS2C().register(DmgplusClient.SPEEDPAD_CONFIG_ID, SPEEDPAD_CONFIG_CODEC);
        PayloadTypeRegistry.playS2C().register(DmgplusClient.BORDER_TINT_ID, BORDER_TINT_CODEC);

        PayloadTypeRegistry.playC2S().register(DmgplusClient.COWSTRIKE_SHOOT_ID, COWSTRIKE_SHOOT_CODEC);
        PayloadTypeRegistry.playS2C().register(DmgplusClient.COWSTRIKE_STATE_ID, COWSTRIKE_STATE_CODEC);
    }
}
