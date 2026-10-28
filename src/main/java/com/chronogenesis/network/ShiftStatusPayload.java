package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: how long the player's temporal shift still holds (0 = at home), and which era it is. */
public record ShiftStatusPayload(int ticksLeft, Component era) implements CustomPacketPayload {
    public static final Type<ShiftStatusPayload> TYPE = new Type<>(ChronoGenesis.id("shift_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShiftStatusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShiftStatusPayload::ticksLeft,
            ComponentSerialization.TRUSTED_STREAM_CODEC, ShiftStatusPayload::era,
            ShiftStatusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
