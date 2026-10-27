package com.chronogenesis.essence;

import org.joml.Vector3f;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Taking essence out of living things, and what that does to them (core-systems.md §1).
 * Attribute-based effects (Size, Speed, Density, Time) live here; per-tick effects (floating, AI
 * throttling) live in {@link DrainEffects}.
 */
public final class EssenceDrain {
    private static final ResourceLocation SIZE_MODIFIER = ChronoGenesis.id("drained_size");
    private static final ResourceLocation SPEED_MODIFIER = ChronoGenesis.id("drained_speed");
    private static final ResourceLocation DENSITY_MODIFIER = ChronoGenesis.id("drained_density");
    private static final ResourceLocation TIME_MODIFIER = ChronoGenesis.id("drained_time");

    /** A fully drained creature shrinks to this fraction of its size. */
    private static final double MIN_SCALE = 0.25;
    /** A fully drained Time bearer has lost this fraction of its max health before it desiccates. */
    private static final double MAX_AGEING = 0.6;

    public static EntityEssence essence(LivingEntity entity) {
        return entity.hasData(ModAttachments.ESSENCE) ? entity.getData(ModAttachments.ESSENCE) : EntityEssence.NONE;
    }

    public static int remaining(LivingEntity entity, Aspect aspect) {
        if (aspect == Aspect.TIME && isBaby(entity)) {
            return 1;
        }
        return Math.max(0, EssenceProfile.max(entity, aspect) - essence(entity).drained(aspect));
    }

    /** 0 = untouched, 1 = fully drained. */
    public static float drainedFraction(LivingEntity entity, Aspect aspect) {
        int drained = essence(entity).drained(aspect);
        if (drained <= 0) {
            return 0;
        }
        int max = EssenceProfile.max(entity, aspect);
        return max <= 0 ? 1 : Math.min(1f, (float) drained / max);
    }

    /**
     * Takes up to {@code amount} drams of {@code aspect} from the entity. Server side only.
     *
     * @return the drams actually taken
     */
    public static int drain(LivingEntity entity, Aspect aspect, int amount) {
        if (entity.level().isClientSide || !entity.isAlive() || amount <= 0) {
            return 0;
        }
        // Babies carry their future: taking it makes them adults, without draining anything permanent.
        if (aspect == Aspect.TIME && entity instanceof AgeableMob baby && baby.isBaby()) {
            baby.setAge(0);
            puff(entity, aspect);
            return 1;
        }
        int taken = Math.min(amount, remaining(entity, aspect));
        if (taken <= 0) {
            return 0;
        }
        EntityEssence essence = essence(entity);
        entity.setData(ModAttachments.ESSENCE, essence.withDrained(aspect, essence.drained(aspect) + taken));
        applyAttributeEffects(entity);
        puff(entity, aspect);
        if (aspect == Aspect.TIME && remaining(entity, aspect) == 0) {
            desiccate(entity);
        }
        return taken;
    }

    /**
     * (Re)applies the attribute modifiers for the drained aspects. The modifiers are transient, so this
     * also runs whenever the entity joins a level.
     */
    public static void applyAttributeEffects(LivingEntity entity) {
        float size = drainedFraction(entity, Aspect.SIZE);
        float speed = drainedFraction(entity, Aspect.SPEED);
        float density = drainedFraction(entity, Aspect.DENSITY);
        float time = drainedFraction(entity, Aspect.TIME);

        setModifier(entity, Attributes.SCALE, SIZE_MODIFIER, -(1 - MIN_SCALE) * size);
        // Fully drained of Speed means rooted in place.
        setModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, -speed);
        setModifier(entity, Attributes.FLYING_SPEED, SPEED_MODIFIER, -speed);
        // Any loss of Density lets go of gravity; DrainEffects decides how high it floats.
        setModifier(entity, Attributes.GRAVITY, DENSITY_MODIFIER, density > 0 ? -1 : 0);
        setModifier(entity, Attributes.MAX_HEALTH, TIME_MODIFIER, -MAX_AGEING * time);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Out of time: the creature withers into dust and Temporal Residue. */
    private static void desiccate(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.WHITE_ASH, entity.getX(), entity.getY(0.5), entity.getZ(),
                    40, entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 0.02);
            level.playSound(null, entity.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.NEUTRAL, 1f, 0.6f);
            entity.spawnAtLocation(ModItems.TEMPORAL_RESIDUE.get());
        }
        entity.hurt(entity.damageSources().wither(), Float.MAX_VALUE);
    }

    private static void puff(LivingEntity entity, Aspect aspect) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(dust(aspect), entity.getX(), entity.getY(0.6), entity.getZ(),
                    8, entity.getBbWidth() / 3, entity.getBbHeight() / 4, entity.getBbWidth() / 3, 0);
        }
    }

    public static DustParticleOptions dust(Aspect aspect) {
        int color = aspect.color();
        return new DustParticleOptions(new Vector3f(
                (color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1f);
    }

    public static boolean isBaby(LivingEntity entity) {
        return entity instanceof AgeableMob ageable && ageable.isBaby();
    }

    private EssenceDrain() {}
}
