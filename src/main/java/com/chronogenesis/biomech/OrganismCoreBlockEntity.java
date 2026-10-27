package com.chronogenesis.biomech;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.Config;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceDrain;
import com.chronogenesis.essence.EssenceStorage;
import com.chronogenesis.registry.ModBlockEntities;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The heart of a grown machine (core-systems.md §2). It hungers for its aspect: fed essence becomes flesh
 * growing out into the Copper Armature around it, one block at a time. Once the frame is full, essence is
 * stored instead, up to a capacity that grows with the body. Unfed, it burns its store, then eats its own
 * flesh, and finally turns Necrotic.
 */
public class OrganismCoreBlockEntity extends BlockEntity implements EssenceStorage {
    private static final int GROWTH_INTERVAL = 40;
    /** Failed feedings in a row before a fleshless organism dies. */
    private static final int STARVATION_LIMIT = 3;

    private int stored;
    private int fleshCount;
    private int starvation;
    private int age;

    public OrganismCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORGANISM_CORE.get(), pos, state);
    }

    public Aspect aspect() {
        return getBlockState().getValue(OrganismCoreBlock.ASPECT);
    }

    public boolean isNecrotic() {
        return getBlockState().getValue(OrganismCoreBlock.NECROTIC);
    }

    public int fleshCount() {
        return fleshCount;
    }

    /** Essence burnt per upkeep interval: one dram per eight blocks of flesh, at least one. */
    public int upkeep() {
        return 1 + fleshCount / 8;
    }

    @Override
    @Nullable
    public EssenceContents contents() {
        return stored > 0 ? new EssenceContents(aspect(), stored) : null;
    }

    @Override
    public int capacity() {
        return Config.ORGANISM_BASE_CAPACITY.get() + Config.ORGANISM_CAPACITY_PER_FLESH.get() * fleshCount;
    }

    @Override
    public boolean accepts(Aspect aspect) {
        return !isNecrotic() && aspect == aspect();
    }

    @Override
    public int insert(Aspect aspect, int drams, boolean simulate) {
        if (!accepts(aspect)) {
            return 0;
        }
        int amount = Math.max(0, Math.min(drams, capacity() - stored));
        if (amount > 0 && !simulate) {
            stored += amount;
            starvation = 0;
            setChanged();
        }
        return amount;
    }

    @Override
    public int extract(int drams, boolean simulate) {
        int amount = Math.min(drams, stored);
        if (amount > 0 && !simulate) {
            stored -= amount;
            setChanged();
        }
        return amount;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OrganismCoreBlockEntity core) {
        if (state.getValue(OrganismCoreBlock.NECROTIC)) {
            return;
        }
        core.age++;
        if (core.age % GROWTH_INTERVAL == 0) {
            core.grow((ServerLevel) level);
        }
        if (core.age % Config.ORGANISM_UPKEEP_INTERVAL.get() == 0) {
            core.feed((ServerLevel) level);
        }
    }

    private void grow(ServerLevel level) {
        Organisms.Body body = Organisms.scan(level, worldPosition, aspect());
        fleshCount = body.flesh().size();
        int cost = Config.ORGANISM_GROWTH_COST.get();
        if (stored >= cost && !body.frontier().isEmpty()) {
            BlockPos target = body.frontier().get(level.random.nextInt(body.frontier().size()));
            level.setBlockAndUpdate(target, ModBlocks.ORGANISM_FLESH.get().defaultBlockState()
                    .setValue(OrganismFleshBlock.ASPECT, aspect()));
            level.playSound(null, target, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 0.5f + level.random.nextFloat() * 0.3f);
            level.sendParticles(EssenceDrain.dust(aspect()), target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                    10, 0.4, 0.4, 0.4, 0);
            stored -= cost;
            fleshCount++;
        }
        // Flesh may have been cut away since the last check.
        stored = Math.min(stored, capacity());
        setChanged();
    }

    private void feed(ServerLevel level) {
        int upkeep = upkeep();
        if (stored >= upkeep) {
            stored -= upkeep;
            starvation = 0;
            setChanged();
            return;
        }
        stored = 0;
        starvation++;
        Organisms.Body body = Organisms.scan(level, worldPosition, aspect());
        if (!body.flesh().isEmpty()) {
            // It eats its own flesh, farthest first, leaving the bare frame behind.
            BlockPos eaten = body.flesh().getLast();
            level.setBlockAndUpdate(eaten, ModBlocks.COPPER_ARMATURE.get().defaultBlockState());
            level.playSound(null, eaten, SoundEvents.HONEY_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 0.5f);
            fleshCount = body.flesh().size() - 1;
        } else if (starvation >= STARVATION_LIMIT) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(OrganismCoreBlock.NECROTIC, true));
            level.playSound(null, worldPosition, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.BLOCKS, 0.6f, 0.4f);
        }
        setChanged();
    }

    /** Seeds a freshly planted core with the culture's essence. */
    public void setStored(int stored) {
        this.stored = stored;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getInt("Stored");
        fleshCount = tag.getInt("Flesh");
        starvation = tag.getInt("Starvation");
        age = tag.getInt("Age");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Stored", stored);
        tag.putInt("Flesh", fleshCount);
        tag.putInt("Starvation", starvation);
        tag.putInt("Age", age);
    }
}
