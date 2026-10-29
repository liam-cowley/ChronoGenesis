package com.chronogenesis.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.chronogenesis.essence.Aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Eldritch powers (core-systems.md section 3): one for each essence, woken at the depth only an Enhancing Gland
 * with the Unbegotten's heart in it reaches (power level 4). They are what the Unbegotten could do, in a body that
 * was never meant to.
 */
public final class EldritchPowers {
    /** How far Thousand Eyes sees, through anything. */
    public static final double EYES_RADIUS = 48.0;
    private static final double REACH = 24.0;

    /** A Collapse in progress: where, how strong, who made it, when it crushes. */
    private record Collapse(ServerLevel level, Vec3 at, int strength, UUID caster, long crushAt) {}

    private static final List<Collapse> COLLAPSES = new ArrayList<>();

    private EldritchPowers() {}

    /** The Eldritch power of an essence. */
    public static Power of(Aspect aspect) {
        return switch (aspect) {
            case DENSITY -> Power.COLLAPSE;
            case SPEED -> Power.SLIP;
            case SIZE -> Power.WRITHE;
            case AWARENESS -> Power.THOUSAND_EYES;
            case TIME -> Power.AEON;
        };
    }

    private static Vec3 lookedAt(ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static List<LivingEntity> creatures(ServerPlayer player, Vec3 at, double radius) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != player && e.isAlive() && !(e instanceof Player other && (other.isCreative() || other.isSpectator()))
                        && e.position().distanceTo(at) <= radius);
    }

    // ---------------------------------------------------------------- Collapse (Density)

    /** A point of crushing weight where you look: for a moment everything near falls into it, then it is crushed. */
    static void collapse(ServerPlayer player, int strength) {
        if (!Powers.spend(player, Power.COLLAPSE, 8 + 2 * strength)) {
            return;
        }
        Vec3 at = lookedAt(player, REACH);
        COLLAPSES.add(new Collapse(player.serverLevel(), at, strength, player.getUUID(), player.level().getGameTime() + 30));
        player.level().playSound(null, BlockPos.containing(at), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.5f, 0.4f);
    }

    // ---------------------------------------------------------------- Slip (Speed)

    /** Through the gap between two moments: you are where you were looking (short of anything solid). */
    static void slip(ServerPlayer player, int strength) {
        Vec3 eye = player.getEyePosition();
        double reach = 8.0 + 8.0 * strength;
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 to = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(player.getLookAngle().scale(0.6));
        // Feet where the eyes would be, settled onto whatever is below.
        Vec3 feet = to.subtract(0, player.getEyeHeight() * 0.5, 0);
        if (!player.level().noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(feet))) {
            feet = to.subtract(0, player.getEyeHeight(), 0);
            if (!player.level().noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(feet))) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.chronogenesis.power.slip.blocked")
                        .withStyle(net.minecraft.ChatFormatting.GRAY), true);
                return;
            }
        }
        if (!Powers.spend(player, Power.SLIP, 4 + 2 * strength)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(0.5), player.getZ(), 40, 0.3, 0.8, 0.3, 0.05);
        player.teleportTo(feet.x, feet.y, feet.z);
        player.resetFallDistance();
        level.sendParticles(ParticleTypes.PORTAL, feet.x, feet.y + 1, feet.z, 40, 0.3, 0.8, 0.3, 0.2);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    // ---------------------------------------------------------------- Writhe (Size)

    /** Tentacles burst from the ground round you: everything near is struck, flung up and held. */
    static void writhe(ServerPlayer player, int strength) {
        if (!Powers.spend(player, Power.WRITHE, 8 + 2 * strength)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double radius = 4.0 + 2.0 * strength;
        for (int i = 0; i < 10 + 4 * strength; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 1.5 + level.random.nextDouble() * radius;
            BlockPos ground = BlockPos.containing(player.getX() + Math.cos(a) * r, player.getY() - 0.5, player.getZ() + Math.sin(a) * r);
            BlockState below = level.getBlockState(ground);
            if (!below.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), ground.getX() + 0.5, ground.getY() + 1.1,
                        ground.getZ() + 0.5, 12, 0.2, 0.4, 0.2, 0.15);
            }
            level.sendParticles(ParticleTypes.SQUID_INK, ground.getX() + 0.5, ground.getY() + 1.5, ground.getZ() + 0.5, 6, 0.1, 0.8, 0.1, 0.02);
        }
        for (LivingEntity creature : creatures(player, player.position(), radius)) {
            creature.hurt(player.damageSources().indirectMagic(player, player), 6.0f + 2.0f * strength);
            creature.setDeltaMovement(creature.getDeltaMovement().add(0, 0.7 + 0.15 * strength, 0));
            creature.hurtMarked = true;
            creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + 20 * strength, 3, false, true));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.2f, 1.2f);
        level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.5f, 0.4f);
    }

    // ---------------------------------------------------------------- Aeon (Time)

    /** What you look at lives an aeon in a moment: creatures in a cone wither and slow; plants grow, and grow old. */
    static void aeon(ServerPlayer player, int strength) {
        if (!Powers.spend(player, Power.AEON, 8 + 3 * strength)) {
            return;
        }
        Powers.expose(player, 10 + 4 * strength);
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = 8.0 + 4.0 * strength;
        for (LivingEntity creature : creatures(player, eye, reach)) {
            Vec3 to = creature.getEyePosition().subtract(eye);
            if (to.normalize().dot(look) > 0.8) {
                creature.addEffect(new MobEffectInstance(MobEffects.WITHER, 100 + 40 * strength, strength, false, true));
                creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2, false, true));
                creature.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true));
                if (creature.isInvertedHealAndHarm()) {
                    // The dead are already old: what is left of them goes to dust.
                    creature.hurt(player.damageSources().indirectMagic(player, player), 6.0f * strength);
                }
            }
        }
        for (int step = 2; step <= reach; step += 2) {
            Vec3 at = eye.add(look.scale(step));
            level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, 10, step * 0.15, step * 0.15, step * 0.15, 0.0);
            BlockPos ground = BlockPos.containing(at);
            for (BlockPos pos : BlockPos.betweenClosed(ground.offset(-1, -2, -1), ground.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(level, pos, state)
                        && level.random.nextInt(3) == 0) {
                    plant.performBonemeal(level, level.random, pos.immutable(), state);
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.2f, 0.5f);
    }

    // ---------------------------------------------------------------- every tick

    static void tick(ServerPlayer player, PlayerEssence essence, long time) {
        if (essence.isOn(Power.THOUSAND_EYES)) {
            // Nothing blinds a thousand eyes.
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
        }
        if (player.tickCount % 2 == 0) {
            tickCollapses(player.serverLevel(), time, player.getUUID());
        }
    }

    private static void tickCollapses(ServerLevel level, long time, UUID ticker) {
        for (Iterator<Collapse> it = COLLAPSES.iterator(); it.hasNext(); ) {
            Collapse collapse = it.next();
            if (collapse.level() != level || !collapse.caster().equals(ticker)) {
                continue;
            }
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(collapse.caster());
            double radius = 6.0 + 2.0 * collapse.strength();
            Vec3 at = collapse.at();
            if (caster == null) {
                it.remove();
                continue;
            }
            if (time < collapse.crushAt()) {
                for (LivingEntity creature : creatures(caster, at, radius)) {
                    Vec3 pull = at.subtract(creature.position()).normalize().scale(0.25);
                    creature.setDeltaMovement(creature.getDeltaMovement().scale(0.6).add(pull));
                    creature.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 20, radius * 0.3, radius * 0.3, radius * 0.3, -0.3);
            } else {
                for (LivingEntity creature : creatures(caster, at, radius * 0.6)) {
                    creature.hurt(caster.damageSources().indirectMagic(caster, caster), 8.0f + 4.0f * collapse.strength());
                    creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, true));
                }
                level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 3, 0.8, 0.8, 0.8, 0);
                level.playSound(null, BlockPos.containing(at), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.5f);
                it.remove();
            }
        }
    }

    static void forget(UUID player) {
        COLLAPSES.removeIf(collapse -> collapse.caster().equals(player));
    }
}
