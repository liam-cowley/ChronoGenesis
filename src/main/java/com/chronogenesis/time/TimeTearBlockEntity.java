package com.chronogenesis.time;

import java.util.Map;
import java.util.Optional;

import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Ages blocks around the tear, swallows dropped items, and closes by itself after a day. */
public class TimeTearBlockEntity extends BlockEntity {
    /** One Minecraft day. */
    private static final int LIFETIME = 24000;
    private static final int AGE_RADIUS = 3;

    /** What a block crumbles into when time rushes over it. Copper uses its own weathering chain. */
    private static final Map<Block, Block> AGEING = Map.ofEntries(
            Map.entry(Blocks.STONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.COBBLESTONE, Blocks.GRAVEL),
            Map.entry(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS),
            Map.entry(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS),
            Map.entry(Blocks.GRASS_BLOCK, Blocks.DIRT),
            Map.entry(Blocks.FARMLAND, Blocks.DIRT),
            Map.entry(Blocks.DIRT, Blocks.COARSE_DIRT),
            Map.entry(Blocks.SANDSTONE, Blocks.SAND),
            Map.entry(Blocks.GLASS, Blocks.SAND),
            Map.entry(Blocks.OAK_PLANKS, Blocks.AIR),
            Map.entry(Blocks.SPRUCE_PLANKS, Blocks.AIR),
            Map.entry(Blocks.BIRCH_PLANKS, Blocks.AIR));

    private int age;

    public TimeTearBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TIME_TEAR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TimeTearBlockEntity tear) {
        ServerLevel serverLevel = (ServerLevel) level;
        tear.age++;
        if (tear.age % 20 == 0) {
            tear.setChanged();
        }
        if (tear.age >= LIFETIME) {
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0f, 0.5f);
            return;
        }

        swallowItems(serverLevel, pos);
        if (tear.age % 30 == 0) {
            ageRandomBlock(serverLevel, pos, serverLevel.getRandom());
        }
    }

    private static void swallowItems(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(1.5);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, item.getX(), item.getY(), item.getZ(), 10, 0.1, 0.1, 0.1, 0.05);
            item.discard();
        }
    }

    private static void ageRandomBlock(ServerLevel level, BlockPos center, RandomSource random) {
        BlockPos target = center.offset(
                random.nextInt(AGE_RADIUS * 2 + 1) - AGE_RADIUS,
                random.nextInt(AGE_RADIUS * 2 + 1) - AGE_RADIUS,
                random.nextInt(AGE_RADIUS * 2 + 1) - AGE_RADIUS);
        BlockState state = level.getBlockState(target);
        Optional<BlockState> aged = WeatheringCopper.getNext(state.getBlock())
                .map(next -> next.withPropertiesOf(state));
        if (aged.isEmpty()) {
            Block next = AGEING.get(state.getBlock());
            if (next != null) {
                aged = Optional.of(next.defaultBlockState());
            }
        }
        aged.ifPresent(newState -> {
            level.setBlockAndUpdate(target, newState);
            level.sendParticles(ParticleTypes.WHITE_ASH, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                    8, 0.4, 0.4, 0.4, 0.0);
        });
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("age", age);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        age = tag.getInt("age");
    }
}
