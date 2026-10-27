package com.chronogenesis.essence;

import java.util.EnumMap;
import java.util.Map;

import com.chronogenesis.registry.ModDataMaps;
import com.chronogenesis.registry.ModTags;
import com.mojang.serialization.Codec;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * How many drams of each aspect a kind of mob holds when untouched.
 * <p>
 * Data-driven through the {@code chronogenesis:essence_profile} data map on entity types
 * ({@code data/<ns>/data_maps/entity_type/essence_profile.json}). Mobs without an entry get
 * {@link #defaultsFor(LivingEntity) defaults derived from their attributes}.
 */
public record EssenceProfile(Map<Aspect, Integer> maxima) {
    public static final Codec<EssenceProfile> CODEC = Codec.unboundedMap(Aspect.CODEC, ExtraCodecs.NON_NEGATIVE_INT)
            .xmap(EssenceProfile::new, EssenceProfile::maxima);

    public static final EssenceProfile EMPTY = new EssenceProfile(Map.of());

    public int max(Aspect aspect) {
        return maxima.getOrDefault(aspect, 0);
    }

    /** The profile that applies to this living entity right now (players have none). */
    public static EssenceProfile of(LivingEntity entity) {
        if (entity instanceof Player) {
            return EMPTY;
        }
        EssenceProfile data = entity.getType().builtInRegistryHolder().getData(ModDataMaps.ESSENCE_PROFILE);
        EssenceProfile base = data != null ? data : defaultsFor(entity);
        // Babies carry their future: a small Time pool even when the adult has none.
        if (entity instanceof AgeableMob ageable && ageable.isBaby() && base.max(Aspect.TIME) == 0) {
            Map<Aspect, Integer> copy = new EnumMap<>(Aspect.class);
            copy.putAll(base.maxima);
            copy.put(Aspect.TIME, 4);
            return new EssenceProfile(copy);
        }
        return base;
    }

    /** Defaults derived from vanilla attributes, so modded mobs work without data. */
    public static EssenceProfile defaultsFor(LivingEntity entity) {
        Map<Aspect, Integer> map = new EnumMap<>(Aspect.class);
        // Base values only: our own drain modifiers must not feed back into the profile.
        double health = entity.getAttributeBaseValue(Attributes.MAX_HEALTH);
        double armor = entity.getAttributeBaseValue(Attributes.ARMOR);
        double speed = entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        double followRange = entity.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE)
                ? entity.getAttributeBaseValue(Attributes.FOLLOW_RANGE) : 8.0;
        // Use the unscaled hitbox so shrinking a mob doesn't shrink its profile.
        double volume = entity.getType().getWidth() * entity.getType().getWidth() * entity.getType().getHeight();

        map.put(Aspect.DENSITY, atLeastOne(health / 2.0 + armor));
        map.put(Aspect.SPEED, atLeastOne(speed * 40.0));
        map.put(Aspect.SIZE, atLeastOne(volume * 4.0));
        map.put(Aspect.AWARENESS, atLeastOne(followRange / 4.0));
        map.put(Aspect.TIME, entity.getType().is(ModTags.Entities.TIME_BEARERS) ? atLeastOne(health / 4.0) : 0);
        return new EssenceProfile(map);
    }

    private static int atLeastOne(double value) {
        return Math.max(1, Mth.ceil(value));
    }
}
