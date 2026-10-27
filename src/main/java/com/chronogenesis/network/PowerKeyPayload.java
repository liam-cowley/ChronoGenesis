package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a power key was pressed. */
public record PowerKeyPayload(Action action) implements CustomPacketPayload {
    public enum Action {
        CYCLE_SELECTED,
        USE_SELECTED
    }

    public static final Type<PowerKeyPayload> TYPE = new Type<>(ChronoGenesis.id("power_key"));
    public static final StreamCodec<ByteBuf, PowerKeyPayload> STREAM_CODEC = ByteBufCodecs.idMapper(
            i -> Action.values()[i], Action::ordinal).map(PowerKeyPayload::new, PowerKeyPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
