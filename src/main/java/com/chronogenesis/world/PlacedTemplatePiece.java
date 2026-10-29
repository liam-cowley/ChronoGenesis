package com.chronogenesis.world;

import com.chronogenesis.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * A {@link PlacedTemplateStructure}'s (or {@link RemainsStructure}'s) template: turned about its own centre, so it
 * stays centred where it was put. With {@code foundations}, whatever it built on its bottom layer is carried down
 * through air and water to solid ground (piers into the sea, footings under a city on uneven land).
 */
public class PlacedTemplatePiece extends TemplateStructurePiece {
    /** How far foundations reach down at most. */
    private static final int FOUNDATION_DEPTH = 64;
    private final boolean foundations;

    public PlacedTemplatePiece(StructureTemplateManager manager, ResourceLocation id, BlockPos origin, Rotation rotation) {
        this(manager, id, origin, rotation, false);
    }

    public PlacedTemplatePiece(StructureTemplateManager manager, ResourceLocation id, BlockPos origin, Rotation rotation,
            boolean foundations) {
        super(ModStructures.PLACED_TEMPLATE_PIECE.get(), 0, manager, id, id.toString(), settings(manager, id, rotation), origin);
        this.foundations = foundations;
    }

    public PlacedTemplatePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.PLACED_TEMPLATE_PIECE.get(), tag, manager,
                id -> settings(manager, id, Rotation.valueOf(tag.getString("Rot"))));
        this.foundations = tag.getBoolean("Found");
    }

    private static StructurePlaceSettings settings(StructureTemplateManager manager, ResourceLocation id, Rotation rotation) {
        Vec3i size = manager.getOrCreate(id).getSize();
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
        tag.putBoolean("Found", foundations);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, chunkBox, chunkPos, pivot);
        if (!foundations) {
            return;
        }
        BoundingBox box = getBoundingBox();
        int bottom = box.minY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), chunkBox.minX()); x <= Math.min(box.maxX(), chunkBox.maxX()); x++) {
            for (int z = Math.max(box.minZ(), chunkBox.minZ()); z <= Math.min(box.maxZ(), chunkBox.maxZ()); z++) {
                BlockState footing = level.getBlockState(pos.set(x, bottom, z));
                if (!built(footing)) {
                    continue;
                }
                for (int y = bottom - 1; y > Math.max(level.getMinBuildHeight(), bottom - FOUNDATION_DEPTH); y--) {
                    BlockState below = level.getBlockState(pos.setY(y));
                    if (!below.isAir() && !below.canBeReplaced() && below.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(pos, footing, 2);
                }
            }
        }
    }

    /** Something the template built (not the land it was set on, nor the sea it stands in). */
    private static boolean built(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()
                && !state.is(BlockTags.DIRT) && !state.is(BlockTags.BASE_STONE_OVERWORLD) && !state.is(BlockTags.SAND)
                && !state.is(Blocks.GRAVEL) && !state.is(BlockTags.ICE) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)
                && !state.is(Blocks.SANDSTONE) && !state.is(Blocks.SNOW_BLOCK) && !state.is(Blocks.POWDER_SNOW) && !state.is(Blocks.CLAY);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }
}
