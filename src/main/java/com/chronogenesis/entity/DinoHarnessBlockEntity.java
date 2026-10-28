package com.chronogenesis.entity;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Which beast a harness is buckled onto. */
public class DinoHarnessBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;

    public DinoHarnessBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DINO_HARNESS.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Nullable
    public Titanosaur owner() {
        return owner != null && level instanceof ServerLevel server && server.getEntity(owner) instanceof Titanosaur beast ? beast : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
