package com.chronogenesis.world;

import com.chronogenesis.entity.Precursor;
import com.chronogenesis.entity.PrecursorGuards;
import com.chronogenesis.registry.ModEntities;
import com.chronogenesis.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Laid over remains as they were before they fell (see {@link RemainsStructure}): once the remains' own pieces have
 * been built in a chunk, this mends them there (cracked stone whole again, the sculk's overgrowth gone back to the
 * listening organs they kept, candles lit, the dead cleared away, the shriekers unable to call anything) and brings
 * back its people and its guards.
 */
public class RestoredPiece extends StructurePiece {
    /** Columns tried per chunk for someone to stand in. */
    private static final int TRIES = 6;
    private final String kingdom;
    private final EntityType<?> guard;
    private final float folkChance;
    private final float guardChance;

    public RestoredPiece(BoundingBox box, String kingdom, EntityType<?> guard, float folkChance, float guardChance) {
        super(ModStructures.RESTORED_PIECE.get(), 0, box);
        this.kingdom = kingdom;
        this.guard = guard;
        this.folkChance = folkChance;
        this.guardChance = guardChance;
    }

    public RestoredPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.RESTORED_PIECE.get(), tag);
        this.kingdom = tag.getString("Kingdom");
        this.guard = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(tag.getString("Guard")));
        this.folkChance = tag.getFloat("Folk");
        this.guardChance = tag.getFloat("GuardChance");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kingdom", kingdom);
        tag.putString("Guard", BuiltInRegistries.ENTITY_TYPE.getKey(guard).toString());
        tag.putFloat("Folk", folkChance);
        tag.putFloat("GuardChance", guardChance);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BoundingBox box = getBoundingBox();
        int x0 = Math.max(box.minX(), chunkBox.minX());
        int x1 = Math.min(box.maxX(), chunkBox.maxX());
        int z0 = Math.max(box.minZ(), chunkBox.minZ());
        int z1 = Math.min(box.maxZ(), chunkBox.maxZ());
        if (x0 > x1 || z0 > z1) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    BlockState mended = mend(state);
                    if (mended != state) {
                        level.setBlock(pos, mended, 2);
                    }
                }
            }
        }
        for (int i = 0; i < TRIES; i++) {
            boolean isGuard = random.nextFloat() < guardChance / TRIES * 3;
            if (!isGuard && random.nextFloat() >= folkChance) {
                continue;
            }
            BlockPos spot = floor(level, random.nextIntBetweenInclusive(x0, x1), random.nextIntBetweenInclusive(z0, z1), box);
            if (spot != null) {
                spawn(level, spot, isGuard);
            }
        }
    }

    /** What a block of the fallen remains was before it fell. */
    public static BlockState mend(BlockState state) {
        if (state.is(Blocks.CRACKED_DEEPSLATE_BRICKS)) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (state.is(Blocks.CRACKED_DEEPSLATE_TILES)) {
            return Blocks.DEEPSLATE_TILES.defaultBlockState();
        }
        if (state.is(Blocks.SCULK)) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (state.is(Blocks.SCULK_VEIN) || state.is(Blocks.SKELETON_SKULL) || state.is(Blocks.SKELETON_WALL_SKULL)
                || state.is(Blocks.COBWEB)) {
            return state.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
        }
        if (state.is(Blocks.SCULK_SHRIEKER)) {
            return state.setValue(SculkShriekerBlock.CAN_SUMMON, false);
        }
        if (state.getBlock() instanceof CandleBlock && !state.getValue(CandleBlock.LIT)
                && !state.getValue(BlockStateProperties.WATERLOGGED)) {
            return state.setValue(CandleBlock.LIT, true);
        }
        if (state.getBlock() instanceof CampfireBlock && !state.getValue(CampfireBlock.LIT)) {
            return state.setValue(CampfireBlock.LIT, true);
        }
        return state;
    }

    /** Somewhere to stand in this column of the city: a built floor with three blocks of air over it. */
    private static BlockPos floor(WorldGenLevel level, int x, int z, BoundingBox box) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, box.maxY(), z);
        int air = 0;
        for (int y = box.maxY(); y > box.minY(); y--) {
            BlockState state = level.getBlockState(pos.setY(y));
            if (state.isAir()) {
                air++;
            } else {
                if (air >= 3 && (state.is(Blocks.DEEPSLATE_BRICKS) || state.is(Blocks.DEEPSLATE_TILES)
                        || state.is(Blocks.POLISHED_DEEPSLATE) || state.is(Blocks.CHISELED_DEEPSLATE)
                        || state.is(Blocks.GRAY_WOOL) || state.is(Blocks.LIGHT_BLUE_WOOL) || state.is(Blocks.BLUE_ICE)
                        || state.is(Blocks.SMOOTH_BASALT) || state.is(Blocks.POLISHED_BASALT))) {
                    return new BlockPos(x, y + 1, z);
                }
                air = 0;
            }
        }
        return null;
    }

    private void spawn(WorldGenLevel level, BlockPos spot, boolean isGuard) {
        Entity entity = (isGuard ? guard : ModEntities.PRECURSOR.get()).create(level.getLevel());
        if (entity == null) {
            return;
        }
        entity.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
        if (entity instanceof Precursor precursor) {
            precursor.setKingdom(kingdom);
        }
        if (isGuard) {
            PrecursorGuards.makeGuard(entity);
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntityWithPassengers(entity);
    }
}
