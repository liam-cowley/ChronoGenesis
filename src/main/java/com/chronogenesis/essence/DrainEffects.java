package com.chronogenesis.essence;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.joml.Vector3f;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The visible consequences of draining an aspect from a mob (core-systems.md section 1):
 * <ul>
 *   <li>Density: floats at a set height; drain more and it rises; only a leash holds it down.</li>
 *   <li>Speed: slows down until rooted.</li>
 *   <li>Size: shrinks.</li>
 *   <li>Awareness: AI updates less and less often, until the mob is Vacant.</li>
 *   <li>Time: babies grow up; adults wither, and desiccate when empty.</li>
 * </ul>
 * Attribute modifiers are transient and recomputed from {@link DrainedEssence} whenever the mob
 * joins a level, so nothing but the drained amounts needs to be saved.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class DrainEffects {
    private static final ResourceLocation SIZE_MOD = ChronoGenesis.id("drained_size");
    private static final ResourceLocation SPEED_MOD = ChronoGenesis.id("drained_speed");
    private static final ResourceLocation FLY_SPEED_MOD = ChronoGenesis.id("drained_flying_speed");
    private static final ResourceLocation GRAVITY_MOD = ChronoGenesis.id("drained_density");
    private static final ResourceLocation AGE_MOD = ChronoGenesis.id("drained_time");

    /** Smallest scale a fully Size-drained mob shrinks to. */
    private static final double MIN_SCALE = 0.25;
    /** Ticks it takes a fully drained, unleashed mob to float out of the world. */
    private static final double FREE_RISE_SPEED = 0.06;

    /** Mobs whose AI we switched off for the current tick (Awareness throttling). */
    private static final Set<Mob> AI_PAUSED = Collections.newSetFromMap(new IdentityHashMap<>());

    private DrainEffects() {}

    // ---------------------------------------------------------------- drain-time effects

    static void onDrained(LivingEntity entity, Aspect aspect, int taken) {
        if (entity.level() instanceof ServerLevel level) {
            Vector3f color = Vec3.fromRGB24(aspect.color()).toVector3f();
            level.sendParticles(new DustParticleOptions(color, 1.0f),
                    entity.getX(), entity.getY(0.6), entity.getZ(), 6 + taken * 2,
                    entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.02);
            level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL,
                    0.6f, 0.6f + entity.getRandom().nextFloat() * 0.3f);
        }
        if (aspect == Aspect.TIME) {
            onTimeDrained(entity, taken);
        }
        refresh(entity);
    }

    private static void onTimeDrained(LivingEntity entity, int taken) {
        if (entity instanceof AgeableMob ageable && ageable.isBaby()) {
            // Stealing a baby's future makes it grow up. 4 drams covers a whole childhood.
            if (MobEssence.remaining(entity, Aspect.TIME) == 0) {
                ageable.setAge(0);
            } else {
                ageable.setAge(Math.min(0, ageable.getAge() + 6000 * taken));
            }
            return;
        }
        if (MobEssence.max(entity, Aspect.TIME) > 0 && MobEssence.remaining(entity, Aspect.TIME) == 0) {
            desiccate(entity);
        }
    }

    /** The mob's time runs out: it crumbles to dust and leaves Temporal Residue behind. */
    public static void desiccate(LivingEntity entity) {
        Level level = entity.level();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ASH, entity.getX(), entity.getY(0.5), entity.getZ(), 40,
                    entity.getBbWidth() * 0.5, entity.getBbHeight() * 0.4, entity.getBbWidth() * 0.5, 0.01);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, entity.getX(), entity.getY(0.5), entity.getZ(), 20,
                    entity.getBbWidth() * 0.5, entity.getBbHeight() * 0.4, entity.getBbWidth() * 0.5, 0.01);
            serverLevel.playSound(null, entity.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.NEUTRAL, 1.0f, 0.5f);
            ItemEntity residue = new ItemEntity(serverLevel, entity.getX(), entity.getY(0.5), entity.getZ(),
                    new ItemStack(ModItems.TEMPORAL_RESIDUE.get()));
            serverLevel.addFreshEntity(residue);
        }
        entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
    }

    // ---------------------------------------------------------------- attribute modifiers

    /** Recomputes all drain-related attribute modifiers from the mob's drained essence. */
    public static void refresh(LivingEntity entity) {
        if (!entity.hasData(ModAttachments.DRAINED_ESSENCE)) {
            return;
        }
        float size = MobEssence.fraction(entity, Aspect.SIZE);
        float speed = MobEssence.fraction(entity, Aspect.SPEED);
        float density = MobEssence.fraction(entity, Aspect.DENSITY);
        float time = MobEssence.fraction(entity, Aspect.TIME);

        setModifier(entity, Attributes.SCALE, SIZE_MOD, -(1.0 - size) * (1.0 - MIN_SCALE));
        setModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_MOD, -(1.0 - speed));
        setModifier(entity, Attributes.FLYING_SPEED, FLY_SPEED_MOD, -(1.0 - speed));
        // Any Density drained: gravity no longer applies, hover steering takes over (see tick).
        setModifier(entity, Attributes.GRAVITY, GRAVITY_MOD, density < 1.0f ? -1.0 : 0.0);
        boolean baby = entity instanceof AgeableMob ageable && ageable.isBaby();
        setModifier(entity, Attributes.MAX_HEALTH, AGE_MOD, baby ? 0.0 : -(1.0 - time) * 0.5);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (amount == 0.0) {
            instance.removeModifier(id);
        } else {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent
    static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            refresh(living);
        }
    }

    // ---------------------------------------------------------------- per-tick behaviour

    @SubscribeEvent
    static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()
                || !entity.hasData(ModAttachments.DRAINED_ESSENCE)) {
            return;
        }
        float density = MobEssence.fraction(entity, Aspect.DENSITY);
        if (density < 1.0f) {
            hover(entity, density);
        }
        float awareness = MobEssence.fraction(entity, Aspect.AWARENESS);
        if (awareness < 1.0f && entity instanceof Mob mob && !mob.isNoAi() && shouldSkipAi(mob, awareness)) {
            mob.setNoAi(true);
            AI_PAUSED.add(mob);
        }
    }

    @SubscribeEvent
    static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && AI_PAUSED.remove(mob)) {
            mob.setNoAi(false);
        }
    }

    /** Safety net: never leave a mob paused if its post-tick event didn't fire. */
    @SubscribeEvent
    static void onServerTickPost(ServerTickEvent.Post event) {
        if (!AI_PAUSED.isEmpty()) {
            AI_PAUSED.forEach(mob -> mob.setNoAi(false));
            AI_PAUSED.clear();
        }
    }

    /**
     * Less Awareness means the AI ticks less often: at 50% it thinks every ~11 ticks,
     * at 0% it never does (the mob is Vacant).
     */
    private static boolean shouldSkipAi(Mob mob, float awareness) {
        if (awareness <= 0.0f) {
            return true;
        }
        int period = 1 + Math.round((1.0f - awareness) * 20.0f);
        return mob.tickCount % period != 0;
    }

    /** Steers a Density-drained mob towards its hover height above the ground. */
    private static void hover(LivingEntity entity, float density) {
        Level level = entity.level();
        boolean leashed = entity instanceof Mob mob && mob.isLeashed();
        Vec3 motion = entity.getDeltaMovement();
        double verticalSpeed;

        if (density <= 0.0f && !leashed) {
            // Nothing holds it to the world any more.
            verticalSpeed = FREE_RISE_SPEED;
            if (entity.getY() > level.getMaxBuildHeight() + 64) {
                entity.discard();
                return;
            }
        } else {
            double hoverHeight = density > 0.5f
                    ? 1.0 + (1.0f - density) * 2.0          // 1-2 blocks
                    : 2.0 + (0.5f - density) * 2.0f * 14.0; // 2-16 blocks
            if (leashed) {
                hoverHeight = Math.min(hoverHeight, 1.0);
            }
            double groundTop = findGroundTop(level, entity.blockPosition());
            if (Double.isNaN(groundTop)) {
                verticalSpeed = 0.0; // Nothing below within range: just hang there.
            } else {
                double bob = Math.sin(entity.tickCount * 0.1) * 0.15;
                double targetY = groundTop + hoverHeight + bob;
                verticalSpeed = Mth.clamp((targetY - entity.getY()) * 0.08, -0.08, 0.08);
            }
        }
        entity.setDeltaMovement(motion.x, verticalSpeed, motion.z);
        entity.resetFallDistance();
        if (entity.tickCount % 5 == 0) {
            entity.hurtMarked = true; // push the new velocity to clients
        }
    }

    /** Top of the first block with collision below {@code start}, or NaN if none within 40 blocks. */
    private static double findGroundTop(Level level, BlockPos start) {
        BlockPos.MutableBlockPos pos = start.mutable();
        for (int i = 0; i < 40 && pos.getY() > level.getMinBuildHeight(); i++) {
            var state = level.getBlockState(pos);
            var shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
            }
            pos.move(0, -1, 0);
        }
        return Double.NaN;
    }
}
