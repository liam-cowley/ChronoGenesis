package com.chronogenesis.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.player.TemporalExposure;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.timetravel.ElderArena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;

/**
 * A limb of the Unbegotten ({@link Unbegotten}), rising out of the void at the edge of its arena, fighting on its own
 * and killed on its own (each has its own health; the body regrows them for a while).
 * <ul>
 *   <li><b>Tentacles</b> (four, round the disc between the causeways). They <b>slam</b> across a causeway to keep a
 *       player from the nail they are heading for: marked on the stone, then the tentacle lies across it, a wall of
 *       its flesh (Unbegotten Flesh) for 15 s, crushing whoever stood there. Lying there, it takes double harm; hacking
 *       at its flesh wounds it. With no nail to guard, it slams where a player stands. Anyone close to its root on the
 *       disc is <b>swept</b> off towards the edge. Every living tentacle shields the body.</li>
 *   <li><b>Eyes</b> (two, on stalks by the south outcrop). They <b>gaze</b>: a beam charged where all can see, that
 *       wounds and floods the body with Temporal Exposure, unless the target breaks its sight (behind a pillar). Once
 *       the maw opens they <b>stare</b>: a disc marked under someone.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public class UnbegottenLimb extends Monster {
    public static final int SLOTS = 6;
    public static final int TENTACLES = 4;
    public static final float TENTACLE_HEALTH = 160.0f;
    public static final float EYE_HEALTH = 110.0f;

    public static final int IDLE = 0;
    /** A slam (or a gaze) is marked and coming. */
    public static final int WINDING = 1;
    /** A tentacle lies across the arena. */
    public static final int SLAMMED = 2;
    /** A tentacle sweeps round its root. */
    public static final int SWEEPING = 3;

    private static final EntityDataAccessor<Boolean> EYE = SynchedEntityData.defineId(UnbegottenLimb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(UnbegottenLimb.class, EntityDataSerializers.INT);
    /** Where a slam lands, or what an eye gazes at. */
    private static final EntityDataAccessor<BlockPos> AIM = SynchedEntityData.defineId(UnbegottenLimb.class, EntityDataSerializers.BLOCK_POS);

    private static final int SLAM_WIND = 40;
    private static final int SLAM_LIE = 300;
    private static final int SWEEP_WIND = 24;
    private static final int GAZE_WIND = 40;
    private static final DustParticleOptions MARK = new DustParticleOptions(new Vector3f(0.65f, 1.0f, 0.5f), 1.8f);

    @Nullable
    private UUID body;
    private int slot;
    @Nullable
    private BlockPos home;
    private int actionTicks;
    private int cooldown = 80;
    private int sweepCooldown;
    private final List<BlockPos> wall = new ArrayList<>();
    @Nullable
    private Player gazeTarget;
    private final List<UnbegottenBlast> blasts = new ArrayList<>();
    /** Client: ticks since the action changed, for drawing. */
    public int clientActionTicks;

    public UnbegottenLimb(EntityType<? extends UnbegottenLimb> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        noPhysics = true;
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, TENTACLE_HEALTH)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EYE, false);
        builder.define(ACTION, IDLE);
        builder.define(AIM, BlockPos.ZERO);
    }

    /** It is the body's limb in this place. Slots past the tentacles are eyes. */
    void bind(Unbegotten body, int slot, BlockPos home) {
        this.body = body.getUUID();
        this.slot = slot;
        this.home = home;
        entityData.set(EYE, slot >= TENTACLES);
        if (slot >= TENTACLES) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(EYE_HEALTH);
            setHealth(EYE_HEALTH);
        }
        // Each limb keeps its own rhythm.
        cooldown = 60 + slot * 35;
    }

    public boolean tentacle() {
        return !entityData.get(EYE);
    }

    public boolean eye() {
        return entityData.get(EYE);
    }

    public int action() {
        return entityData.get(ACTION);
    }

    public BlockPos aim() {
        return entityData.get(AIM);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(ACTION)) {
            clientActionTicks = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientActionTicks++;
        }
    }

    @Nullable
    private Unbegotten body() {
        return body != null && level() instanceof ServerLevel level && level.getEntity(body) instanceof Unbegotten owner && owner.isAlive()
                ? owner : null;
    }

    /** Its arena still fights, and with its body. */
    private boolean fighting(ServerLevel level) {
        if (home == null || body == null) {
            return false;
        }
        ElderArena.Arena arena = ElderArena.arenas(level).at(home);
        return arena != null && arena.state == ElderArena.State.FIGHT && body.equals(arena.boss);
    }

    private void setAction(int action) {
        entityData.set(ACTION, action);
        actionTicks = 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        if (tickCount % 20 == 0 && !fighting(level)) {
            retract(level);
            discard();
            return;
        }
        // Its body may not be loaded yet (another chunk); it waits.
        Unbegotten owner = body();
        if (home != null) {
            Vec3 spot = ElderArena.limbSpot(home, slot);
            setDeltaMovement(spot.subtract(position()).scale(0.2));
        }
        UnbegottenBlast.tick(blasts, level, this);
        if (owner == null || home == null) {
            return;
        }
        List<ServerPlayer> players = ElderArena.players(level, home);
        int phase = owner.phase();
        actionTicks++;
        if (eye()) {
            eyeTick(level, players, phase);
        } else {
            tentacleTick(level, players, phase);
        }
    }

    // ---------------------------------------------------------------- tentacles

    private void tentacleTick(ServerLevel level, List<ServerPlayer> players, int phase) {
        switch (action()) {
            case WINDING -> {
                if (actionTicks % 4 == 0) {
                    markWall(level);
                }
                if (actionTicks >= SLAM_WIND) {
                    slam(level, players);
                }
            }
            case SLAMMED -> {
                if (actionTicks >= (wall.isEmpty() ? 20 : SLAM_LIE)) {
                    retract(level);
                    setAction(IDLE);
                    cooldown = phase == Unbegotten.PHASE_MAW ? 80 : 140;
                }
            }
            case SWEEPING -> {
                if (actionTicks >= SWEEP_WIND) {
                    sweep(level, players);
                    setAction(IDLE);
                    sweepCooldown = 100;
                }
            }
            default -> {
                sweepCooldown--;
                ServerPlayer close = players.stream().filter(p -> !p.isCreative() && horizontal(p.position(), position()) < 7.5).findFirst().orElse(null);
                if (close != null && sweepCooldown <= 0) {
                    setAction(SWEEPING);
                    entityData.set(AIM, close.blockPosition());
                    level.playSound(null, blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 2.0f, 0.3f);
                } else if (--cooldown <= 0 && !players.isEmpty()) {
                    windSlam(level, players);
                }
            }
        }
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        return Math.sqrt(Mth.square(a.x - b.x) + Mth.square(a.z - b.z));
    }

    /**
     * Picks where to slam: across the causeway to the undriven nail nearest a player, if that causeway is one of the two
     * beside this tentacle; else where a player stands.
     */
    private void windSlam(ServerLevel level, List<ServerPlayer> players) {
        ServerPlayer target = players.get(random.nextInt(players.size()));
        Direction[] guarded = {ElderArena.SIDES[slot % 4], ElderArena.SIDES[(slot + 1) % 4]};
        Direction across = null;
        double best = Double.MAX_VALUE;
        for (Direction side : guarded) {
            BlockPos nail = home.relative(side, ElderArena.NAIL_RING).above(ElderArena.PILLAR + 1);
            BlockPos crossing = home.relative(side, (ElderArena.RADIUS + ElderArena.NAIL_RING - ElderArena.OUTCROP) / 2);
            boolean undriven = level.getBlockState(nail).is(ModBlocks.UNMADE_NAIL.get())
                    && !level.getBlockState(nail).getValue(com.chronogenesis.timetravel.UnmadeNailBlock.DRIVEN);
            boolean taken = level.getBlockState(crossing.above()).is(ModBlocks.UNBEGOTTEN_FLESH.get());
            double d = target.position().distanceToSqr(Vec3.atCenterOf(nail));
            if (undriven && !taken && d < best) {
                best = d;
                across = side;
            }
        }
        wall.clear();
        if (across != null) {
            BlockPos crossing = home.relative(across, (ElderArena.RADIUS + ElderArena.NAIL_RING - ElderArena.OUTCROP) / 2);
            entityData.set(AIM, crossing.above());
            for (int w = -2; w <= 2; w++) {
                for (int y = 1; y <= 2; y++) {
                    wall.add(crossing.relative(across.getClockWise(), w).above(y));
                }
            }
        } else {
            entityData.set(AIM, BlockPos.containing(target.getX(), home.getY() + 1, target.getZ()));
            blasts.add(new UnbegottenBlast(new Vec3(target.getX(), home.getY() + 1, target.getZ()), 0, 3.0, SLAM_WIND, 12.0f, 0));
        }
        setAction(WINDING);
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.4f);
    }

    private void markWall(ServerLevel level) {
        for (BlockPos pos : wall) {
            if (pos.getY() == aim().getY()) {
                level.sendParticles(MARK, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 3, 0.4, 0.05, 0.4, 0);
            }
        }
    }

    private void slam(ServerLevel level, List<ServerPlayer> players) {
        setAction(SLAMMED);
        Vec3 at = Vec3.atBottomCenterOf(aim());
        level.playSound(null, aim(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5f, 0.4f);
        level.playSound(null, aim(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 3.0f, 0.3f);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 4, 1.5, 0.3, 1.5, 0);
        for (BlockPos pos : wall) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, ModBlocks.UNBEGOTTEN_FLESH.get().defaultBlockState(), 3);
            }
        }
        if (!wall.isEmpty()) {
            for (ServerPlayer player : players) {
                if (!player.isCreative() && player.position().distanceTo(at) < 3.5) {
                    player.hurt(damageSources().mobAttack(this), 14.0f);
                    Vec3 away = player.position().subtract(at).multiply(1, 0, 1).normalize();
                    player.knockback(1.2, -away.x, -away.z);
                    player.hurtMarked = true;
                }
            }
        }
    }

    /** It lifts off the arena again: its wall of flesh goes with it. */
    private void retract(ServerLevel level) {
        for (BlockPos pos : wall) {
            if (level.getBlockState(pos).is(ModBlocks.UNBEGOTTEN_FLESH.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
        wall.clear();
    }

    private void sweep(ServerLevel level, List<ServerPlayer> players) {
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3.0f, 0.3f);
        for (ServerPlayer player : players) {
            if (!player.isCreative() && horizontal(player.position(), position()) < 9.0) {
                player.hurt(damageSources().mobAttack(this), 10.0f);
                Vec3 out = player.position().subtract(Vec3.atCenterOf(home)).multiply(1, 0, 1).normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(out.x * 1.2, 0.6, out.z * 1.2));
                player.hurtMarked = true;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY(0.5), player.getZ(), 2, 0.4, 0.2, 0.4, 0);
            }
        }
    }

    // ---------------------------------------------------------------- eyes

    private void eyeTick(ServerLevel level, List<ServerPlayer> players, int phase) {
        Player watched = gazeTarget != null && gazeTarget.isAlive() ? gazeTarget
                : players.stream().filter(this::hasLineOfSight).findFirst().orElse(null);
        if (watched != null) {
            getLookControl().setLookAt(watched, 30.0f, 30.0f);
            entityData.set(AIM, watched.blockPosition());
        }
        if (action() == WINDING) {
            if (gazeTarget == null || !gazeTarget.isAlive()) {
                setAction(IDLE);
                return;
            }
            Vec3 from = getEyePosition();
            Vec3 to = gazeTarget.getEyePosition();
            if (actionTicks % 3 == 0) {
                for (int i = 0; i <= 16; i++) {
                    Vec3 at = from.lerp(to, i / 16.0);
                    level.sendParticles(ParticleTypes.DRAGON_BREATH, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
            if (actionTicks >= GAZE_WIND) {
                if (hasLineOfSight(gazeTarget)) {
                    gazeTarget.hurt(damageSources().indirectMagic(this, this), 12.0f);
                    if (gazeTarget instanceof ServerPlayer player) {
                        TemporalExposure.expose(player, 15);
                    }
                    level.sendParticles(ParticleTypes.SONIC_BOOM, to.x, to.y, to.z, 1, 0, 0, 0, 0);
                    level.playSound(null, gazeTarget.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.6f);
                }
                gazeTarget = null;
                setAction(IDLE);
                cooldown = phase == Unbegotten.PHASE_MAW ? 70 : 120 + random.nextInt(40);
            }
            return;
        }
        if (--cooldown > 0 || players.isEmpty()) {
            return;
        }
        if (phase == Unbegotten.PHASE_MAW && random.nextBoolean()) {
            ServerPlayer stared = players.get(random.nextInt(players.size()));
            blasts.add(new UnbegottenBlast(new Vec3(stared.getX(), home.getY() + 1, stared.getZ()), 0, 5.0, 50, 10.0f, 8));
            stared.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            cooldown = 90;
            return;
        }
        ServerPlayer seen = players.stream().filter(p -> !p.isCreative() && hasLineOfSight(p)).findAny().orElse(null);
        if (seen != null) {
            gazeTarget = seen;
            setAction(WINDING);
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.6f);
        } else {
            cooldown = 20;
        }
    }

    // ---------------------------------------------------------------- harm

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (action() == SLAMMED && !wall.isEmpty()) {
            // Lying across the arena, it is open.
            amount *= 2.0f;
        }
        return super.hurt(source, amount);
    }

    /** Hacking at a tentacle's flesh where it lies across the arena wounds it. */
    @SubscribeEvent
    static void onHitFlesh(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.UNBEGOTTEN_FLESH.get())) {
            return;
        }
        Player player = event.getEntity();
        for (UnbegottenLimb limb : event.getLevel().getEntitiesOfClass(UnbegottenLimb.class,
                new net.minecraft.world.phys.AABB(event.getPos()).inflate(ElderArena.BOUNDS), limb -> limb.wall.contains(event.getPos()))) {
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * Math.max(0.2f, player.getAttackStrengthScale(0.5f));
            limb.hurt(limb.damageSources().playerAttack(player), damage);
            player.resetAttackStrengthTicker();
            event.getLevel().playSound(null, event.getPos(), SoundEvents.SLIME_HURT, SoundSource.HOSTILE, 1.0f, 0.5f);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            retract(level);
            Unbegotten owner = body();
            if (owner != null) {
                owner.limbDied(slot);
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.6), getZ(), 120, 1.0, 4.0, 1.0, 0.3);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level && reason.shouldDestroy()) {
            retract(level);
        }
        super.remove(reason);
    }

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
    protected SoundEvent getAmbientSound() {
        return eye() ? null : SoundEvents.SLIME_SQUISH;
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
        return eye() ? 1.2f : 0.5f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (body != null) {
            tag.putUUID("Body", body);
        }
        tag.putInt("Slot", slot);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        tag.putBoolean("Eye", eye());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        body = tag.hasUUID("Body") ? tag.getUUID("Body") : null;
        slot = tag.getInt("Slot");
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        entityData.set(EYE, tag.getBoolean("Eye"));
    }
}
