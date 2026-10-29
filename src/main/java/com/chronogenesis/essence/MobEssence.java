package com.chronogenesis.essence;

import com.chronogenesis.registry.ModAttachments;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Read and drain the essence held by a living mob. Server-side only. */
public final class MobEssence {
    private MobEssence() {}

    public static int max(LivingEntity entity, Aspect aspect) {
        return EssenceProfile.of(entity).max(aspect);
    }

    public static int remaining(LivingEntity entity, Aspect aspect) {
        int max = max(entity, aspect);
        if (max == 0) {
            return 0;
        }
        int drained = entity.getData(ModAttachments.DRAINED_ESSENCE).get(aspect);
        return Math.max(0, max - drained);
    }

    /** Fraction of the aspect left, 1.0 for untouched (or for mobs that never had any). */
    public static float fraction(LivingEntity entity, Aspect aspect) {
        int max = max(entity, aspect);
        if (max == 0) {
            return 1.0f;
        }
        return (float) remaining(entity, aspect) / max;
    }

    /** Only living mobs hold essence: not players, not armor stands. */
    public static boolean canBeDrained(LivingEntity entity) {
        return entity instanceof Mob && entity.isAlive();
    }

    /**
     * Takes up to {@code amount} drams of an aspect from the mob, applies the visible effects,
     * and returns how much was actually taken.
     */
    public static int drain(LivingEntity entity, Aspect aspect, int amount) {
        if (!canBeDrained(entity) || amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, remaining(entity, aspect));
        if (taken <= 0) {
            return 0;
        }
        DrainedEssence current = entity.getData(ModAttachments.DRAINED_ESSENCE);
        entity.setData(ModAttachments.DRAINED_ESSENCE, current.with(aspect, current.get(aspect) + taken));
        DrainEffects.onDrained(entity, aspect, taken);
        fossilise(entity, aspect);
        return taken;
    }

    /**
     * A dinosaur in the Jurassic Age with the last of its Time drawn out leaves a fossil of its kind: the shape of it,
     * set hard (fed to the Chronocortex, it can bud the creature's eggs).
     */
    private static void fossilise(LivingEntity entity, Aspect aspect) {
        if (aspect == Aspect.TIME && remaining(entity, Aspect.TIME) == 0 && com.chronogenesis.entity.Dinos.isDino(entity.getType())
                && entity.level().dimension().location().equals(com.chronogenesis.ChronoGenesis.id("jurassic_age"))) {
            entity.spawnAtLocation(com.chronogenesis.item.FossilItem.of(entity.getType()));
            entity.playSound(net.minecraft.sounds.SoundEvents.BONE_BLOCK_BREAK, 1.0f, 0.6f);
        }
    }

    /** Gives essence back (Temporal Fields, injecting a mob). Returns how much was restored. */
    public static int restore(LivingEntity entity, Aspect aspect, int amount) {
        DrainedEssence current = entity.getData(ModAttachments.DRAINED_ESSENCE);
        int restored = Math.min(amount, current.get(aspect));
        if (restored <= 0) {
            return 0;
        }
        entity.setData(ModAttachments.DRAINED_ESSENCE, current.with(aspect, current.get(aspect) - restored));
        DrainEffects.refresh(entity);
        return restored;
    }
}
