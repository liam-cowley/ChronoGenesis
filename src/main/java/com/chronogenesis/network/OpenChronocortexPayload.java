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
import net.minecraft.resources.ResourceLocation;

/** Server to client: open (or refresh) the Chronocortex screen with what it currently knows. */
public record OpenChronocortexPayload(BlockPos pos, int stage, List<OrganismReport> organisms, Map<Aspect, Integer> essence,
                                      Mind mind) implements CustomPacketPayload {
    /** Its mind: lobes (room), awareness, lessons learned, its name, whether its cranium is whole, eras remembered. */
    public record Mind(int lobes, int awareness, List<ResourceLocation> lessons, String name, boolean cranium,
                       List<ResourceLocation> memories) {
        public static final StreamCodec<FriendlyByteBuf, Mind> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Mind::lobes,
                ByteBufCodecs.VAR_INT, Mind::awareness,
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Mind::lessons,
                ByteBufCodecs.STRING_UTF8, Mind::name,
                ByteBufCodecs.BOOL, Mind::cranium,
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Mind::memories,
                Mind::new);
    }

    public static final Type<OpenChronocortexPayload> TYPE = new Type<>(ChronoGenesis.id("open_chronocortex"));
    public static final StreamCodec<FriendlyByteBuf, OpenChronocortexPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenChronocortexPayload::pos,
            ByteBufCodecs.VAR_INT, OpenChronocortexPayload::stage,
            OrganismReport.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenChronocortexPayload::organisms,
            ByteBufCodecs.map(size -> new EnumMap<>(Aspect.class), Aspect.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            OpenChronocortexPayload::essence,
            Mind.STREAM_CODEC, OpenChronocortexPayload::mind,
            OpenChronocortexPayload::new);

    public static OpenChronocortexPayload of(ChronocortexBlockEntity cortex) {
        Mind mind = new Mind(cortex.lobes(), cortex.awareness(), List.copyOf(cortex.lessons()),
                cortex.name() == null ? "" : cortex.name(), cortex.craniumComplete(), List.copyOf(cortex.memories()));
        return new OpenChronocortexPayload(cortex.getBlockPos(), cortex.stage(), cortex.reports(), cortex.availableEssence(), mind);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
