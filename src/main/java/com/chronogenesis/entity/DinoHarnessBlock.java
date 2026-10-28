package com.chronogenesis.entity;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * A Dino Harness, buckled on a Titanosaur's back: the anchor its cargo hangs from ({@link DinoCargo}). Blocks placed
 * touching the harness, or touching anything already attached, join the cargo; breaking one off detaches it.
 * Breaking the harness itself (by any means) drops the whole cargo at once.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public class DinoHarnessBlock extends BaseEntityBlock {
    public static final MapCodec<DinoHarnessBlock> CODEC = simpleCodec(DinoHarnessBlock::new);
    /** How far from a placed block a carrying beast is looked for. */
    private static final double SEARCH = 48.0;

    public DinoHarnessBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DinoHarnessBlockEntity(pos, state);
    }

    /** Broken, blown up, or pushed: unless it is only its beast carrying it along, the cargo falls. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !movedByPiston && level.getBlockEntity(pos) instanceof DinoHarnessBlockEntity harness) {
            Titanosaur beast = harness.owner();
            if (beast != null && !beast.cargo().moving() && pos.equals(beast.cargo().harness())) {
                beast.cargo().collapse();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** A block placed against a beast's cargo joins it. */
    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.isCanceled()) {
            return;
        }
        BlockPos pos = event.getPos();
        for (Titanosaur beast : level.getEntitiesOfClass(Titanosaur.class, new net.minecraft.world.phys.AABB(pos).inflate(SEARCH),
                beast -> beast.cargo().harnessed())) {
            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
                if (beast.cargo().contains(pos.relative(side))) {
                    beast.cargo().attach(pos);
                    return;
                }
            }
        }
    }

    /** A block broken off a beast's cargo leaves it. */
    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (Titanosaur beast : level.getEntitiesOfClass(Titanosaur.class,
                    new net.minecraft.world.phys.AABB(event.getPos()).inflate(SEARCH), beast -> beast.cargo().contains(event.getPos()))) {
                beast.cargo().detach(event.getPos());
            }
        }
    }
}
