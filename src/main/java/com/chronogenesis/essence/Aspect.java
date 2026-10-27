package com.chronogenesis.essence;

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The five essences: physical, almost philosophical attributes of a living creature.
 * See docs/design/core-systems.md section 1.
 */
public enum Aspect implements StringRepresentable {
    DENSITY(0x6E7F96, Stability.STABLE),
    SPEED(0xB8C93A, Stability.STABLE),
    SIZE(0xB4762E, Stability.STABLE),
    AWARENESS(0xB9A3E3, Stability.VOLATILE),
    TIME(0x3FD1C2, Stability.UNSTABLE);

    public static final Codec<Aspect> CODEC = StringRepresentable.fromEnum(Aspect::values);
    public static final StreamCodec<ByteBuf, Aspect> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Aspect::ordinal);

    /** How the aspect behaves in containers. */
    public enum Stability {
        /** Fits in syringes, vials and jugs. */
        STABLE,
        /** Evaporates out of glass: only grown organisms can take and hold it. */
        VOLATILE,
        /** Ages ordinary glass to sand: needs Stasis Ampoules and Stasis Bladders. */
        UNSTABLE
    }

    private final int color;
    private final Stability stability;

    Aspect(int color, Stability stability) {
        this.color = color;
        this.stability = stability;
    }

    /** RGB colour used for tinting vials, flesh and the HUD. */
    public int color() {
        return color;
    }

    public Stability stability() {
        return stability;
    }

    /** Whether ordinary glass containers (syringe, vial, jug) can hold this aspect. */
    public boolean fitsInGlass() {
        return stability == Stability.STABLE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "aspect.chronogenesis." + getSerializedName();
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }
}
