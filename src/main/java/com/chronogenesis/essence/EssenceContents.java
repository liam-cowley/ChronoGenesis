package com.chronogenesis.essence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Essence held by an item or block: one aspect, a number of drams. An empty container simply has no
 * {@code ModDataComponents.ESSENCE} component.
 */
public record EssenceContents(Aspect aspect, int drams) {
    public static final Codec<EssenceContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Aspect.CODEC.fieldOf("aspect").forGetter(EssenceContents::aspect),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("drams").forGetter(EssenceContents::drams)
    ).apply(instance, EssenceContents::new));

    public static final StreamCodec<ByteBuf, EssenceContents> STREAM_CODEC = StreamCodec.composite(
            Aspect.STREAM_CODEC, EssenceContents::aspect,
            ByteBufCodecs.VAR_INT, EssenceContents::drams,
            EssenceContents::new);
}
