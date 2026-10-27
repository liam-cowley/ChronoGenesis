package com.chronogenesis.organism;

import com.chronogenesis.Config;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click behaviour shared by all organism tissue (flesh and cores). */
public final class OrganismInteractions {
    private OrganismInteractions() {}

    /** Feeding: pour a container of the matching aspect into the organism. */
    public static ItemInteractionResult useItemOnTissue(ItemStack stack, Level level, BlockPos pos, Player player,
            InteractionHand hand) {
        EssenceContents contents = EssenceItems.get(stack);
        if (contents == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        OrganismCoreBlockEntity core = OrganismScan.findCore(level, pos, Config.MAX_ORGANISM_SIZE.getAsInt());
        if (core == null || core.isNecrotic()) {
            tell(player, Component.translatable("message.chronogenesis.organism.dead"));
            return ItemInteractionResult.FAIL;
        }
        if (contents.aspect() != core.aspect()) {
            tell(player, Component.translatable("message.chronogenesis.organism.recoils", contents.aspect().displayName()));
            level.playSound(null, pos, SoundEvents.SLIME_HURT, SoundSource.BLOCKS, 0.8f, 0.6f);
            return ItemInteractionResult.FAIL;
        }
        int room = core.room();
        if (room <= 0) {
            tell(player, Component.translatable("message.chronogenesis.organism.sated"));
            return ItemInteractionResult.FAIL;
        }
        EssenceContents taken = EssenceItems.drainHeld(player, hand, core.aspect(), room);
        if (taken != null) {
            core.feed(taken.amount());
            level.playSound(null, pos, SoundEvents.HONEY_DRINK, SoundSource.BLOCKS, 0.8f, 0.7f);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** Empty-handed: report the organism's state. */
    public static InteractionResult inspect(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        OrganismCoreBlockEntity core = OrganismScan.findCore(level, pos, Config.MAX_ORGANISM_SIZE.getAsInt());
        if (core == null) {
            tell(player, Component.translatable("message.chronogenesis.organism.dead"));
        } else {
            tell(player, core.statusLine());
        }
        return InteractionResult.SUCCESS;
    }

    static void tell(Player player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
    }
}
