package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.player.Power;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: something done with powers. {@code power} and {@code value} are used by the actions that need
 * them: SELECT and TOGGLE (power), STRENGTH (power, new strength), NUDGE (value: +1 or -1 on the selected power).
 */
public record PowerActionPayload(Action action, Power power, int value) implements CustomPacketPayload {
    public enum Action {
        /** Fire (or switch) the selected active power. */
        USE,
        /** Select an active power on the wheel. */
        SELECT,
        /** Switch a passive power on or off. */
        TOGGLE,
        /** Set a power's strength. */
        STRENGTH,
        /** Raise or lower the selected power's strength by one. */
        NUDGE
    }

    public static final Type<PowerActionPayload> TYPE = new Type<>(ChronoGenesis.id("power_action"));
    public static final StreamCodec<ByteBuf, PowerActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), PowerActionPayload::action,
            Power.STREAM_CODEC, PowerActionPayload::power,
            ByteBufCodecs.VAR_INT, PowerActionPayload::value,
            PowerActionPayload::new);

    public static PowerActionPayload use() {
        return new PowerActionPayload(Action.USE, Power.REWIND, 0);
    }

    public static PowerActionPayload nudge(int delta) {
        return new PowerActionPayload(Action.NUDGE, Power.REWIND, delta);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
