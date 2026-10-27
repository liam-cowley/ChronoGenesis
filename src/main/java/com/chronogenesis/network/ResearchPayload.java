package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: research this topic at the Chronocortex at {@code pos}. */
public record ResearchPayload(BlockPos pos, ResourceLocation topic) implements CustomPacketPayload {
    public static final Type<ResearchPayload> TYPE = new Type<>(ChronoGenesis.id("research"));
    public static final StreamCodec<ByteBuf, ResearchPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ResearchPayload::pos,
            ResourceLocation.STREAM_CODEC, ResearchPayload::topic,
            ResearchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
