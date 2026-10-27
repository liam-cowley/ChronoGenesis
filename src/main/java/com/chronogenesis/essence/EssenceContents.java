package com.chronogenesis.essence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * An amount of one aspect, in drams. Used as the item data component on syringes, vials,
 * ampoules and cultures, and inside jugs and organisms.
 */
public record EssenceContents(Aspect aspect, int amount) {
    public static final Codec<EssenceContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Aspect.CODEC.fieldOf("aspect").forGetter(EssenceContents::aspect),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("amount").forGetter(EssenceContents::amount)
    ).apply(instance, EssenceContents::new));

    public static final StreamCodec<ByteBuf, EssenceContents> STREAM_CODEC = StreamCodec.composite(
            Aspect.STREAM_CODEC, EssenceContents::aspect,
            ByteBufCodecs.VAR_INT, EssenceContents::amount,
            EssenceContents::new);

    public EssenceContents withAmount(int newAmount) {
        return new EssenceContents(aspect, newAmount);
    }

    public boolean isEmpty() {
        return amount <= 0;
    }
}
