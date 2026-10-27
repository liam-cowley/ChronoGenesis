package com.chronogenesis.organism;

import java.util.List;

import com.chronogenesis.Config;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives an organism (core-systems.md section 2):
 * <ol>
 *   <li>Fed essence is stored, up to a capacity that grows with the organism's size.</li>
 *   <li>While there is frame (Brass Armature) touching it and enough stored essence, it grows
 *       into the frame one block at a time: essence becomes flesh.</li>
 *   <li>It eats regularly (upkeep). If it cannot, it eats its own flesh, shrinking back to bare
 *       frame, and finally turns Necrotic.</li>
 * </ol>
 */
public class OrganismCoreBlockEntity extends BlockEntity {
    private static final int SCAN_INTERVAL = 40;
    private static final int GROWTH_INTERVAL = 10;
    private static final int BASE_CAPACITY = 16;
    private static final int CAPACITY_PER_BLOCK = 8;

    private int stored;
    private int size;
    private int upkeepTimer;
    private int missedMeals;
    private int tick;
    private List<BlockPos> frontier = List.of();
    private List<BlockPos> flesh = List.of();

    public OrganismCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORGANISM_CORE.get(), pos, state);
    }

    public Aspect aspect() {
        return getBlockState().getValue(FleshBlock.ASPECT);
    }

    public boolean isNecrotic() {
        return getBlockState().getValue(OrganismCoreBlock.PHASE) == CorePhase.NECROTIC;
    }

    public int stored() {
        return stored;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return BASE_CAPACITY + size * CAPACITY_PER_BLOCK;
    }

    public int room() {
        return Math.max(0, capacity() - stored);
    }

    /** Adds essence. Returns how much was accepted. */
    public int feed(int amount) {
        if (isNecrotic()) {
            return 0;
        }
        int accepted = Math.min(amount, room());
        if (accepted > 0) {
            stored += accepted;
            setChanged();
        }
        return accepted;
    }

    /** Takes essence out (Taps, Glands). Returns how much was taken. */
    public int extract(int amount) {
        int taken = Math.min(amount, stored);
        if (taken > 0) {
            stored -= taken;
            setChanged();
        }
        return taken;
    }

    public int comparatorSignal() {
        return capacity() == 0 ? 0 : Mth.floor(15.0f * stored / capacity());
    }

    public Component statusLine() {
        if (isNecrotic()) {
            return Component.translatable("message.chronogenesis.organism.necrotic");
        }
        String hunger = stored < capacity() / 4 ? "starving" : stored < capacity() * 3 / 4 ? "hungry" : "sated";
        return Component.translatable("message.chronogenesis.organism.status",
                aspect().displayName(), size + 1, stored, capacity(),
                Component.translatable("message.chronogenesis.organism.hunger." + hunger));
    }

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, OrganismCoreBlockEntity core) {
        if (core.isNecrotic()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        core.tick++;
        if (core.tick % SCAN_INTERVAL == 1) {
            core.rescan(serverLevel);
        }
        if (core.tick % GROWTH_INTERVAL == 0 && !core.tryGrow(serverLevel)) {
            core.mature(serverLevel);
        }
        if (++core.upkeepTimer >= Config.UPKEEP_INTERVAL.getAsInt()) {
            core.upkeepTimer = 0;
            core.eat(serverLevel);
        }
        if (core.tick % 100 == 0 && core.stored < core.capacity() / 4) {
            // Hungry organisms make it known.
            serverLevel.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 0.4f, 1.4f);
        }
    }

    private void rescan(ServerLevel level) {
        OrganismScan.Body body = OrganismScan.scan(level, worldPosition, aspect(), Config.MAX_ORGANISM_SIZE.getAsInt());
        flesh = body.flesh();
        frontier = body.frontier();
        if (size != flesh.size()) {
            size = flesh.size();
            stored = Math.min(stored, capacity());
            setChanged();
        }
        updatePhase(level);
    }

    /** Seed until it has grown flesh; then awake, or hungry below a quarter of its capacity. */
    private void updatePhase(ServerLevel level) {
        if (isNecrotic()) {
            return;
        }
        CorePhase phase = size == 0 ? CorePhase.SEED : stored < capacity() / 4 ? CorePhase.HUNGRY : CorePhase.AWAKE;
        if (getBlockState().getValue(OrganismCoreBlock.PHASE) != phase) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(OrganismCoreBlock.PHASE, phase));
        }
    }

    /**
     * Essence becomes growth: spend stored drams to turn the nearest frame block into young flesh
     * (growth stage 0: tendrils on the frame). Returns whether anything grew.
     */
    private boolean tryGrow(ServerLevel level) {
        int cost = Config.GROWTH_COST.getAsInt();
        if (stored < cost || frontier.isEmpty() || size >= Config.MAX_ORGANISM_SIZE.getAsInt()) {
            return false;
        }
        for (BlockPos target : frontier) {
            if (level.getBlockState(target).is(ModBlocks.BRASS_ARMATURE.get())) {
                level.setBlockAndUpdate(target, ModBlocks.CHRONOFLESH.get().defaultBlockState()
                        .setValue(FleshBlock.ASPECT, aspect()).setValue(FleshBlock.GROWTH, 0));
                level.playSound(null, target, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 0.6f);
                level.sendParticles(ParticleTypes.ITEM_SLIME, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                        12, 0.4, 0.4, 0.4, 0.05);
                stored -= cost;
                size++;
                rescan(level);
                setChanged();
                return true;
            }
        }
        rescan(level);
        return false;
    }

    /** Young flesh swells into its frame, one stage at a time, nearest first. Free, but only while fed. */
    private void mature(ServerLevel level) {
        if (stored <= 0) {
            return;
        }
        for (BlockPos pos : flesh) {
            BlockState state = level.getBlockState(pos);
            if (OrganismScan.isTissue(state, aspect()) && state.hasProperty(FleshBlock.GROWTH)
                    && state.getValue(FleshBlock.GROWTH) < FleshBlock.MATURE) {
                level.setBlockAndUpdate(pos, state.setValue(FleshBlock.GROWTH, state.getValue(FleshBlock.GROWTH) + 1));
                level.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.5f, 0.6f);
                return;
            }
        }
    }

    /** Upkeep: 1 dram per 8 blocks of flesh. If it cannot eat, it eats itself. */
    private void eat(ServerLevel level) {
        int meal = 1 + size / 8;
        if (stored >= meal) {
            stored -= meal;
            missedMeals = 0;
            setChanged();
            return;
        }
        stored = 0;
        missedMeals++;
        // A bare culture survives a few missed meals before it dies.
        if (flesh.isEmpty() && missedMeals >= 3) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(OrganismCoreBlock.PHASE, CorePhase.NECROTIC));
            level.playSound(null, worldPosition, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
            return;
        }
        if (flesh.isEmpty()) {
            setChanged();
            return;
        }
        // Starving: the farthest flesh withers one growth stage, and from tendrils back to bare frame.
        BlockPos withered = flesh.get(flesh.size() - 1);
        BlockState witheredState = level.getBlockState(withered);
        if (OrganismScan.isTissue(witheredState, aspect())) {
            int growth = witheredState.hasProperty(FleshBlock.GROWTH) ? witheredState.getValue(FleshBlock.GROWTH) : 0;
            level.setBlockAndUpdate(withered, growth > 0
                    ? witheredState.setValue(FleshBlock.GROWTH, growth - 1)
                    : ModBlocks.BRASS_ARMATURE.get().defaultBlockState());
            level.sendParticles(ParticleTypes.SMOKE, withered.getX() + 0.5, withered.getY() + 0.5, withered.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.01);
        }
        rescan(level);
        setChanged();
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("stored", stored);
        tag.putInt("size", size);
        tag.putInt("upkeep_timer", upkeepTimer);
        tag.putInt("missed_meals", missedMeals);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getInt("stored");
        size = tag.getInt("size");
        upkeepTimer = tag.getInt("upkeep_timer");
        missedMeals = tag.getInt("missed_meals");
    }
}
