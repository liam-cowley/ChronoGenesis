package com.chronogenesis.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.player.PlayerGraft;
import com.chronogenesis.registry.ModEntities;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.time.TimeZones;
import com.chronogenesis.timetravel.ElderArena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * The Unbegotten (core-systems.md section 5, the last era): the player's own Chronocortex, grown on the time of the
 * Unmade Nails into an Elder God, and free. A raid boss of separate parts round the edge of its arena
 * ({@link ElderArena}): this is its <b>body</b>, risen past the north rim, a brain-mass with one great eye and a maw;
 * its <b>limbs</b> ({@link UnbegottenLimb}: four tentacles round the disc, two eyes by the south outcrop) fight on
 * their own and can be killed on their own. It fights in three phases:
 * <ol>
 *   <li><b>Unborn</b> (until all four nails are driven again): its wounds <b>unhappen</b> (every 5 s it is as whole as
 *       5 s before), unless held in slowed or stopped time; it tears any such hold that lasts longer than 5 s. Its
 *       <b>Ripples of Unmaking</b> (rings, then the middle, marked on the floor) sweep the disc; its tentacles slam
 *       across the causeways to keep players from the nails.</li>
 *   <li><b>Time begins</b>: its wounds stay. It sheds <b>shreds</b> of itself (vexes); dead limbs regrow faster.</li>
 *   <li><b>The maw</b> (below 35%): it opens, <b>inhales</b> (dragging everyone towards the rim), and <b>stars</b>
 *       fall on the arena (marked discs, and one under every player). Dead limbs no longer regrow.</li>
 * </ol>
 * Every living tentacle shields the body from 15% of every wound. Slain, it leaves its heart and grafts for the body.
 */
public class Unbegotten extends Monster {
    public static final int PHASE_UNBORN = 1;
    public static final int PHASE_TIME = 2;
    public static final int PHASE_MAW = 3;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Unbegotten.class, EntityDataSerializers.INT);
    /** Ticks left of an inhale (synced: the maw gapes). */
    private static final EntityDataAccessor<Integer> INHALING = SynchedEntityData.defineId(Unbegotten.class, EntityDataSerializers.INT);

    private static final int REWIND_EVERY = 100;
    /** A hold on time near it lasts at most this long before it tears it. */
    private static final int HOLD_LIMIT = 100;
    private static final float TENTACLE_SHIELD = 0.15f;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(Component.translatable("entity.chronogenesis.unbegotten"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true).setCreateWorldFog(true);
    private final ServerBossEvent limbsEvent = new ServerBossEvent(Component.translatable("entity.chronogenesis.unbegotten.limbs"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6);
    /** Its health over the last {@value #REWIND_EVERY} ticks, sampled every 5. */
    private final Deque<Float> history = new ArrayDeque<>();
    private final List<UnbegottenBlast> blasts = new ArrayList<>();
    @Nullable
    private BlockPos home;
    @Nullable
    private String cortexName;
    private final UUID[] limbs = new UUID[UnbegottenLimb.SLOTS];
    /** Ticks until a dead limb grows back (0: alive or not regrowing). */
    private final int[] regrow = new int[UnbegottenLimb.SLOTS];
    /** Checks in a row a limb's place was loaded and it was not there. */
    private final int[] missing = new int[UnbegottenLimb.SLOTS];
    private int ripple = 160;
    private boolean rippleOuter;
    private int shreds = 300;
    private int inhale = 240;
    private int stars = 160;
    private int crush = 60;

    public Unbegotten(EntityType<? extends Unbegotten> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        noPhysics = true;
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_UNBORN);
        builder.define(INHALING, 0);
    }

    public void setHome(BlockPos home) {
        this.home = home;
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    /** It was the Chronocortex called this (or nothing, if it was never named): it keeps the name. */
    public void setCortexName(@Nullable String name) {
        cortexName = name;
        if (name != null) {
            setCustomName(Component.translatable("entity.chronogenesis.unbegotten.named", name));
        }
        bossEvent.setName(getDisplayName());
    }

    @Nullable
    public String cortexName() {
        return cortexName;
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    public boolean inhaling() {
        return entityData.get(INHALING) > 0;
    }

    /** Time has begun around it (all its arena's nails driven): its wounds stay. Without an arena, always. */
    public boolean mortal() {
        return home == null || ElderArena.driven(level(), home) >= 4;
    }

    /** Held in a slowed or stopped chunk: it cannot rewind there. */
    public boolean heldInTime() {
        return TimeZones.at(level(), blockPosition()) != null;
    }

    // ---------------------------------------------------------------- limbs

    /** A limb grows (back) into its place. */
    public void growLimb(ServerLevel level, int slot) {
        if (home == null) {
            return;
        }
        UnbegottenLimb limb = ModEntities.UNBEGOTTEN_LIMB.get().create(level);
        if (limb == null) {
            return;
        }
        Vec3 at = ElderArena.limbSpot(home, slot);
        limb.moveTo(at.x, at.y, at.z, 0, 0);
        limb.bind(this, slot, home);
        limb.finalizeSpawn(level, level.getCurrentDifficultyAt(home), MobSpawnType.EVENT, null);
        level.addFreshEntity(limb);
        limbs[slot] = limb.getUUID();
        regrow[slot] = 0;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 6, at.z, 80, 1.0, 4.0, 1.0, 0.2);
        level.playSound(null, BlockPos.containing(at), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    /** A limb died: it grows back in a while (not once the maw has opened). */
    void limbDied(int slot) {
        limbs[slot] = null;
        int phase = phase();
        regrow[slot] = phase == PHASE_UNBORN ? 900 : phase == PHASE_TIME ? 600 : 0;
    }

    public List<UnbegottenLimb> livingLimbs() {
        List<UnbegottenLimb> out = new ArrayList<>();
        if (level() instanceof ServerLevel level) {
            for (UUID id : limbs) {
                if (id != null && level.getEntity(id) instanceof UnbegottenLimb limb && limb.isAlive()) {
                    out.add(limb);
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- fighting

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        if (tickCount % 20 == 0 && !stillFighting(level)) {
            for (UnbegottenLimb limb : livingLimbs()) {
                limb.discard();
            }
            discard();
            return;
        }
        hold();
        List<ServerPlayer> players = home == null ? List.of() : ElderArena.players(level, home);
        Player nearest = players.stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this))).orElse(null);
        if (nearest != null) {
            getLookControl().setLookAt(nearest, 10.0f, 10.0f);
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        updateLimbBar();
        updatePhase(level, players);
        if (tickCount % 5 == 0) {
            history.addLast(getHealth());
            while (history.size() > REWIND_EVERY / 5) {
                history.removeFirst();
            }
        }
        if (tickCount % REWIND_EVERY == 0 && phase() == PHASE_UNBORN) {
            rewind(level, players);
        }
        if (tickCount % 10 == 0 && home != null && TimeZones.tear(level, Vec3.atCenterOf(home), ElderArena.BOUNDS + 24, HOLD_LIMIT,
                "message.chronogenesis.power.time.torn") > 0) {
            level.playSound(null, home, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0f, 0.4f);
        }
        if (tickCount % 20 == 0 && home != null) {
            // A limb lost some other way (a command, a broken save) counts as dead.
            for (int slot = 0; slot < limbs.length; slot++) {
                boolean gone = limbs[slot] != null && level.getEntity(limbs[slot]) == null
                        && level.isPositionEntityTicking(BlockPos.containing(ElderArena.limbSpot(home, slot)));
                missing[slot] = gone ? missing[slot] + 1 : 0;
                if (missing[slot] >= 10) {
                    missing[slot] = 0;
                    limbDied(slot);
                }
            }
        }
        for (int slot = 0; slot < limbs.length; slot++) {
            if (limbs[slot] == null && regrow[slot] > 0 && --regrow[slot] == 0 && phase() != PHASE_MAW) {
                growLimb(level, slot);
            }
        }
        UnbegottenBlast.tick(blasts, level, this);
        if (players.isEmpty() || home == null) {
            return;
        }
        int phase = phase();
        if (--ripple <= 0) {
            ripple = phase == PHASE_UNBORN ? 260 : phase == PHASE_TIME ? 200 : 160;
            ripple(level);
        }
        if (--crush <= 0) {
            crush = 50;
            crush(level, players);
        }
        if (phase >= PHASE_TIME && --shreds <= 0) {
            shreds = 400;
            sendShreds(level);
        }
        if (phase == PHASE_MAW) {
            if (--inhale <= 0) {
                inhale = 320;
                entityData.set(INHALING, 70);
                level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3.0f, 0.3f);
            }
            if (--stars <= 0) {
                stars = 220;
                starfall(level, players);
            }
        }
        int inhaling = entityData.get(INHALING);
        if (inhaling > 0) {
            entityData.set(INHALING, inhaling - 1);
            inhale(level, players);
        }
    }

    /** Its arena still says it fights, and that it is this one (a second body, from an old save, goes). */
    private boolean stillFighting(ServerLevel level) {
        if (home == null) {
            return true;
        }
        ElderArena.Arena arena = ElderArena.arenas(level).at(home);
        return arena != null && arena.state == ElderArena.State.FIGHT && getUUID().equals(arena.boss);
    }

    /** It hangs where it rose, swaying. */
    private void hold() {
        if (home == null) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 spot = ElderArena.bodySpot(home).add(Math.sin(tickCount * 0.02) * 1.5, Math.sin(tickCount * 0.05) * 0.8, 0);
        setDeltaMovement(spot.subtract(position()).scale(0.1));
    }

    private void updateLimbBar() {
        float health = 0;
        for (UnbegottenLimb limb : livingLimbs()) {
            health += limb.getHealth();
        }
        float whole = UnbegottenLimb.TENTACLES * UnbegottenLimb.TENTACLE_HEALTH
                + (UnbegottenLimb.SLOTS - UnbegottenLimb.TENTACLES) * UnbegottenLimb.EYE_HEALTH;
        limbsEvent.setProgress(Math.min(1.0f, health / whole));
    }

    private void updatePhase(ServerLevel level, List<ServerPlayer> players) {
        int phase = !mortal() ? PHASE_UNBORN : getHealth() < getMaxHealth() * 0.35f ? PHASE_MAW : PHASE_TIME;
        if (phase == phase()) {
            return;
        }
        entityData.set(PHASE, phase);
        String key = phase == PHASE_TIME ? "message.chronogenesis.unbegotten.phase.time"
                : phase == PHASE_MAW ? "message.chronogenesis.unbegotten.phase.maw" : "message.chronogenesis.unbegotten.phase.unborn";
        for (ServerPlayer player : players) {
            player.displayClientMessage(Component.translatable(key, getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        }
        level.playSound(null, blockPosition(), phase == PHASE_MAW ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 4.0f, 0.5f);
        if (phase == PHASE_MAW) {
            bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
    }

    private void rewind(ServerLevel level, List<ServerPlayer> players) {
        if (history.isEmpty() || mortal() || heldInTime()) {
            return;
        }
        float past = history.peekFirst();
        if (past > getHealth()) {
            setHealth(past);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 160, 3, 3, 3, 0.3);
            level.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.HOSTILE, 3.0f, 0.3f);
            for (ServerPlayer player : players) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.unbegotten.unhappens")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
    }

    /** A Ripple of Unmaking: the outer ring of the disc (and the outcrops' inner halves), then the middle, in turn. */
    private void ripple(ServerLevel level) {
        Vec3 middle = Vec3.atBottomCenterOf(home).add(0, 1, 0);
        rippleOuter = !rippleOuter;
        float damage = phase() == PHASE_UNBORN ? 9.0f : 12.0f;
        if (rippleOuter) {
            blasts.add(new UnbegottenBlast(middle, 6.5, ElderArena.NAIL_RING - 1.5, 60, damage, 10));
        } else {
            blasts.add(new UnbegottenBlast(middle, 0, 7.5, 60, damage, 10));
            for (int side = 0; side < 4; side++) {
                // The outcrops are not safe either.
                BlockPos outcrop = home.relative(ElderArena.SIDES[side], ElderArena.NAIL_RING);
                blasts.add(new UnbegottenBlast(Vec3.atBottomCenterOf(outcrop).add(0, 1, 0), 0, ElderArena.OUTCROP + 0.5, 60, damage, 10));
            }
        }
        level.playSound(null, home, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 4.0f, 0.3f);
    }

    /** Anyone close to the body (on the north outcrop) is crushed and thrown back. */
    private void crush(ServerLevel level, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (player.distanceTo(this) < 9.0 && !player.isCreative()) {
                player.hurt(damageSources().mobAttack(this), 10.0f);
                Vec3 away = player.position().subtract(position()).multiply(1, 0, 1).normalize();
                player.knockback(1.4, -away.x, -away.z);
                player.hurtMarked = true;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY(0.5), player.getZ(), 3, 0.5, 0.3, 0.5, 0.0);
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 2.0f, 0.5f);
            }
        }
    }

    private void sendShreds(ServerLevel level) {
        if (level.getEntitiesOfClass(Vex.class, new AABB(home).inflate(ElderArena.BOUNDS)).size() >= 6) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            Vex vex = EntityType.VEX.create(level);
            if (vex != null) {
                vex.moveTo(getX() + random.nextGaussian() * 3, getY(0.5), getZ() + 4 + random.nextGaussian() * 3, 0, 0);
                vex.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                vex.setLimitedLife(20 * 30);
                level.addFreshEntity(vex);
            }
        }
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 3.0f, 0.5f);
    }

    /** The maw drinks the air: everyone is dragged towards it (and the edge). */
    private void inhale(ServerLevel level, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (player.isCreative()) {
                continue;
            }
            Vec3 towards = position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(0.075);
            player.setDeltaMovement(player.getDeltaMovement().add(towards));
            player.hurtMarked = true;
        }
        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY(0.4), getZ() + 6, 12, 6, 2, 6, -0.4);
        }
    }

    /** Stars fall: discs marked at random over the arena, and one under every player. */
    private void starfall(ServerLevel level, List<ServerPlayer> players) {
        Vec3 middle = Vec3.atBottomCenterOf(home).add(0, 1, 0);
        for (int i = 0; i < 5; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * ElderArena.NAIL_RING;
            blasts.add(new UnbegottenBlast(middle.add(Math.cos(a) * r, 0, Math.sin(a) * r), 0, 3.5, 40, 10.0f, 6));
        }
        for (ServerPlayer player : players) {
            blasts.add(new UnbegottenBlast(new Vec3(player.getX(), middle.y, player.getZ()), 0, 3.0, 40, 10.0f, 6));
        }
        level.playSound(null, home, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 4.0f, 0.4f);
    }

    // ---------------------------------------------------------------- what it is immune to, and its end

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            int tentacles = (int) livingLimbs().stream().filter(UnbegottenLimb::tentacle).count();
            amount *= 1.0f - TENTACLE_SHIELD * tentacles;
        }
        return super.hurt(source, amount);
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
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (UnbegottenLimb limb : livingLimbs()) {
            limb.kill();
        }
        spawnAtLocation(new ItemStack(ModItems.HEART_OF_THE_UNBEGOTTEN.get()));
        // Two of the grafts it was growing, for a body that is not its own.
        List<PlayerGraft> grafts = new ArrayList<>(List.of(PlayerGraft.values()));
        Collections.shuffle(grafts, new java.util.Random(random.nextLong()));
        for (int i = 0; i < 2; i++) {
            spawnAtLocation(new ItemStack(grafts.get(i).item()));
        }
        List<ServerPlayer> players = home == null ? List.of() : ElderArena.players(level, home);
        for (ServerPlayer player : players) {
            ExperienceOrb.award(level, player.position(), 300);
            Clues.grant(player, Clues.UNBEGOTTEN_SLAIN);
            player.displayClientMessage(Component.translatable("message.chronogenesis.unbegotten.slain", getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 500, 4, 4, 4, 0.5);
        level.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 4.0f, 0.5f);
        if (home != null) {
            ElderArena.endFight(level, home);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
        limbsEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
        limbsEvent.removePlayer(player);
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
        return 0.3f;
    }

    @Override
    protected float getSoundVolume() {
        return 4.0f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        if (cortexName != null) {
            tag.putString("CortexName", cortexName);
        }
        ListTag list = new ListTag();
        for (int slot = 0; slot < limbs.length; slot++) {
            CompoundTag limb = new CompoundTag();
            limb.putInt("Slot", slot);
            if (limbs[slot] != null) {
                limb.putUUID("Id", limbs[slot]);
            }
            limb.putInt("Regrow", regrow[slot]);
            list.add(limb);
        }
        tag.put("Limbs", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
        cortexName = tag.contains("CortexName") ? tag.getString("CortexName") : null;
        for (Tag entry : tag.getList("Limbs", Tag.TAG_COMPOUND)) {
            CompoundTag limb = (CompoundTag) entry;
            int slot = limb.getInt("Slot");
            if (slot >= 0 && slot < limbs.length) {
                limbs[slot] = limb.hasUUID("Id") ? limb.getUUID("Id") : null;
                regrow[slot] = limb.getInt("Regrow");
            }
        }
        bossEvent.setName(getDisplayName());
    }
}
