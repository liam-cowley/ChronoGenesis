package com.chronogenesis.network;

import java.util.List;

import com.chronogenesis.ChronoGenesis;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client, every tick while any chunk of the dimension has its own time: each one's rate and lag. */
public record TimeZonesPayload(List<Entry> zones) implements CustomPacketPayload {
    public record Entry(long chunk, float rate, double lag, boolean releasing) {
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Entry::chunk,
                ByteBufCodecs.FLOAT, Entry::rate,
                ByteBufCodecs.DOUBLE, Entry::lag,
                ByteBufCodecs.BOOL, Entry::releasing,
                Entry::new);
    }

    public static final Type<TimeZonesPayload> TYPE = new Type<>(ChronoGenesis.id("time_zones"));
    public static final StreamCodec<ByteBuf, TimeZonesPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(TimeZonesPayload::new, TimeZonesPayload::zones);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
