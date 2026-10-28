package com.chronogenesis.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The composite powers (two essences each; see {@link Power}) and Stutter's bubble.
 * Actives are fired from {@link Powers#use}; passives work here, from the tick and from events.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class CompositePowers {
    private static final String SLOWED_TAG = "chronogenesis_slowed";

    /** Impetus charges: player -> the tick the charge ends. */
    private static final Map<UUID, Long> CHARGES = new HashMap<>();
    private static final Map<UUID, Set<Integer>> CHARGE_HITS = new HashMap<>();
    /** Ground Slams in the air: player -> strength. */
    private static final Map<UUID, Integer> SLAMS = new HashMap<>();
    /** Stutter bubbles: player -> the tick it ends. */
    private static final Map<UUID, Long> STUTTERS = new HashMap<>();
    /** Premonition: player -> the tick it can foresee again. */
    private static final Map<UUID, Long> FORESIGHT = new HashMap<>();

    private CompositePowers() {}

    // ---------------------------------------------------------------- actives

    /** Impetus (Density + Speed): a charge forward. What you run into is thrown aside and hurt. */
    static void impetus(ServerPlayer player, int strength) {
        if (!Powers.spend(player, Power.IMPETUS, 2 + strength)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize().scale(1.2 + 0.4 * strength);
        player.setDeltaMovement(flat.x, Math.max(0.1, player.getDeltaMovement().y), flat.z);
        player.hurtMarked = true;
        CHARGES.put(player.getUUID(), player.level().getGameTime() + 8 + 2L * strength);
        CHARGE_HITS.put(player.getUUID(), new HashSet<>());
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    /** Ground Slam (Density + Size): drop like a stone. Where you land, the ground heaves. */
    static void groundSlam(ServerPlayer player, int strength) {
        if (!Powers.spend(player, Power.GROUND_SLAM, 2 + strength)) {
            return;
        }
        if (player.onGround()) {
            shockwave(player, strength);
        } else {
            player.setDeltaMovement(player.getDeltaMovement().x * 0.2, -2.5, player.getDeltaMovement().z * 0.2);
            player.hurtMarked = true;
            SLAMS.put(player.getUUID(), strength);
        }
    }

    private static void shockwave(ServerPlayer player, int strength) {
        ServerLevel level = player.serverLevel();
        double radius = 3.0 + strength;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2, radius),
                e -> e != player && e.isAlive())) {
            double distance = Math.max(1.0, target.distanceTo(player));
            target.hurt(player.damageSources().playerAttack(player), (float) ((3.0 + 2.0 * strength) / Math.sqrt(distance)));
            Vec3 away = target.position().subtract(player.position()).normalize().scale(0.6);
            target.push(away.x, 0.5 + 0.15 * strength, away.z);
            target.hurtMarked = true;
        }
        BlockState ground = level.getBlockState(player.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX(), player.getY() + 0.1,
                    player.getZ(), 60, radius / 2, 0.1, radius / 2, 0.15);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.5f);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4f, 1.4f);
    }

    /** Stutter (Speed + Time): a bubble where everything moves at half speed, projectiles too. */
    static void stutter(ServerPlayer player, int strength) {
        if (!Powers.spend(player, Power.STUTTER, 3 + strength)) {
            return;
        }
        Powers.expose(player, 6);
        STUTTERS.put(player.getUUID(), player.level().getGameTime() + 40 + 20L * strength);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    private static double stutterRadius(int strength) {
        return 3.0 + 1.5 * (strength - 1);
    }

    /** Overlook (Size + Awareness): too small to be noticed. Invisible, and hunters lose your trail. */
    static void overlook(ServerPlayer player, int strength) {
        if (!Powers.spend(player, Power.OVERLOOK, 3 + strength)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200 * strength, 0, false, false, true));
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16 + 8 * strength),
                mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        player.serverLevel().sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY(0.5), player.getZ(), 20, 0.3, 0.6, 0.3, 0.01);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    /** Regrowth (Size + Time): your body's time runs back to before the wound. */
    static void regrowth(ServerPlayer player, int strength) {
        if (player.getHealth() >= player.getMaxHealth()
                && player.getActiveEffects().stream().noneMatch(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.chronogenesis.power.regrowth.whole")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return;
        }
        if (!Powers.spend(player, Power.REGROWTH, 3 + 2 * strength)) {
            return;
        }
        player.heal(4.0f * strength + 2.0f);
        player.getActiveEffects().stream()
                .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .map(MobEffectInstance::getEffect).toList()
                .forEach(player::removeEffect);
        Powers.expose(player, 4 * strength);
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(0.5), player.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        player.serverLevel().sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.0), player.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    // ---------------------------------------------------------------- every tick

    static void tick(ServerPlayer player, PlayerEssence essence, long time) {
        UUID id = player.getUUID();
        ServerLevel level = player.serverLevel();

        Long chargeEnd = CHARGES.get(id);
        if (chargeEnd != null) {
            if (time >= chargeEnd) {
                CHARGES.remove(id);
                CHARGE_HITS.remove(id);
            } else {
                charge(player, essence.strength(Power.IMPETUS));
            }
        }

        Integer slam = SLAMS.get(id);
        if (slam != null && (player.onGround() || player.isInWater())) {
            SLAMS.remove(id);
            shockwave(player, slam);
        }

        Long stutterEnd = STUTTERS.get(id);
        if (stutterEnd != null) {
            if (time >= stutterEnd) {
                STUTTERS.remove(id);
            } else {
                double radius = stutterRadius(essence.strength(Power.STUTTER));
                slowAround(player, radius, true);
                if (time % 4 == 0) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 12,
                            radius / 2, 1.0, radius / 2, 0.0);
                }
            }
        }

        if (essence.isOn(Power.DILATION)) {
            slowAround(player, 3.0 + essence.strength(Power.DILATION), false);
        }
        if (essence.isOn(Power.DREAD) && time % 20 == 0) {
            int strength = essence.strength(Power.DREAD);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(6 + 2 * strength),
                    mob -> mob instanceof Enemy && mob.getTarget() == player)) {
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, strength - 1, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, strength - 1, false, true));
            }
        }
    }

    /** The charging body knocks aside whatever it meets. */
    private static void charge(ServerPlayer player, int strength) {
        Set<Integer> hit = CHARGE_HITS.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
        Vec3 motion = player.getDeltaMovement();
        Vec3 flat = new Vec3(motion.x, 0, motion.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? player.getLookAngle() : flat.normalize();
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.8),
                e -> e != player && e.isAlive() && !hit.contains(e.getId()))) {
            hit.add(target.getId());
            target.hurt(player.damageSources().playerAttack(player), 3.0f + 2.0f * strength);
            target.knockback(1.2 + 0.5 * strength, -dir.x, -dir.z);
            target.push(0, 0.3, 0);
            target.hurtMarked = true;
            player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.7f);
        }
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
    }

    /** Slows living things (if {@code creatures}) and projectiles near the player. Each projectile is slowed once. */
    private static void slowAround(ServerPlayer player, double radius, boolean creatures) {
        AABB area = player.getBoundingBox().inflate(radius);
        for (Entity entity : player.serverLevel().getEntities(player, area)) {
            if (creatures && entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 2, false, false));
            } else if (entity instanceof Projectile projectile && projectile.getOwner() != player
                    && !projectile.getPersistentData().getBoolean(SLOWED_TAG)) {
                projectile.getPersistentData().putBoolean(SLOWED_TAG, true);
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(creatures ? 0.5 : 0.3));
                projectile.hurtMarked = true;
            }
        }
    }

    // ---------------------------------------------------------------- events

    /** A Ground Slam lands on its shockwave, not on your legs. */
    @SubscribeEvent
    static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SLAMS.containsKey(player.getUUID())) {
            event.setDamageMultiplier(0.0f);
        }
    }

    /** Reflexes (Speed + Awareness): an incoming projectile may glance off you. */
    @SubscribeEvent
    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerEssence essence = Powers.get(player);
        if (!essence.isOn(Power.REFLEXES) || essence.pool(Aspect.SPEED) <= 0) {
            return;
        }
        int strength = essence.strength(Power.REFLEXES);
        if (player.getRandom().nextFloat() < 0.35f + 0.15f * strength && Powers.spend(player, Aspect.SPEED, 1)) {
            event.setCanceled(true);
            Projectile projectile = event.getProjectile();
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.3).add(0, 0.2, 0));
            projectile.hurtMarked = true;
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
    }

    /** Premonition (Awareness + Time): the next blow from a creature is foreseen, and you are not where it lands. */
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == null) {
            return;
        }
        PlayerEssence essence = Powers.get(player);
        if (!essence.isOn(Power.PREMONITION)) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < FORESIGHT.getOrDefault(player.getUUID(), Long.MIN_VALUE)) {
            return;
        }
        int strength = essence.strength(Power.PREMONITION);
        if (!Powers.spend(player, Power.PREMONITION, 2)) {
            return;
        }
        event.setCanceled(true);
        FORESIGHT.put(player.getUUID(), now + 300 - 60L * strength);
        Powers.expose(player, 3);
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(0.5), player.getZ(), 16,
                0.3, 0.6, 0.3, 0.02);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    static void forget(UUID player) {
        CHARGES.remove(player);
        CHARGE_HITS.remove(player);
        SLAMS.remove(player);
        STUTTERS.remove(player);
        FORESIGHT.remove(player);
    }

    /** For tests and the HUD: whether a Ground Slam is falling. */
    public static boolean slamming(Player player) {
        return SLAMS.containsKey(player.getUUID());
    }
}
