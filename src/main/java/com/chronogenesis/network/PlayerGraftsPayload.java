package com.chronogenesis.network;

import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.player.PlayerGraft;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: the grafts grown into a player's body (sent to them and to everyone who can see them). */
public record PlayerGraftsPayload(int entity, List<PlayerGraft> grafts) implements CustomPacketPayload {
    public static final Type<PlayerGraftsPayload> TYPE = new Type<>(ChronoGenesis.id("player_grafts"));
    public static final StreamCodec<ByteBuf, PlayerGraftsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerGraftsPayload::entity,
            PlayerGraft.STREAM_CODEC.apply(ByteBufCodecs.list()), PlayerGraftsPayload::grafts,
            PlayerGraftsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
