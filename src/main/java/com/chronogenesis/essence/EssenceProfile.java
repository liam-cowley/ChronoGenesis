package com.chronogenesis.essence;

import java.util.Map;

import com.chronogenesis.registry.ModRegistries;
import com.chronogenesis.registry.ModTags;
import com.mojang.serialization.Codec;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * How many drams of each aspect a kind of creature holds. Data-driven: the file
 * {@code data/<ns>/chronogenesis/essence_profile/<path>.json} applies to the entity type {@code <ns>:<path>},
 * e.g. {@code {"density": 4, "speed": 6}}. Aspects missing from the file (or a missing file) fall back to
 * defaults derived from the creature's attributes.
 */
public record EssenceProfile(Map<Aspect, Integer> drams) {
    public static final Codec<EssenceProfile> CODEC = Codec.unboundedMap(Aspect.CODEC, Codec.intRange(0, 1024))
            .xmap(EssenceProfile::new, EssenceProfile::drams);

    private static final int MAX_DEFAULT = 64;
    private static final int TIME_BEARER_DRAMS = 3;

    /** Drams of {@code aspect} the entity holds when untouched. Players hold none: they are the experimenter. */
    public static int max(LivingEntity entity, Aspect aspect) {
        if (entity instanceof Player) {
            return 0;
        }
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        Integer configured = entity.level().registryAccess().registry(ModRegistries.ESSENCE_PROFILE)
                .map(registry -> registry.get(key))
                .map(profile -> profile.drams().get(aspect))
                .orElse(null);
        return configured != null ? configured : defaultFor(entity, aspect);
    }

    /** Defaults from core-systems.md §1, "Where essence comes from". */
    private static int defaultFor(LivingEntity entity, Aspect aspect) {
        return switch (aspect) {
            case DENSITY -> clamp((entity.getMaxHealth() + 2 * entity.getAttributeBaseValue(Attributes.ARMOR)) / 2.5);
            case SPEED -> clamp(entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 30);
            case SIZE -> {
                var dimensions = entity.getType().getDimensions();
                yield clamp(dimensions.width() * dimensions.width() * dimensions.height() * 6);
            }
            case AWARENESS -> entity.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE)
                    ? clamp(entity.getAttributeBaseValue(Attributes.FOLLOW_RANGE) / 4) : 1;
            case TIME -> entity.getType().is(ModTags.TIME_BEARERS) ? TIME_BEARER_DRAMS : 0;
        };
    }

    private static int clamp(double value) {
        return Mth.clamp((int) Math.round(value), 1, MAX_DEFAULT);
    }
}
