package com.chronogenesis.timetravel;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Cortex Offshoot: a bud of the Chronocortex in a small jar, roots through the glass. Planted in a past era it
 * is recorded ({@link Offshoots}) and grows through the ages into the Fossil Cortex that waits in the present.
 * (Its {@link OffshootItem} only plants it in an era.)
 */
public class CortexOffshootBlock extends Block {
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 10, 12);

    public CortexOffshootBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ResourceLocation era = Offshoots.eraOf(level);
        if (era == null) {
            return;
        }
        Offshoots.plant(server.getServer(), era, pos);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0);
        if (placer instanceof Player player) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.offshoot.planted",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.DARK_AQUA), false);
        }
    }
}
