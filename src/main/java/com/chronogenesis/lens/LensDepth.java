package com.chronogenesis.lens;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The Temporal Lens power setting: how far back it looks (core-systems.md section 7). Deeper costs more Time.
 * "Ages" (the land itself changing) needs a stage 1 Chronocortex and is not built yet.
 */
public enum LensDepth implements StringRepresentable {
    /** Hours: ghosts of recent events. Nothing to see yet; cheap. */
    ECHO("echo", 1),
    /** Ruined labs with their lights on; faded blueprints become readable. */
    DECADES("decades", 1),
    /** Ruins whole again: castles, bridges. */
    CENTURIES("centuries", 2);

    public static final Codec<LensDepth> CODEC = StringRepresentable.fromEnum(LensDepth::values);
    public static final StreamCodec<ByteBuf, LensDepth> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], LensDepth::ordinal);

    private final String name;
    private final int cost;

    LensDepth(String name, int cost) {
        this.name = name;
        this.cost = cost;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** Drams of Time burnt every two seconds of looking. */
    public int cost() {
        return cost;
    }

    public LensDepth next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean reaches(LensDepth needed) {
        return ordinal() >= needed.ordinal();
    }

    public Component displayName() {
        return Component.translatable("lens.chronogenesis.depth." + name);
    }
}
