package com.chronogenesis.organism;

import com.chronogenesis.registry.ModItems;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Enhancing Gland: infuses the player with its organism's essence (core-systems.md section 3).
 * <ul>
 *   <li>{@link #LEVEL}: how deep it fills you. A gland grows a level when fed a culture of its own essence, with
 *       drams from its organism, once the Chronocortex has worked out how (Deeper Infusion for level 2, Deepest
 *       Infusion for level 3). Infusing raises your power level for the essence to the gland's level.</li>
 *   <li>{@link #NEEDLE}: a Confluence Needle in its port. If its organism is wired by veins to an organism of
 *       another essence, the gland mixes both into you, and you learn their composite power.</li>
 * </ul>
 */
public class GlandBlock extends GraftBlock {
    public static final MapCodec<GlandBlock> CODEC = simpleCodec(GlandBlock::new);
    public static final int MAX_LEVEL = 3;
    public static final IntegerProperty LEVEL = IntegerProperty.create("gland_level", 1, MAX_LEVEL);
    public static final BooleanProperty NEEDLE = BooleanProperty.create("needle");

    public GlandBlock(Properties properties) {
        super(GraftType.ENHANCING_GLAND, properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 1).setValue(NEEDLE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL, NEEDLE);
    }

    /** The needle is not part of the gland: it falls out when the gland is cut away. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getValue(NEEDLE) && !newState.is(this)) {
            popResource(level, pos, new ItemStack(ModItems.CONFLUENCE_NEEDLE.get()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
