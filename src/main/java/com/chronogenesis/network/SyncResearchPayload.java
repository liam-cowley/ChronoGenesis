package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.PlayerResearch;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the player's clues and completed research, for the journal and the research screen. */
public record SyncResearchPayload(PlayerResearch research) implements CustomPacketPayload {
    public static final Type<SyncResearchPayload> TYPE = new Type<>(ChronoGenesis.id("sync_research"));
    public static final StreamCodec<ByteBuf, SyncResearchPayload> STREAM_CODEC =
            PlayerResearch.STREAM_CODEC.map(SyncResearchPayload::new, SyncResearchPayload::research);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
