package com.chronogenesis.network;

import java.util.ArrayList;
import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.PastViews;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server to client: what the Temporal Lens shows right now. {@code positions}/{@code states} are the past's
 * blocks, {@code erased} the present blocks that were not there then. All empty: the view closes.
 */
public record PastViewPayload(List<BlockPos> positions, List<BlockState> states, List<BlockPos> erased)
        implements CustomPacketPayload {
    public static final Type<PastViewPayload> TYPE = new Type<>(ChronoGenesis.id("past_view"));
    public static final StreamCodec<ByteBuf, PastViewPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), PastViewPayload::positions,
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY).apply(ByteBufCodecs.list()), PastViewPayload::states,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), PastViewPayload::erased,
            PastViewPayload::new);

    public static final PastViewPayload CLOSED = new PastViewPayload(List.of(), List.of(), List.of());

    public static PastViewPayload of(PastViews.Past past) {
        List<BlockPos> positions = new ArrayList<>(past.blocks().size());
        List<BlockState> states = new ArrayList<>(past.blocks().size());
        for (PastViews.PastBlock block : past.blocks()) {
            positions.add(block.pos());
            states.add(block.state());
        }
        return new PastViewPayload(positions, states, List.copyOf(past.erased()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
