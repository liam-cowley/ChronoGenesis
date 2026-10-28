package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: shift me into this era, Chronocortex at {@code pos}. */
public record ShiftPayload(BlockPos pos, ResourceLocation era) implements CustomPacketPayload {
    public static final Type<ShiftPayload> TYPE = new Type<>(ChronoGenesis.id("shift"));
    public static final StreamCodec<FriendlyByteBuf, ShiftPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShiftPayload::pos,
            ResourceLocation.STREAM_CODEC, ShiftPayload::era,
            ShiftPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
