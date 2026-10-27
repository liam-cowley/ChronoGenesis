package com.chronogenesis.essence;

import com.chronogenesis.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * A mote of essence being pulled out of a creature: it drifts up out of the body, then homes in on
 * {@code target} (the mouth of whatever is drinking it) and is swallowed there. Tinted by the essence's colour.
 */
public record EssenceMoteOptions(int color, Vec3 target) implements ParticleOptions {
    public static final MapCodec<EssenceMoteOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("color").forGetter(EssenceMoteOptions::color),
            Vec3.CODEC.fieldOf("target").forGetter(EssenceMoteOptions::target)
    ).apply(instance, EssenceMoteOptions::new));

    public static final StreamCodec<FriendlyByteBuf, EssenceMoteOptions> STREAM_CODEC = StreamCodec.of(
            (buf, options) -> {
                buf.writeInt(options.color);
                buf.writeVec3(options.target);
            },
            buf -> new EssenceMoteOptions(buf.readInt(), buf.readVec3()));

    public static EssenceMoteOptions of(Aspect aspect, Vec3 target) {
        return new EssenceMoteOptions(aspect.color(), target);
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.ESSENCE_MOTE.get();
    }
}
