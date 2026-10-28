package com.chronogenesis.timetravel;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Which era a Fossil Cortex grew through (its memories). Whether it is linked is the block's {@code awake}. */
public class FossilCortexBlockEntity extends BlockEntity {
    @Nullable
    private ResourceLocation era;

    public FossilCortexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOSSIL_CORTEX.get(), pos, state);
    }

    @Nullable
    public ResourceLocation era() {
        return era;
    }

    public void setEra(ResourceLocation era) {
        this.era = era;
        setChanged();
    }

    public boolean linked() {
        return getBlockState().getValue(FossilCortexBlock.AWAKE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (era != null) {
            tag.putString("era", era.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        era = tag.contains("era") ? ResourceLocation.tryParse(tag.getString("era")) : null;
    }
}
