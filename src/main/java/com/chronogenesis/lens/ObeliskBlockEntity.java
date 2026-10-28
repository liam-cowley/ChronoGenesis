package com.chronogenesis.lens;

import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marks an obelisk's capstone. Holds no data: it exists so obelisks can be found among a chunk's block entities. */
public class ObeliskBlockEntity extends BlockEntity {
    public ObeliskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OBELISK.get(), pos, state);
    }
}
