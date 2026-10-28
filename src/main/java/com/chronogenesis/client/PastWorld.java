package com.chronogenesis.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

/**
 * The world as the Temporal Lens shows it: the past's blocks where the past is known, air where something
 * stands now that was not there then, and the present everywhere else. Block models are tessellated against
 * this, so faces the present hides (under rubble, behind a cobweb) appear, and faces the past hides do not.
 * Inside the past's areas, light is the past's own ({@link PastLight}): removed blocks cast no shadow. Tint and
 * the rest come from the real level.
 */
public final class PastWorld implements BlockAndTintGetter {
    private final Level level;
    private final Map<BlockPos, BlockState> past;
    private final Set<BlockPos> erased;
    private List<PastLight> light = List.of();

    public PastWorld(Level level, Map<BlockPos, BlockState> past, Set<BlockPos> erased) {
        this.level = level;
        this.past = past;
        this.erased = erased;
    }

    /** Sets the past's own light for its areas (computed from this world, so it is set afterwards). */
    public void setLight(List<PastLight> light) {
        this.light = light;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        for (PastLight area : light) {
            if (area.contains(pos)) {
                return area.get(layer, pos);
            }
        }
        return level.getBrightness(layer, pos);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return Math.max(getBrightness(LightLayer.SKY, pos) - amount, getBrightness(LightLayer.BLOCK, pos));
    }

    private boolean changed(BlockPos pos) {
        return past.containsKey(pos) || erased.contains(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState then = past.get(pos);
        if (then != null) {
            return then;
        }
        return erased.contains(pos) ? Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return changed(pos) ? getBlockState(pos).getFluidState() : level.getFluidState(pos);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return changed(pos) ? null : level.getBlockEntity(pos);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return level.getBlockTint(pos, resolver);
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }
}
