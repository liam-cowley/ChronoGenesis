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

/**
 * Server to client: open (or refresh) the Chronocortex screen with what it currently knows. {@code core}: opened at the
 * jar itself, where its sense of time is (only there can it shift anyone).
 */
public record OpenChronocortexPayload(BlockPos pos, int stage, List<OrganismReport> organisms, Map<Aspect, Integer> essence,
                                      Mind mind, boolean core) implements CustomPacketPayload {
    /**
     * Its mind: lobes (room), awareness, lessons learned, its name, whether the next stage's structure is whole, eras
     * remembered, Time Capsules built into it.
     */
    public record Mind(int lobes, int awareness, List<ResourceLocation> lessons, String name, boolean structure,
                       List<ResourceLocation> memories, int capsules) {
        private static final StreamCodec<io.netty.buffer.ByteBuf, List<ResourceLocation>> IDS = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list());
        public static final StreamCodec<FriendlyByteBuf, Mind> STREAM_CODEC = StreamCodec.of((buf, mind) -> {
            buf.writeVarInt(mind.lobes());
            buf.writeVarInt(mind.awareness());
            IDS.encode(buf, mind.lessons());
            buf.writeUtf(mind.name());
            buf.writeBoolean(mind.structure());
            IDS.encode(buf, mind.memories());
            buf.writeVarInt(mind.capsules());
        }, buf -> new Mind(buf.readVarInt(), buf.readVarInt(), IDS.decode(buf), buf.readUtf(), buf.readBoolean(), IDS.decode(buf), buf.readVarInt()));
    }

    public static final Type<OpenChronocortexPayload> TYPE = new Type<>(ChronoGenesis.id("open_chronocortex"));
    public static final StreamCodec<FriendlyByteBuf, OpenChronocortexPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenChronocortexPayload::pos,
            ByteBufCodecs.VAR_INT, OpenChronocortexPayload::stage,
            OrganismReport.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenChronocortexPayload::organisms,
            ByteBufCodecs.map(size -> new EnumMap<>(Aspect.class), Aspect.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            OpenChronocortexPayload::essence,
            Mind.STREAM_CODEC, OpenChronocortexPayload::mind,
            ByteBufCodecs.BOOL, OpenChronocortexPayload::core,
            OpenChronocortexPayload::new);

    public static OpenChronocortexPayload of(ChronocortexBlockEntity cortex, boolean core) {
        int next = Math.min(ChronocortexBlockEntity.MAX_STAGE, cortex.stage() + 1);
        Mind mind = new Mind(cortex.lobes(), cortex.awareness(), List.copyOf(cortex.lessons()),
                cortex.name() == null ? "" : cortex.name(), cortex.structureComplete(Math.max(2, next)), List.copyOf(cortex.memories()),
                cortex.capsules());
        return new OpenChronocortexPayload(cortex.getBlockPos(), cortex.stage(), cortex.reports(), cortex.availableEssence(), mind, core);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
