package com.chronogenesis.biocomputer;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.organism.OrganismCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** What the Chronocortex reports about one wired organism. Sent to the client for its screen. */
public record OrganismReport(BlockPos pos, Aspect aspect, int stored, int capacity, int size, int instability, int burstAt) {
    public static final StreamCodec<FriendlyByteBuf, OrganismReport> STREAM_CODEC = StreamCodec.of(
            (buf, report) -> {
                buf.writeBlockPos(report.pos);
                buf.writeEnum(report.aspect);
                buf.writeVarInt(report.stored);
                buf.writeVarInt(report.capacity);
                buf.writeVarInt(report.size);
                buf.writeVarInt(report.instability);
                buf.writeVarInt(report.burstAt);
            },
            buf -> new OrganismReport(buf.readBlockPos(), buf.readEnum(Aspect.class), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

    public static OrganismReport of(OrganismCoreBlockEntity core) {
        return new OrganismReport(core.getBlockPos(), core.aspect(), core.stored(), core.capacity(), core.size() + 1,
                core.instability(), OrganismCoreBlockEntity.BURST_AT);
    }

    /** The same three hunger words the organisms use themselves. */
    public String hunger() {
        return stored < capacity / 4 ? "starving" : stored < capacity * 3 / 4 ? "hungry" : "sated";
    }
}
