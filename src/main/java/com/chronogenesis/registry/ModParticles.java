package com.chronogenesis.registry;

import java.util.function.Supplier;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EssenceMoteOptions;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particle types. Their client-side looks are registered in {@code client/ClientEvents}. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ChronoGenesis.MODID);

    /** Essence being pulled out of a creature towards whatever drinks it (a Leech Tendril). */
    public static final Supplier<ParticleType<EssenceMoteOptions>> ESSENCE_MOTE = PARTICLE_TYPES.register("essence_mote",
            () -> new ParticleType<EssenceMoteOptions>(false) {
                @Override
                public MapCodec<EssenceMoteOptions> codec() {
                    return EssenceMoteOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, EssenceMoteOptions> streamCodec() {
                    return EssenceMoteOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {}
}
