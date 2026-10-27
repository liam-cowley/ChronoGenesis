package com.chronogenesis.network;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.OrganismReport;
import com.chronogenesis.essence.Aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: open (or refresh) the Chronocortex screen with what it currently knows. */
public record OpenChronocortexPayload(BlockPos pos, int stage, List<OrganismReport> organisms, Map<Aspect, Integer> essence)
        implements CustomPacketPayload {
    public static final Type<OpenChronocortexPayload> TYPE = new Type<>(ChronoGenesis.id("open_chronocortex"));
    public static final StreamCodec<FriendlyByteBuf, OpenChronocortexPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenChronocortexPayload::pos,
            ByteBufCodecs.VAR_INT, OpenChronocortexPayload::stage,
            OrganismReport.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenChronocortexPayload::organisms,
            ByteBufCodecs.map(size -> new EnumMap<>(Aspect.class), Aspect.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            OpenChronocortexPayload::essence,
            OpenChronocortexPayload::new);

    public static OpenChronocortexPayload of(ChronocortexBlockEntity cortex) {
        return new OpenChronocortexPayload(cortex.getBlockPos(), cortex.stage(), cortex.reports(), cortex.availableEssence());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
