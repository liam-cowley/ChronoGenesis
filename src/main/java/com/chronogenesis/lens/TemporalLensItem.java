package com.chronogenesis.lens;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.network.PastViewPayload;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Temporal Lens (core-systems.md section 7): brass and glass with an amethyst lens. Hold use to look into the
 * past, sneak-use to change how far back. It burns Time from its own store (pour a Stasis Ampoule onto it), or
 * from your Time pool when that is empty. Through it, ruins stand whole again as ghosts, and faded blueprints
 * can be read: hold the view on one for {@value #SCAN_TICKS} ticks to copy it into the journal.
 */
public class TemporalLensItem extends EssenceContainerItem {
    public static final int CAPACITY = 16;
    /** Ticks between Time payments and view refreshes. */
    private static final int BEAT = 40;
    public static final int SCAN_TICKS = 40;
    private static final double SCAN_REACH = 6.0;

    /** Who is reading which blueprint, and for how long. Server only, forgotten on release. */
    private static final Map<UUID, Scan> SCANS = new HashMap<>();

    private record Scan(BlockPos pos, int ticks) {}

    public TemporalLensItem(Properties properties) {
        super(properties, CAPACITY, aspect -> aspect == Aspect.TIME);
    }

    public static LensDepth depth(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LENS_DEPTH.get(), LensDepth.DECADES);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            LensDepth next = depth(stack).next();
            stack.set(ModDataComponents.LENS_DEPTH.get(), next);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.lens.depth", next.displayName())
                        .withStyle(ChatFormatting.DARK_AQUA), true);
            }
            player.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0f, 1.4f);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (timeAvailable(player, stack) < depth(stack).cost()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.lens.no_time")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.playSound(SoundEvents.SPYGLASS_USE, 1.0f, 0.7f);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int used = getUseDuration(stack, entity) - remaining;
        if (used % BEAT == 0) {
            if (!pay(player, stack, depth(stack).cost())) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.lens.no_time")
                        .withStyle(ChatFormatting.GRAY), true);
                player.stopUsingItem();
                return;
            }
            PastViews.View view = PastViews.around(serverLevel, player.blockPosition(), depth(stack));
            view.clues().forEach(clue -> Clues.grant(player, clue));
            send(player, PastViewPayload.of(view));
        }
        scanTick(player, depth(stack));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        stopLooking(entity);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        stopLooking(entity);
        return stack;
    }

    private static void stopLooking(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            SCANS.remove(player.getUUID());
            send(player, PastViewPayload.CLOSED);
        }
        entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0f, 0.7f);
    }

    // ---------------------------------------------------------------- Time

    public static int timeAvailable(Player player, ItemStack lens) {
        EssenceContents own = EssenceItems.get(lens);
        return (own == null ? 0 : own.amount()) + Powers.get(player).pool(Aspect.TIME);
    }

    /** Burns Time: the lens's own store first, then the wearer's pool. */
    public static boolean pay(Player player, ItemStack lens, int cost) {
        if (timeAvailable(player, lens) < cost) {
            return false;
        }
        EssenceContents own = EssenceItems.get(lens);
        int fromLens = own == null ? 0 : Math.min(cost, own.amount());
        if (fromLens > 0) {
            EssenceItems.set(lens, own.withAmount(own.amount() - fromLens));
        }
        int fromPool = cost - fromLens;
        if (fromPool > 0) {
            Powers.spend(player, Aspect.TIME, fromPool);
        }
        return true;
    }

    // ---------------------------------------------------------------- reading blueprints

    /**
     * Holding the view on a faded blueprint (Decades or deeper) reads it a little more each tick; after
     * {@value #SCAN_TICKS} ticks it is copied into the journal as a clue. Returns true when one is finished.
     */
    public static boolean scanTick(ServerPlayer player, LensDepth depth) {
        BlockPos looked = lookedAtBlueprint(player);
        if (looked == null || !depth.reaches(LensDepth.DECADES)) {
            SCANS.remove(player.getUUID());
            return false;
        }
        Scan scan = SCANS.get(player.getUUID());
        int ticks = scan != null && scan.pos().equals(looked) ? scan.ticks() + 1 : 1;
        SCANS.put(player.getUUID(), new Scan(looked, ticks));
        if (ticks < SCAN_TICKS) {
            return false;
        }
        SCANS.remove(player.getUUID());
        BlockState state = player.level().getBlockState(looked);
        Clues.grant(player, state.getValue(FadedBlueprintBlock.DESIGN).clue());
        player.level().playSound(null, looked, SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.6f);
        player.displayClientMessage(Component.translatable("message.chronogenesis.lens.scanned").withStyle(ChatFormatting.DARK_AQUA), true);
        return true;
    }

    private static BlockPos lookedAtBlueprint(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(SCAN_REACH));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !player.level().getBlockState(hit.getBlockPos()).is(ModBlocks.FADED_BLUEPRINT.get())) {
            return null;
        }
        return hit.getBlockPos();
    }

    private static void send(ServerPlayer player, PastViewPayload payload) {
        if (!(player instanceof FakePlayer) && player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chronogenesis.lens.depth", depth(stack).displayName()).withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.chronogenesis.lens.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
