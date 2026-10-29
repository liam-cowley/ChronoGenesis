package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Client to server: a block of a Titanosaur's cargo was used or broken. {@code offset} is the block's place in the
 * cargo (from the harness), {@code face} the side clicked and {@code hit} the point clicked, both in the cargo's frame.
 */
public record CargoActionPayload(int beast, boolean breaking, BlockPos offset, Direction face, Vec3 hit) implements CustomPacketPayload {
    public static final Type<CargoActionPayload> TYPE = new Type<>(ChronoGenesis.id("cargo_action"));
    private static final StreamCodec<FriendlyByteBuf, Vec3> VEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    public static final StreamCodec<FriendlyByteBuf, CargoActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CargoActionPayload::beast,
            ByteBufCodecs.BOOL, CargoActionPayload::breaking,
            BlockPos.STREAM_CODEC, CargoActionPayload::offset,
            Direction.STREAM_CODEC, CargoActionPayload::face,
            VEC, CargoActionPayload::hit,
            CargoActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
