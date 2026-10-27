package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.player.PlayerEssence;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the player's own essence state (for the HUD, Iron Fists and Awareness sight). */
public record SyncPlayerEssencePayload(PlayerEssence essence) implements CustomPacketPayload {
    public static final Type<SyncPlayerEssencePayload> TYPE = new Type<>(ChronoGenesis.id("sync_player_essence"));
    public static final StreamCodec<ByteBuf, SyncPlayerEssencePayload> STREAM_CODEC =
            PlayerEssence.STREAM_CODEC.map(SyncPlayerEssencePayload::new, SyncPlayerEssencePayload::essence);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
