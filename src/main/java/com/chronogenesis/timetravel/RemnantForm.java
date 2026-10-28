package com.chronogenesis.timetravel;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * What a lasting block becomes over the ages ({@code data/<ns>/data_maps/block/remnant.json}). A block with an entry
 * outlasts time: placed in a past era, it remains, crumbled, in the present and in the Temporal Lens's past.
 * Without {@code into} it stays as it is (gold does not tarnish). See {@link Remnants}.
 */
public record RemnantForm(Optional<Block> into) {
    public static final Codec<RemnantForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("into").forGetter(RemnantForm::into)
    ).apply(instance, RemnantForm::new));
}
