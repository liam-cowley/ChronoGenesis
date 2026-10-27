package com.chronogenesis.essence;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The five essences: physical, almost philosophical attributes of a creature (core-systems.md §1).
 * Stability decides which containers can hold it.
 */
public enum Aspect implements StringRepresentable {
    DENSITY("density", 0x6E7F94, Stability.STABLE, ChatFormatting.GRAY),
    SPEED("speed", 0xB5C83A, Stability.STABLE, ChatFormatting.YELLOW),
    SIZE("size", 0xB7742E, Stability.STABLE, ChatFormatting.GOLD),
    AWARENESS("awareness", 0xB9A3E3, Stability.VOLATILE, ChatFormatting.LIGHT_PURPLE),
    TIME("time", 0x2FA59A, Stability.UNSTABLE, ChatFormatting.DARK_AQUA);

    public static final Codec<Aspect> CODEC = StringRepresentable.fromEnum(Aspect::values);
    public static final StreamCodec<ByteBuf, Aspect> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Aspect::ordinal);

    private final String name;
    private final int color;
    private final Stability stability;
    private final ChatFormatting chatColor;

    Aspect(String name, int color, Stability stability, ChatFormatting chatColor) {
        this.name = name;
        this.color = color;
        this.stability = stability;
        this.chatColor = chatColor;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** RGB colour of the essence, used for tinting liquids and particles. */
    public int color() {
        return color;
    }

    public Stability stability() {
        return stability;
    }

    public Component displayName() {
        return Component.translatable("aspect.chronogenesis." + name).withStyle(chatColor);
    }

    public enum Stability {
        /** Fits in syringes, vials and jugs. */
        STABLE,
        /** Evaporates out of glass: only grown machines can take it. */
        VOLATILE,
        /** Ages glass to sand: only Stasis Ampoules and Stasis Bladders. */
        UNSTABLE
    }
}
