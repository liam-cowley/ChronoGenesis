package com.chronogenesis.essence;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The per-tick side of draining: Density makes creatures float, Awareness throttles their AI.
 * Attribute effects are re-applied when an entity joins a level, since their modifiers are transient.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class DrainEffects {
    /** Hover height above the ground per unit of Density drained, in blocks. */
    private static final double HOVER_PER_FRACTION = 6;
    private static final double BASE_HOVER = 0.5;
    /** A leash holds a floating creature at most this far above whatever it is tied to. */
    private static final double LEASH_SLACK = 4;
    private static final int GROUND_SCAN_DEPTH = 64;
    /** At most this many skipped AI ticks per AI tick, just before a creature goes Vacant. */
    private static final int MAX_AI_SKIP = 8;

    /** Mobs whose AI was switched off for the current tick only. */
    private static final Set<Mob> SUPPRESSED_THIS_TICK = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living
                && living.hasData(ModAttachments.ESSENCE)) {
            EssenceDrain.applyAttributeEffects(living);
        }
    }

    @SubscribeEvent
    static void onTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide
                || !mob.hasData(ModAttachments.ESSENCE) || mob.isNoAi()) {
            return;
        }
        float awareness = EssenceDrain.drainedFraction(mob, Aspect.AWARENESS);
        if (awareness <= 0) {
            return;
        }
        // The less aware, the less often it thinks. Fully drained, it is Vacant and never does.
        boolean vacant = awareness >= 1;
        int interval = 1 + (int) (awareness * MAX_AI_SKIP);
        if (vacant || mob.tickCount % interval != 0) {
            mob.setNoAi(true);
            SUPPRESSED_THIS_TICK.add(mob);
        }
    }

    @SubscribeEvent
    static void onTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof LivingEntity living) || !living.hasData(ModAttachments.ESSENCE)) {
            return;
        }
        boolean suppressed = living instanceof Mob mob && SUPPRESSED_THIS_TICK.remove(mob);
        if (suppressed) {
            ((Mob) living).setNoAi(false);
        }
        float density = EssenceDrain.drainedFraction(living, Aspect.DENSITY);
        if (density > 0) {
            floatUp(living, density, suppressed);
        }
    }

    private static void floatUp(LivingEntity entity, float density, boolean aiSuppressed) {
        Level level = entity.level();
        double ground = groundBelow(entity);
        double target;
        if (density >= 1 || Double.isNaN(ground)) {
            target = entity.getY() + 1; // fully drained: drifts off into the sky
        } else {
            target = ground + BASE_HOVER + density * HOVER_PER_FRACTION;
        }
        if (entity instanceof Leashable leashable && leashable.isLeashed() && leashable.getLeashHolder() != null) {
            target = Math.min(target, leashable.getLeashHolder().getY() + LEASH_SLACK);
        }
        double bob = Mth.sin((entity.tickCount + entity.getId()) * 0.08f) * 0.004;
        double vy = Mth.clamp((target - entity.getY()) * 0.08, -0.06, density >= 1 ? 0.04 : 0.06) + bob;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, vy, motion.z);
        entity.resetFallDistance();
        if (aiSuppressed) {
            // Without AI the mob does not run its own physics this tick, so carry it ourselves.
            entity.move(MoverType.SELF, new Vec3(0, vy, 0));
        }
        if (entity.getY() > level.getMaxBuildHeight() + 64) {
            entity.discard();
        }
    }

    /** Y of the first solid surface below the entity, or NaN if there is none within reach. */
    private static double groundBelow(LivingEntity entity) {
        Level level = entity.level();
        BlockPos.MutableBlockPos pos = entity.blockPosition().mutable();
        for (int i = 0; i < GROUND_SCAN_DEPTH && pos.getY() > level.getMinBuildHeight(); i++) {
            var state = level.getBlockState(pos);
            var shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty() || !state.getFluidState().isEmpty()) {
                return pos.getY() + (shape.isEmpty() ? 1 : shape.max(Direction.Axis.Y));
            }
            pos.move(0, -1, 0);
        }
        return Double.NaN;
    }

    private DrainEffects() {}
}
