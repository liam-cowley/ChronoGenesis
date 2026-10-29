package com.chronogenesis.entity;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.player.TemporalExposure;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.time.TimeZones;
import com.chronogenesis.timetravel.ElderArena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Unbegotten, the Elder God that waits before the beginning of time (core-systems.md section 5, the last era).
 * It floats over its arena, an eye in a mass of flesh hung with tentacles, and:
 * <ul>
 *   <li><b>lashes</b> everyone near it, throwing them back (towards the island's edge, and the nothing past it);</li>
 *   <li><b>gazes</b>: an eye-beam, charged where all can see, that wounds and floods the body with Temporal Exposure
 *       (the suit keeps it out);</li>
 *   <li>below 60% health sends out <b>shreds</b> of itself (vexes); below 30% <b>drags</b> everyone towards it.</li>
 * </ul>
 * While time has not begun, its wounds <b>unhappen</b>: every five seconds it is back as whole as it was five seconds
 * before. Drive the arena's four Unmade Nails and time begins (its wounds stay), or hold it in slowed or stopped
 * time, where it cannot rewind. Slain, it leaves its heart.
 */
public class Unbegotten extends Monster {
    private static final int REWIND_EVERY = 100;
    private static final int LASH_EVERY = 60;
    private static final double LASH_REACH = 9.0;
    private static final int GAZE_EVERY = 140;
    private static final int GAZE_CHARGE = 30;
    private static final double GAZE_REACH = 48.0;
    private static final int SHREDS_EVERY = 300;
    private static final int MAX_SHREDS = 6;
    private static final int DRAG_EVERY = 160;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(Component.translatable("entity.chronogenesis.unbegotten"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true).setCreateWorldFog(true);
    /** Its health over the last {@value #REWIND_EVERY} ticks, sampled every 5. */
    private final Deque<Float> history = new ArrayDeque<>();
    @Nullable
    private BlockPos home;
    private int lash = LASH_EVERY;
    private int gaze = GAZE_EVERY;
    private int gazeCharge;
    @Nullable
    private Player gazeTarget;
    private int shreds = SHREDS_EVERY;
    private int drag = DRAG_EVERY;

    public Unbegotten(EntityType<? extends Unbegotten> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    public void setHome(BlockPos home) {
        this.home = home;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    /** Time has begun around it (all its arena's nails driven): its wounds stay. Without an arena, always. */
    public boolean mortal() {
        return home == null || ElderArena.driven(level(), home) >= 4;
    }

    /** Held in a slowed or stopped chunk: it cannot rewind there. */
    public boolean heldInTime() {
        return TimeZones.at(level(), blockPosition()) != null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        hover();
        Player target = nearestPlayer(GAZE_REACH);
        if (target != null) {
            getLookControl().setLookAt(target, 30.0f, 30.0f);
        }
        if (tickCount % 5 == 0) {
            history.addLast(getHealth());
            while (history.size() > REWIND_EVERY / 5) {
                history.removeFirst();
            }
        }
        if (tickCount % REWIND_EVERY == 0) {
            rewind(level);
        }
        if (--lash <= 0) {
            lash = LASH_EVERY;
            lash(level);
        }
        gazeTick(level, target);
        float health = getHealth() / getMaxHealth();
        if (health < 0.6f && --shreds <= 0) {
            shreds = SHREDS_EVERY;
            sendShreds(level);
        }
        if (health < 0.3f && --drag <= 0) {
            drag = DRAG_EVERY;
            drag(level);
        }
    }

    /** Circles slowly above its arena's middle, bobbing. */
    private void hover() {
        BlockPos centre = home != null ? home : blockPosition();
        double t = tickCount * 0.01;
        Vec3 goal = new Vec3(centre.getX() + 0.5 + Math.cos(t) * 6, centre.getY() + 9 + Math.sin(tickCount * 0.05) * 1.5,
                centre.getZ() + 0.5 + Math.sin(t) * 6);
        Vec3 pull = goal.subtract(position()).scale(0.04);
        if (pull.length() > 0.3) {
            pull = pull.normalize().scale(0.3);
        }
        setDeltaMovement(getDeltaMovement().scale(0.6).add(pull));
    }

    private void rewind(ServerLevel level) {
        if (history.isEmpty() || mortal() || heldInTime()) {
            return;
        }
        float past = history.peekFirst();
        if (past > getHealth()) {
            setHealth(past);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 120, 1.5, 1.5, 1.5, 0.3);
            level.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 2.0f, 0.3f);
            for (ServerPlayer player : players(GAZE_REACH)) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.unbegotten.unhappens")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
    }

    private void lash(ServerLevel level) {
        List<ServerPlayer> near = players(LASH_REACH);
        if (near.isEmpty()) {
            return;
        }
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 2.0f, 0.5f);
        for (ServerPlayer player : near) {
            player.hurt(damageSources().mobAttack(this), 8.0f);
            Vec3 away = player.position().subtract(position()).multiply(1, 0, 1).normalize();
            player.knockback(1.6, -away.x, -away.z);
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.4, 0));
            player.hurtMarked = true;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY(0.5), player.getZ(), 3, 0.5, 0.3, 0.5, 0.0);
        }
    }

    private void gazeTick(ServerLevel level, @Nullable Player nearest) {
        if (gazeCharge > 0) {
            gazeCharge--;
            if (gazeTarget == null || !gazeTarget.isAlive() || !hasLineOfSight(gazeTarget)) {
                gazeCharge = 0;
                return;
            }
            Vec3 from = getEyePosition();
            Vec3 to = gazeTarget.getEyePosition();
            if (gazeCharge % 3 == 0) {
                for (int i = 0; i <= 12; i++) {
                    Vec3 at = from.lerp(to, i / 12.0);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            if (gazeCharge == 0) {
                gazeTarget.hurt(damageSources().indirectMagic(this, this), 10.0f);
                if (gazeTarget instanceof ServerPlayer player) {
                    TemporalExposure.expose(player, 15);
                }
                level.sendParticles(ParticleTypes.SONIC_BOOM, to.x, to.y, to.z, 1, 0, 0, 0, 0);
                level.playSound(null, gazeTarget.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.6f);
            }
            return;
        }
        if (--gaze <= 0 && nearest != null && hasLineOfSight(nearest)) {
            gaze = GAZE_EVERY;
            gazeTarget = nearest;
            gazeCharge = GAZE_CHARGE;
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.5f);
        }
    }

    private void sendShreds(ServerLevel level) {
        if (level.getEntitiesOfClass(Vex.class, getBoundingBox().inflate(32)).size() >= MAX_SHREDS) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            Vex vex = EntityType.VEX.create(level);
            if (vex != null) {
                vex.moveTo(getX() + random.nextGaussian() * 2, getY(0.5), getZ() + random.nextGaussian() * 2, 0, 0);
                vex.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                vex.setLimitedLife(20 * 30);
                level.addFreshEntity(vex);
            }
        }
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    private void drag(ServerLevel level) {
        for (ServerPlayer player : players(28)) {
            Vec3 towards = position().subtract(player.position()).normalize().scale(0.9);
            player.setDeltaMovement(player.getDeltaMovement().add(towards.x, Math.max(0.2, towards.y), towards.z));
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        }
        level.playSound(null, blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 2.0f, 0.4f);
    }

    private List<ServerPlayer> players(double reach) {
        return level().getEntitiesOfClass(ServerPlayer.class, new AABB(blockPosition()).inflate(reach),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceTo(this) <= reach);
    }

    @Nullable
    private Player nearestPlayer(double reach) {
        return players(reach).stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElse(null);
    }

    // ---------------------------------------------------------------- what it is immune to, and its end

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.FELL_OUT_OF_WORLD) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            spawnAtLocation(new ItemStack(ModItems.HEART_OF_THE_UNBEGOTTEN.get()));
            ExperienceOrb.award(level, position(), 500);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 400, 3, 3, 3, 0.5);
            level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 3.0f, 0.5f);
            for (ServerPlayer player : players(128)) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.unbegotten.slain")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }
}
