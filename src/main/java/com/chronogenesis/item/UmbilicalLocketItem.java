package com.chronogenesis.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceMoteOptions;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.GraftType;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Powers;
import com.chronogenesis.player.TemporalExposure;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Umbilical Locket (core-systems.md section 3): a bauble worn in the Curios necklace slot. Use it on an
 * organism's Tap to tie it to that organism; while you wear it within {@value #RANGE} blocks of it, it slowly
 * refills your pool of the organism's essence. A literal umbilical cord to your lab.
 */
public class UmbilicalLocketItem extends Item implements ICurioItem {
    /** How far the cord stretches, in blocks. */
    public static final int RANGE = 48;
    private static final int INTERVAL = 40;
    /** Drams drawn per beat: slower than standing at the Gland itself. */
    private static final int DRAMS_PER_BEAT = 2;

    public UmbilicalLocketItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static GlobalPos link(ItemStack stack) {
        return stack.get(ModDataComponents.LOCKET_LINK.get());
    }

    /** Tie the locket to the organism behind a Tap. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof GraftBlock graft) || graft.type() != GraftType.TAP) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GraftBlockEntity tap && context.getPlayer() != null) {
            OrganismCoreBlockEntity core = tap.core();
            Player player = context.getPlayer();
            if (core == null) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.organism.dead"), true);
                return InteractionResult.FAIL;
            }
            context.getItemInHand().set(ModDataComponents.LOCKET_LINK.get(), GlobalPos.of(level.dimension(), core.getBlockPos()));
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0f, 0.6f);
            player.displayClientMessage(Component.translatable("message.chronogenesis.locket.linked", core.aspect().displayName())
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof ServerPlayer player && player.tickCount % INTERVAL == 0) {
            refill(player, stack);
        }
    }

    /**
     * One beat of the cord: if the linked organism is alive, in this dimension and in range, draw a little of
     * its essence into the wearer's pool. Returns the drams drawn.
     */
    public static int refill(ServerPlayer player, ItemStack stack) {
        GlobalPos link = link(stack);
        if (link == null || !link.dimension().equals(player.level().dimension())
                || link.pos().distToCenterSqr(player.position()) > (double) RANGE * RANGE
                || !(player.level().getBlockEntity(link.pos()) instanceof OrganismCoreBlockEntity core) || core.isNecrotic()) {
            return 0;
        }
        Aspect aspect = core.aspect();
        PlayerEssence essence = Powers.get(player);
        if (essence.level(aspect) == 0) {
            return 0; // never infused with it: there is no pool to fill
        }
        int room = essence.max(aspect) - essence.pool(aspect);
        int taken = core.extract(Math.min(room, DRAMS_PER_BEAT));
        if (taken <= 0) {
            return 0;
        }
        essence = essence.withPool(aspect, essence.pool(aspect) + taken);
        if (aspect == Aspect.TIME) {
            essence = essence.withExposure(essence.exposure() + TemporalExposure.scaled(player, taken));
        }
        Powers.set(player, essence);
        // A thread of essence leaves the organism for the wearer.
        ServerLevel level = player.serverLevel();
        BlockPos at = link.pos();
        level.sendParticles(EssenceMoteOptions.of(aspect, chest(player)),
                at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 3 * taken, 0.2, 0.2, 0.2, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.25f, 1.6f);
        return taken;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos link = link(stack);
        if (link == null) {
            tooltip.add(Component.translatable("tooltip.chronogenesis.locket.unlinked").withStyle(ChatFormatting.GRAY));
        } else {
            BlockPos pos = link.pos();
            tooltip.add(Component.translatable("tooltip.chronogenesis.locket.linked", pos.getX(), pos.getY(), pos.getZ(),
                    link.dimension().location().toString()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.chronogenesis.locket.range", RANGE).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return link(stack) != null;
    }

    /** Where the cord's motes fly to: the wearer's chest, where the locket hangs. */
    private static Vec3 chest(Player player) {
        return player.getEyePosition().subtract(0, 0.4, 0);
    }
}
