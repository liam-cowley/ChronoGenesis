package com.chronogenesis.organism;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.Config;
import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.biocomputer.DissectionTableBlockEntity;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.essence.EssenceMoteOptions;
import com.chronogenesis.essence.MobEssence;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.time.TemporalStress;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Behaviour of all graft types. Finds its organism through the flesh it grows on. */
public class GraftBlockEntity extends BlockEntity {
    private static final int LEECH_INTERVAL = 40;
    private static final int LEECH_RANGE = 4;
    /** Motes drawn out of each creature per drain: the visible stream. */
    private static final int LEECH_MOTES = 10;
    /** Ticks the tendril stays swollen once the stream reaches it. */
    private static final int GULP_TICKS = 8;
    private static final int TAP_INTERVAL = 20;
    private static final int TAP_RATE = 4;
    /** Hearts beyond this many add nothing: the veins cannot carry more. */
    public static final int MAX_HEARTS = 3;

    // Eye: watches for creatures and reports how full the organism is.
    private static final int EYE_INTERVAL = 10;
    public static final int EYE_RANGE = 4;

    // Temporal Field (core-systems.md section 6): drained creatures slowly regenerate.
    private static final int FIELD_INTERVAL = 100;
    public static final int FIELD_RADIUS = 6;

    // Twin Clocks (core-systems.md section 4, Law of the Heavy Heart).
    private static final int CLOCK_INTERVAL = 40;
    /** How far around a Clock Density flesh counts as the mass weighing on it. */
    public static final int CLOCK_MASS_RADIUS = 2;
    /** Missed beats (mass difference, summed every interval) that condense into one dram of Time. */
    public static final int BEATS_PER_TIME_DRAM = 600;

    // Racetrack (core-systems.md section 4, Law of the Road).
    private static final int GATE_INTERVAL = 40;
    /** A pulse still travelling after this many ticks stalls and clogs its vein. */
    public static final int STALL_TICKS = 600;
    /** Each 40 ticks a pulse spends on the road condenses into one dram of Time. */
    public static final int TICKS_PER_TIME_DRAM = 40;
    public static final int MAX_PACE = 5;

    @Nullable
    private BlockPos cachedCore;
    private int tick;
    /** Starting Gate setting: ticks per vein block = pace * 4. Higher pace = slower pulse = more Time. */
    private int pace = 2;
    @Nullable
    private Pulse pulse;
    /** Eye: whether it sees a creature, and the comparator level it last reported. Not saved. */
    private boolean seesCreature;
    private int reportedFill;
    /** Free Clock: missed beats gathered so far towards the next dram of Time. */
    private int missedBeats;
    /** Leech Tendril: tick at which the stream arrives (and it gulps), or -1. Not saved. */
    private int gulpAt = -1;

    /** A Speed pulse running down a vein track. Not saved: an unloaded track loses its pulse. */
    private static final class Pulse {
        final List<BlockPos> path;
        final BlockPos finish;
        int index;
        int wait;
        int travelled;

        Pulse(List<BlockPos> path, BlockPos finish) {
            this.path = path;
            this.finish = finish;
        }
    }

    public GraftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAFT.get(), pos, state);
    }

    public GraftType type() {
        return ((GraftBlock) getBlockState().getBlock()).type();
    }

    public int pace() {
        return pace;
    }

    public boolean seesCreature() {
        return seesCreature;
    }

    public int reportedFill() {
        return reportedFill;
    }

    /** The organism this graft belongs to, or null if it is cut off or dead. */
    @Nullable
    public OrganismCoreBlockEntity core() {
        if (level == null) {
            return null;
        }
        if (cachedCore != null && level.getBlockEntity(cachedCore) instanceof OrganismCoreBlockEntity core
                && OrganismScan.isTissue(level.getBlockState(GraftBlock.supportPos(getBlockState(), worldPosition)), core.aspect())) {
            return core.isNecrotic() ? null : core;
        }
        OrganismCoreBlockEntity found = OrganismScan.findCore(level, GraftBlock.supportPos(getBlockState(), worldPosition),
                Config.MAX_ORGANISM_SIZE.getAsInt());
        cachedCore = found == null ? null : found.getBlockPos();
        return found == null || found.isNecrotic() ? null : found;
    }

    private BlockPos front() {
        return worldPosition.relative(getBlockState().getValue(GraftBlock.FACING));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GraftBlockEntity graft) {
        ServerLevel serverLevel = (ServerLevel) level;
        graft.tick++;
        switch (graft.type()) {
            case LEECH_TENDRIL -> {
                if (graft.tick % LEECH_INTERVAL == 0) {
                    graft.leech(serverLevel);
                }
                graft.gulpTick(serverLevel, state);
            }
            case TAP -> {
                if (graft.tick % TAP_INTERVAL == 0) {
                    graft.tap(serverLevel);
                }
            }
            case STARTING_GATE -> graft.gateTick(serverLevel);
            case EYE -> {
                if (graft.tick % EYE_INTERVAL == 0) {
                    graft.watch(serverLevel);
                }
            }
            case CLOCK -> {
                if (graft.tick % CLOCK_INTERVAL == 0) {
                    graft.clockTick(serverLevel);
                }
            }
            case TEMPORAL_FIELD -> {
                if (graft.tick % FIELD_INTERVAL == 0) {
                    graft.rewind(serverLevel);
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- Leech Tendril

    /** Drain 1 dram of the organism's aspect from every mob in range. Time needs Stasis Bladders. */
    private void leech(ServerLevel level) {
        OrganismCoreBlockEntity core = core();
        if (core == null || core.room() <= 0) {
            return;
        }
        Aspect aspect = core.aspect();
        if (aspect == Aspect.TIME && core.bladders() == 0) {
            return;
        }
        AABB area = new AABB(worldPosition).inflate(LEECH_RANGE);
        Vec3 mouth = mouth();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, MobEssence::canBeDrained)) {
            if (core.room() <= 0) {
                break;
            }
            int taken = MobEssence.drain(entity, aspect, 1);
            if (taken > 0) {
                core.feed(taken);
                if (aspect == Aspect.TIME) {
                    TemporalStress.add(level, entity.blockPosition(), taken * TemporalStress.PER_DRAM_EXTRACTED);
                }
                // The essence seeps out of the body and streams into the tendril's mouth.
                level.sendParticles(EssenceMoteOptions.of(aspect, mouth), entity.getX(), entity.getY(0.5), entity.getZ(),
                        LEECH_MOTES * taken, entity.getBbWidth() * 0.3, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.3, 0);
                level.playSound(null, entity.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 0.7f, 0.7f);
                // Gulp when the slowest stream arrives, if several creatures are drained at once.
                gulpAt = Math.max(gulpAt, tick + arrivalTicks(from(entity).distanceTo(mouth)));
            }
        }
    }

    /**
     * Ticks a stream of essence motes takes to fly from a creature to the tendril's mouth: they drift out for
     * a moment, then accelerate in (see the client's EssenceMoteParticle). Measured: 14 ticks plus 2 per block.
     */
    public static int arrivalTicks(double distance) {
        return 14 + (int) Math.round(distance * 2);
    }

    private static Vec3 from(LivingEntity entity) {
        return new Vec3(entity.getX(), entity.getY(0.5), entity.getZ());
    }

    /** The tendril's mouth: just out from the block, the way it faces. */
    private Vec3 mouth() {
        Direction facing = getBlockState().getValue(GraftBlock.FACING);
        return Vec3.atCenterOf(worldPosition).add(facing.getStepX() * 0.3, facing.getStepY() * 0.3, facing.getStepZ() * 0.3);
    }

    /** When the stream arrives the tendril swells and swallows, then settles back. */
    private void gulpTick(ServerLevel level, BlockState state) {
        if (gulpAt < 0) {
            if (state.getValue(GraftBlock.ACTIVE)) {
                // Unloaded mid-gulp: settle back.
                level.setBlock(worldPosition, state.setValue(GraftBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
            }
            return;
        }
        if (tick == gulpAt) {
            level.setBlock(worldPosition, state.setValue(GraftBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
            level.playSound(null, worldPosition, SoundEvents.GENERIC_DRINK, SoundSource.BLOCKS, 0.6f, 0.5f);
            Vec3 mouth = mouth();
            level.sendParticles(ParticleTypes.ITEM_SLIME, mouth.x, mouth.y, mouth.z, 4, 0.1, 0.1, 0.1, 0.02);
        } else if (tick >= gulpAt + GULP_TICKS) {
            level.setBlock(worldPosition, state.setValue(GraftBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
            gulpAt = -1;
        }
    }

    // ---------------------------------------------------------------- Tap

    /** Pour into a jug in front, or push through the veins in front to the nearest Maw. */
    private void tap(ServerLevel level) {
        OrganismCoreBlockEntity core = core();
        if (core == null || core.stored() <= 0) {
            return;
        }
        BlockPos front = front();
        int rate = tapRate(core);
        if (level.getBlockEntity(front) instanceof EssenceJugBlockEntity jug) {
            int moved = core.extract(Math.min(rate, jug.space(core.aspect())));
            if (moved > 0) {
                jug.insert(core.aspect(), moved);
            }
            return;
        }
        if (level.getBlockEntity(front) instanceof DissectionTableBlockEntity table) {
            // The reanimation altar soaks up Awareness and Time for the brain on it.
            int moved = core.extract(Math.min(rate, table.space(core.aspect())));
            if (moved > 0) {
                table.insert(core.aspect(), moved);
                level.sendParticles(ParticleTypes.DRIPPING_HONEY, front.getX() + 0.5, front.getY() + 1.0, front.getZ() + 0.5,
                        2, 0.15, 0.1, 0.15, 0);
            }
            return;
        }
        if (!level.getBlockState(front).is(ModBlocks.BRASS_CONDUIT.get())) {
            return;
        }
        VeinNetwork network = VeinNetwork.walk(level, front, worldPosition);
        // The veins grow along the conduit first, one block per beat, for a dram each.
        if (network.growthTarget() != null && core.extract(1) == 1) {
            growVein(level, network.growthTarget());
            return;
        }
        for (BlockPos mawPos : network.grafts()) {
            if (level.getBlockEntity(mawPos) instanceof GraftBlockEntity maw && maw.type() == GraftType.MAW) {
                OrganismCoreBlockEntity target = maw.core();
                if (target != null && target != core && target.aspect() == core.aspect() && target.room() > 0) {
                    int moved = core.extract(Math.min(rate, target.room()));
                    target.feed(moved);
                    level.sendParticles(ParticleTypes.FALLING_DRIPSTONE_LAVA, mawPos.getX() + 0.5, mawPos.getY() + 0.7,
                            mawPos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0);
                    return;
                }
            }
        }
    }

    /** Drams a Tap pushes per beat: 4, and 4 more for every Heart on the organism (up to {@value #MAX_HEARTS}). */
    public static int tapRate(OrganismCoreBlockEntity core) {
        return TAP_RATE * (1 + Math.min(core.hearts(), MAX_HEARTS));
    }

    private static void growVein(ServerLevel level, BlockPos conduit) {
        BlockState state = level.getBlockState(conduit);
        level.setBlockAndUpdate(conduit, state.setValue(BrassConduitBlock.VEIN, true));
        level.playSound(null, conduit, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.6f, 0.5f);
        level.sendParticles(ParticleTypes.ITEM_SLIME, conduit.getX() + 0.5, conduit.getY() + 0.5, conduit.getZ() + 0.5,
                6, 0.2, 0.2, 0.2, 0.02);
    }

    // ---------------------------------------------------------------- Racetrack

    /**
     * Starting Gate: every couple of seconds, if no pulse is running, spend Speed to send one down
     * the veins to a Finish graft. Time made = time on the road (length x pace), the Law of the Road.
     */
    private void gateTick(ServerLevel level) {
        if (pulse != null) {
            advancePulse(level);
            return;
        }
        if (tick % GATE_INTERVAL != 0) {
            return;
        }
        OrganismCoreBlockEntity core = core();
        if (core == null || core.aspect() != Aspect.SPEED) {
            return;
        }
        VeinNetwork network = VeinNetwork.walk(level, front(), worldPosition);
        for (BlockPos graftPos : network.grafts()) {
            if (level.getBlockEntity(graftPos) instanceof GraftBlockEntity finish && finish.type() == GraftType.FINISH) {
                List<BlockPos> path = network.pathTo(level, graftPos);
                int cost = launchCost(path.size());
                if (path.isEmpty() || core.stored() < cost) {
                    return;
                }
                core.extract(cost);
                pulse = new Pulse(path, graftPos);
                level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6f, 1.6f);
                return;
            }
        }
    }

    /** Speed spent per pulse: 1, plus 1 for every 8 blocks of track (veins feed on what passes). */
    public static int launchCost(int trackLength) {
        return 1 + trackLength / 8;
    }

    private void advancePulse(ServerLevel level) {
        Pulse p = pulse;
        BlockPos at = p.path.get(p.index);
        if (!BrassConduitBlock.isOpenVein(level.getBlockState(at))) {
            pulse = null; // the track was cut
            return;
        }
        p.travelled++;
        p.wait++;
        if (p.travelled % 2 == 0) {
            level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(Aspect.SPEED.color()).toVector3f(), 1.2f),
                    at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0);
        }
        if (p.travelled > STALL_TICKS) {
            // Too slow: the pulse dies in the vein and rots there.
            level.setBlockAndUpdate(at, level.getBlockState(at).setValue(BrassConduitBlock.CLOGGED, true));
            level.playSound(null, at, SoundEvents.SLIME_DEATH_SMALL, SoundSource.BLOCKS, 1.0f, 0.5f);
            pulse = null;
            return;
        }
        int delay = pace * 4 * (VeinNetwork.isHeavy(level, at) ? 2 : 1);
        if (p.wait < delay) {
            return;
        }
        p.wait = 0;
        p.index++;
        if (p.index >= p.path.size()) {
            arrive(level, p);
            pulse = null;
        }
    }

    private static void arrive(ServerLevel level, Pulse p) {
        int time = Math.max(1, p.travelled / TICKS_PER_TIME_DRAM);
        BlockPos finishPos = p.finish;
        level.playSound(null, finishPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.7f);
        if (level.getBlockEntity(finishPos) instanceof GraftBlockEntity finish) {
            OrganismCoreBlockEntity core = finish.core();
            if (core != null && core.aspect() == Aspect.TIME) {
                int fed = core.feed(time);
                TemporalStress.add(level, finishPos, fed);
                if (fed > 0) {
                    Clues.witness(level, finishPos, Clues.LONG_ROAD);
                }
                level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(Aspect.TIME.color()).toVector3f(), 1.5f),
                        finishPos.getX() + 0.5, finishPos.getY() + 0.8, finishPos.getZ() + 0.5, 8 + fed, 0.2, 0.2, 0.2, 0);
                return;
            }
        }
        // No Time organism to catch it: the time scatters.
        level.sendParticles(ParticleTypes.WHITE_ASH, finishPos.getX() + 0.5, finishPos.getY() + 0.8, finishPos.getZ() + 0.5,
                12, 0.3, 0.3, 0.3, 0.02);
    }

    // ---------------------------------------------------------------- Eye

    /** Redstone while a creature is within reach; comparator output follows how full the organism is. */
    private void watch(ServerLevel level) {
        OrganismCoreBlockEntity core = core();
        boolean sees = core != null && !level.getEntitiesOfClass(LivingEntity.class,
                new AABB(worldPosition).inflate(EYE_RANGE), entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
        int fill = core == null ? 0 : core.comparatorSignal();
        if (sees != seesCreature) {
            seesCreature = sees;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        if (fill != reportedFill) {
            reportedFill = fill;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // ---------------------------------------------------------------- Temporal Field

    /**
     * Rewinds one drained creature in range: gives back a dram of the essence it has lost most of, paid for
     * with a dram of the organism's Time. Breed-and-drain farms become sustainable.
     */
    private void rewind(ServerLevel level) {
        OrganismCoreBlockEntity core = core();
        if (core == null || core.aspect() != Aspect.TIME || core.stored() <= 0) {
            return;
        }
        AABB area = new AABB(worldPosition).inflate(FIELD_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, MobEssence::canBeDrained)) {
            Aspect lost = mostDrained(entity);
            if (lost == null) {
                continue;
            }
            if (MobEssence.restore(entity, lost, 1) > 0) {
                core.extract(1);
                TemporalStress.add(level, worldPosition, 1);
                // The essence flows back the other way: out of the field and into the creature.
                Vec3 from = Vec3.atCenterOf(worldPosition);
                level.sendParticles(EssenceMoteOptions.of(lost, entity.position().add(0, entity.getBbHeight() / 2, 0)),
                        from.x, from.y + 0.3, from.z, 8, 0.15, 0.15, 0.15, 0);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY(0.5), entity.getZ(),
                        10, entity.getBbWidth() / 2, entity.getBbHeight() / 3, entity.getBbWidth() / 2, 0.02);
                level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6f, 1.4f);
                return; // one creature per beat
            }
        }
    }

    /** The aspect the creature has lost the largest share of, or null if it is whole. */
    @Nullable
    private static Aspect mostDrained(LivingEntity entity) {
        Aspect most = null;
        float worst = 0;
        for (Aspect aspect : Aspect.values()) {
            // fraction() is what is left (1.0 when whole, or when it never had any).
            float lost = 1.0f - MobEssence.fraction(entity, aspect);
            if (lost > worst) {
                worst = lost;
                most = aspect;
            }
        }
        return most;
    }

    // ---------------------------------------------------------------- Twin Clocks

    /**
     * Runs on the free Clock, the one grafted on a Time organism. Every interval it compares itself with
     * each Clock at the other end of its veins that is buried in a Density organism: the more Density flesh
     * weighs on the buried Clock than on this one, the more beats it has missed. Missed beats are Time.
     */
    private void clockTick(ServerLevel level) {
        OrganismCoreBlockEntity core = core();
        if (core == null || core.aspect() != Aspect.TIME
                || !level.getBlockState(front()).is(ModBlocks.BRASS_CONDUIT.get())) {
            return;
        }
        int ownMass = massAround(level, worldPosition);
        int missed = 0;
        for (BlockPos other : VeinNetwork.walk(level, front(), worldPosition).grafts()) {
            if (level.getBlockEntity(other) instanceof GraftBlockEntity buried && buried.type() == GraftType.CLOCK) {
                OrganismCoreBlockEntity heavy = buried.core();
                if (heavy != null && heavy.aspect() == Aspect.DENSITY) {
                    missed += Math.max(0, massAround(level, other) - ownMass);
                }
            }
        }
        if (missed <= 0) {
            return;
        }
        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.3f, 0.6f);
        missedBeats += missed;
        int drams = missedBeats / BEATS_PER_TIME_DRAM;
        if (drams > 0) {
            int fed = core.feed(drams);
            missedBeats -= drams * BEATS_PER_TIME_DRAM;
            if (fed > 0) {
                TemporalStress.add(level, worldPosition, fed);
                Clues.witness(level, worldPosition, Clues.HEAVY_HEART);
                level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 0.5f);
                level.sendParticles(new DustParticleOptions(Vec3.fromRGB24(Aspect.TIME.color()).toVector3f(), 1.2f),
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.7, worldPosition.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0);
            }
        }
        setChanged();
    }

    /** Density flesh within {@value #CLOCK_MASS_RADIUS} blocks of a Clock: the mass slowing it down. */
    public static int massAround(Level level, BlockPos clock) {
        int mass = 0;
        for (BlockPos pos : BlockPos.betweenClosed(clock.offset(-CLOCK_MASS_RADIUS, -CLOCK_MASS_RADIUS, -CLOCK_MASS_RADIUS),
                clock.offset(CLOCK_MASS_RADIUS, CLOCK_MASS_RADIUS, CLOCK_MASS_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if ((state.is(ModBlocks.CHRONOFLESH.get()) || state.is(ModBlocks.ORGANISM_CORE.get()))
                    && state.getValue(FleshBlock.ASPECT) == Aspect.DENSITY) {
                mass++;
            }
        }
        return mass;
    }

    // ---------------------------------------------------------------- interaction

    public ItemInteractionResult useItemOn(ItemStack stack, Player player, InteractionHand hand) {
        if (level == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (type() == GraftType.TAP && EssenceItems.isContainer(stack)) {
            if (level.isClientSide()) {
                return ItemInteractionResult.SUCCESS;
            }
            OrganismCoreBlockEntity core = core();
            if (core == null) {
                OrganismInteractions.tell(player, Component.translatable("message.chronogenesis.organism.dead"));
                return ItemInteractionResult.FAIL;
            }
            if (!core.aspect().fitsInGlass() && core.aspect() != Aspect.TIME) {
                OrganismInteractions.tell(player, Component.translatable("message.chronogenesis.tap.volatile",
                        core.aspect().displayName()));
                return ItemInteractionResult.FAIL;
            }
            int space = EssenceItems.space(stack, core.aspect());
            int moved = core.extract(space);
            if (moved > 0) {
                EssenceItems.fillHeld(player, hand, core.aspect(), moved);
                level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 0.8f);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        // Anything else: treat the graft as part of the organism (feeding).
        return OrganismInteractions.useItemOnTissue(stack, level, GraftBlock.supportPos(getBlockState(), worldPosition),
                player, hand);
    }

    public InteractionResult useWithoutItem(Player player) {
        if (level == null) {
            return InteractionResult.PASS;
        }
        switch (type()) {
            case ENHANCING_GLAND -> {
                if (!level.isClientSide()) {
                    OrganismCoreBlockEntity core = core();
                    if (core == null) {
                        OrganismInteractions.tell(player, Component.translatable("message.chronogenesis.organism.dead"));
                    } else {
                        Powers.infuseFromGland(player, core, worldPosition);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            case STARTING_GATE -> {
                if (!level.isClientSide()) {
                    pace = pace % MAX_PACE + 1;
                    setChanged();
                    OrganismInteractions.tell(player, Component.translatable("message.chronogenesis.gate.pace",
                            pace, pace * 4));
                    level.playSound(null, worldPosition, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 0.5f + pace * 0.15f);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            default -> {
                return OrganismInteractions.inspect(level, GraftBlock.supportPos(getBlockState(), worldPosition), player);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("pace", pace);
        tag.putInt("missed_beats", missedBeats);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pace = tag.contains("pace") ? tag.getInt("pace") : 2;
        missedBeats = tag.getInt("missed_beats");
    }
}
