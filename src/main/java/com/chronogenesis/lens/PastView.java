package com.chronogenesis.lens;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * How a structure looked in the past (core-systems.md section 7: "every ruin structure has a matching past
 * template"). Data-driven: {@code data/<ns>/chronogenesis/past_view/<name>.json}. Server-side only.
 *
 * @param structure the ruin as it generates in the world
 * @param template  the structure template of the same piece in its prime, placed the same way
 * @param depth     the Lens setting needed to see that far back
 * @param clue      clue granted the first time someone sees it through the Lens, if any
 */
public record PastView(ResourceKey<Structure> structure, ResourceLocation template, LensDepth depth,
                       Optional<ResourceLocation> clue) {
    public static final Codec<PastView> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.STRUCTURE).fieldOf("structure").forGetter(PastView::structure),
            ResourceLocation.CODEC.fieldOf("template").forGetter(PastView::template),
            LensDepth.CODEC.optionalFieldOf("depth", LensDepth.DECADES).forGetter(PastView::depth),
            ResourceLocation.CODEC.optionalFieldOf("clue").forGetter(PastView::clue)
    ).apply(instance, PastView::new));
}
