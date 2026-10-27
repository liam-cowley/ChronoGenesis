package com.chronogenesis.essence;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Swappable Chrono Syringe needles. Each one draws a single aspect (core-systems.md §1, "Units and containers"). */
public enum Needle implements StringRepresentable {
    /** Iron is heavy. */
    IRON("iron", Aspect.DENSITY),
    /** Gold tools are the fastest in Minecraft. */
    GOLD("gold", Aspect.SPEED),
    /** Bone is the frame that gives a body its size. */
    BONE("bone", Aspect.SIZE),
    /** Amethyst resonates. Draws tiny amounts of Time straight into a Stasis Ampoule. */
    AMETHYST("amethyst", Aspect.TIME);

    public static final Codec<Needle> CODEC = StringRepresentable.fromEnum(Needle::values);
    public static final StreamCodec<ByteBuf, Needle> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Needle::ordinal);

    private final String name;
    private final Aspect aspect;

    Needle(String name, Aspect aspect) {
        this.name = name;
        this.aspect = aspect;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Aspect aspect() {
        return aspect;
    }
}
