package com.chronogenesis.item;

import java.util.List;

import com.chronogenesis.Config;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.CortexLobeBlock;
import com.chronogenesis.biocomputer.DissectionTableBlockEntity;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.journal.JournalUnlock;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.organism.EssenceJugBlockEntity;
import com.chronogenesis.organism.GraftBlockEntity;
import com.chronogenesis.organism.OrganismCoreBlockEntity;
import com.chronogenesis.organism.OrganismScan;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Power;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creative-only testing aid (no recipe; found only in the creative tab). While it is anywhere in a player's
 * inventory, every power is at its highest level with a full pool, Temporal Exposure stays at zero,
 * instruments stay charged (every Temporal Lens carried is kept full of Time and costs nothing to look through),
 * and research at a Chronocortex draws its essence from the hourglass instead of wired organisms.
 * <p>
 * It is also an endless reservoir of every essence. Used on an organism (flesh, core or graft) it fills it with
 * its own essence; on an Essence Jug, with the jug's essence (or, if empty, the essence chosen with sneak-use in
 * the air); on a Dissection Table holding a brain, with the Awareness and Time the ritual needs; on the
 * Chronocortex (or one of its lobes), it fills every wired organism and grows every lobe its armature allows.
 */
public class EternalHourglassItem extends Item {
    private static final int INTERVAL = 10;

    public EternalHourglassItem(Properties properties) {
        super(properties);
    }

    /** Whether the player carries an Eternal Hourglass. */
    public static boolean carried(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.ETERNAL_HOURGLASS.get()));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player && player.tickCount % INTERVAL == 0) {
            saturate(player);
        }
    }

    /** Fills every pool at the deepest power level (the Eldritch one), teaches every composite power, clears Exposure, and charges every Lens carried. */
    public static void saturate(ServerPlayer player) {
        PlayerEssence before = Powers.get(player);
        PlayerEssence essence = before;
        for (Aspect aspect : Aspect.values()) {
            if (essence.level(aspect) < com.chronogenesis.organism.GlandBlock.PUMPED_LEVEL) {
                // Every power, the Eldritch ones too.
                essence = essence.withLevel(aspect, com.chronogenesis.organism.GlandBlock.PUMPED_LEVEL);
            }
            essence = essence.withPool(aspect, essence.max(aspect));
        }
        for (Power power : Power.values()) {
            if (power.isComposite()) {
                essence = essence.withComposite(power);
            }
        }
        essence = essence.withExposure(0);
        if (!essence.equals(before)) {
            Powers.set(player, essence);
            Powers.applyModifiers(player);
        }
        for (ItemStack carried : player.getInventory().items) {
            charge(carried);
        }
        player.getInventory().offhand.forEach(EternalHourglassItem::charge);
    }

    private static void charge(ItemStack stack) {
        if (stack.getItem() instanceof TemporalLensItem) {
            EssenceContents own = EssenceItems.get(stack);
            if (own == null || own.amount() < TemporalLensItem.CAPACITY) {
                EssenceItems.set(stack, new EssenceContents(Aspect.TIME, TemporalLensItem.CAPACITY));
            }
        }
    }

    /** The essence it pours into things that have none of their own yet (an empty jug). */
    public static Aspect chosen(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ASPECT.get(), Aspect.DENSITY);
    }

    /** Pours into what it is used on, before the block itself reacts. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean target = blockEntity instanceof ChronocortexBlockEntity || state.is(ModBlocks.CORTEX_LOBE.get())
                || blockEntity instanceof EssenceJugBlockEntity || blockEntity instanceof DissectionTableBlockEntity
                || blockEntity instanceof GraftBlockEntity || state.is(ModBlocks.CHRONOFLESH.get())
                || state.is(ModBlocks.ORGANISM_CORE.get());
        if (!target) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Component said = pour(serverLevel, pos, state, blockEntity, stack);
        context.getPlayer().displayClientMessage(said.copy().withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.7f, 1.4f);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.02);
        return InteractionResult.SUCCESS;
    }

    private static Component pour(ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack) {
        ChronocortexBlockEntity cortex = blockEntity instanceof ChronocortexBlockEntity c ? c
                : state.is(ModBlocks.CORTEX_LOBE.get()) ? CortexLobeBlock.cortexOf(level, pos) : null;
        if (cortex != null) {
            return cortex.saturate(level);
        }
        if (blockEntity instanceof EssenceJugBlockEntity jug) {
            Aspect aspect = jug.contents() != null ? jug.contents().aspect() : chosen(stack);
            if (!aspect.fitsInGlass()) {
                return Component.translatable("message.chronogenesis.hourglass.not_glass", aspect.displayName());
            }
            return poured(jug.insert(aspect, EssenceJugBlockEntity.CAPACITY), aspect);
        }
        if (blockEntity instanceof DissectionTableBlockEntity table) {
            if (!table.hasBrain()) {
                return Component.translatable("message.chronogenesis.table.no_brain");
            }
            int moved = table.insert(Aspect.AWARENESS, DissectionTableBlockEntity.NEEDED)
                    + table.insert(Aspect.TIME, DissectionTableBlockEntity.NEEDED);
            return Component.translatable("message.chronogenesis.hourglass.table", moved);
        }
        OrganismCoreBlockEntity core = blockEntity instanceof GraftBlockEntity graft ? graft.core()
                : OrganismScan.findCore(level, pos, Config.MAX_ORGANISM_SIZE.getAsInt());
        if (core == null) {
            return Component.translatable("message.chronogenesis.organism.dead");
        }
        return poured(core.feed(core.room()), core.aspect());
    }

    private static Component poured(int drams, Aspect aspect) {
        return drams == 0 ? Component.translatable("message.chronogenesis.hourglass.full")
                : Component.translatable("message.chronogenesis.hourglass.poured", drams, aspect.displayName());
    }

    /** Sneak-use in the air: the next essence to pour into empty things. Otherwise, in creative, the whole journal. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                Aspect next = Aspect.values()[(chosen(stack).ordinal() + 1) % Aspect.values().length];
                stack.set(ModDataComponents.ASPECT.get(), next);
                player.displayClientMessage(Component.translatable("message.chronogenesis.hourglass.chosen",
                        next.displayName()).withStyle(style -> style.withColor(next.color())), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.isCreative()) {
                JournalUnlock.unlockAll(serverPlayer);
            } else {
                player.displayClientMessage(Component.translatable("message.chronogenesis.journal.creative_only")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass.pour").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass.chosen", chosen(stack).displayName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass.journal").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.chronogenesis.eternal_hourglass.creative").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
