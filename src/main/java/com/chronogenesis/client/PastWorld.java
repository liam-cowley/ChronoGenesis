package com.chronogenesis.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
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
 * The past, as the client knows it, for rendering: blocks and light from the past dimension's sections the server
 * sent ({@link PastSections}). Where nothing was sent, it is open air under the sky. Tint, shade and height come
 * from the present level (same seed, same biomes).
 */
public final class PastWorld implements BlockAndTintGetter {
    private final Level present;

    public PastWorld(Level present) {
        this.present = present;
    }

    @Nullable
    private static PastSections.Section section(BlockPos pos) {
        return PastSections.get(SectionPos.asLong(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getY()), SectionPos.blockToSectionCoord(pos.getZ())));
    }

    /**
     * Where the past was not sent, the edge must not look like the edge of the world (walls of cut-away
     * terrain): unknown space counts as solid ground, so the faces bordering it are culled, except straight
     * above what is known, where it is open sky.
     */
    @Override
    public BlockState getBlockState(BlockPos pos) {
        PastSections.Section section = section(pos);
        if (section == null) {
            return section(pos.below(16)) != null ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        return section.blocks().getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        PastSections.Section section = section(pos);
        if (section == null) {
            return layer == LightLayer.SKY ? 15 : 0;
        }
        return (layer == LightLayer.SKY ? section.sky() : section.block()).get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return Math.max(getBrightness(LightLayer.SKY, pos) - amount, getBrightness(LightLayer.BLOCK, pos));
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return present.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return present.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return present.getBlockTint(pos, resolver);
    }

    @Override
    public int getHeight() {
        return present.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return present.getMinBuildHeight();
    }
}
