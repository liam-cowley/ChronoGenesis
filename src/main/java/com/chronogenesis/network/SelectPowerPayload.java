package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a power was picked in the radial menu. */
public record SelectPowerPayload(Aspect aspect) implements CustomPacketPayload {
    public static final Type<SelectPowerPayload> TYPE = new Type<>(ChronoGenesis.id("select_power"));
    public static final StreamCodec<ByteBuf, SelectPowerPayload> STREAM_CODEC =
            Aspect.STREAM_CODEC.map(SelectPowerPayload::new, SelectPowerPayload::aspect);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
