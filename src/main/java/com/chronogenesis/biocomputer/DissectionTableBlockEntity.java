package com.chronogenesis.biocomputer;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The reanimation altar (early-game.md, "It's Alive!"). It needs a brain in a jar, Awareness and Time
 * poured in by Taps facing it (Time also by hand, from Stasis Ampoules), and lightning rods on its frame.
 * Then lightning does the rest: see {@link ReanimationRitual}.
 */
public class DissectionTableBlockEntity extends BlockEntity {
    /** Drams of each of Awareness and Time the brain must soak up before it can be woken. */
    public static final int NEEDED = 16;

    private int awareness;
    private int time;

    public DissectionTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISSECTION_TABLE.get(), pos, state);
    }

    public boolean hasBrain() {
        return getBlockState().getValue(DissectionTableBlock.BRAIN);
    }

    public int awareness() {
        return awareness;
    }

    public int time() {
        return time;
    }

    /** Only a brain soaks anything up, and only Awareness and Time. */
    public int space(Aspect aspect) {
        if (!hasBrain()) {
            return 0;
        }
        return switch (aspect) {
            case AWARENESS -> NEEDED - awareness;
            case TIME -> NEEDED - time;
            default -> 0;
        };
    }

    public int insert(Aspect aspect, int amount) {
        int moved = Math.max(0, Math.min(amount, space(aspect)));
        if (moved > 0) {
            if (aspect == Aspect.AWARENESS) {
                awareness += moved;
            } else {
                time += moved;
            }
            setChanged();
        }
        return moved;
    }

    /** The brain is gone (taken out, or burnt by lightning it was not ready for): so is what it soaked up. */
    public void clear() {
        awareness = 0;
        time = 0;
        setChanged();
    }

    /** Lightning rods on the table's frame: anywhere in the 3x3 above it, or beside it. */
    public int rods() {
        if (level == null) {
            return 0;
        }
        int rods = 0;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-1, 0, -1), worldPosition.offset(1, 1, 1))) {
            if (level.getBlockState(pos).getBlock() instanceof LightningRodBlock) {
                rods++;
            }
        }
        return rods;
    }

    public boolean isReady() {
        return hasBrain() && awareness >= NEEDED && time >= NEEDED;
    }

    public static boolean isReady(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DissectionTableBlockEntity table && table.isReady() && table.rods() > 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("awareness", awareness);
        tag.putInt("time", time);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        awareness = tag.getInt("awareness");
        time = tag.getInt("time");
    }
}
