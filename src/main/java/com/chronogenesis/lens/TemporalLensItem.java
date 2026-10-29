package com.chronogenesis.lens;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.item.EssenceContainerItem;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModDataComponents;
import com.chronogenesis.registry.ModSounds;
import com.chronogenesis.timetravel.Era;
import com.chronogenesis.timetravel.Eras;
import com.chronogenesis.timetravel.PastDimension;
import com.chronogenesis.timetravel.PastStamping;
import com.chronogenesis.timetravel.TemporalShift;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

/**
 * The Temporal Lens (core-systems.md section 7): brass and glass with an amethyst lens. Hold use to look into the
 * past, sneak-use to change how far back. It burns Time from its own store (pour a Stasis Ampoule onto it), or
 * from your Time pool when that is empty. Through it, ruins stand whole again as ghosts, and faded blueprints
 * can be read: hold the view on one for {@value #SCAN_TICKS} ticks to copy it into the journal.
 */
public class TemporalLensItem extends EssenceContainerItem {
    public static final int CAPACITY = 16;
    /** Ticks between Time payments (and stamping the past of ruins in view). */
    private static final int BEAT = 40;
    /** Ticks between bursts of the past sent to the client. */
    private static final int SYNC_INTERVAL = 5;
    public static final int SCAN_TICKS = 40;
    private static final double SCAN_REACH = 6.0;

    /** Who is reading which blueprint, and for how long. Server only, forgotten on release. */
    private static final Map<UUID, Scan> SCANS = new HashMap<>();
    /** Ticks each viewer has held their gaze on an obelisk from the edge of its horizon. Server only. */
    private static final Map<UUID, Integer> EDGE_READING = new HashMap<>();
    /** How far an obelisk can be seen through the Lens. */
    public static final double OBELISK_SIGHT = 48.0;
    public static final int INSCRIPTION_TICKS = 60;
    /** Drams of Time per two seconds of looking into an era. */
    public static final int ERA_COST = 3;

    private record Scan(BlockPos pos, int ticks) {}

    public TemporalLensItem(Properties properties) {
        super(properties, CAPACITY, aspect -> aspect == Aspect.TIME);
    }

    public static LensDepth depth(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LENS_DEPTH.get(), LensDepth.DECADES);
    }

    /** The era the Lens looks into instead of its depth, if it is set to one. */
    @Nullable
    public static ResourceLocation era(ItemStack stack) {
        return stack.get(ModDataComponents.LENS_ERA.get());
    }

    /** What the Lens is set to: its depth, or the era's name. */
    public static Component setting(ItemStack stack, Level level) {
        ResourceLocation id = era(stack);
        if (id == null) {
            return depth(stack).displayName();
        }
        Era era = Eras.get(level.registryAccess(), id);
        return era == null ? Component.literal(id.toString()) : era.title();
    }

    /** Drams of Time burnt every two seconds: an era costs more than any depth. */
    public static int cost(ItemStack stack) {
        return era(stack) != null ? ERA_COST : depth(stack).cost();
    }

    /**
     * Sneak-use: the next setting. Echo, Decades, Centuries, then every era the holder has stood in that it can see into (oldest last),
     * then round again.
     */
    public static void cycle(Player player, ItemStack stack) {
        List<ResourceLocation> eras = new ArrayList<>(TemporalShift.visited(player).stream()
                .filter(id -> Eras.get(player.level().registryAccess(), id) != null && Eras.get(player.level().registryAccess(), id).lens()).toList());
        eras.sort(Comparator.comparingInt(id -> Eras.get(player.level().registryAccess(), id).order()));
        int depths = LensDepth.values().length;
        ResourceLocation current = era(stack);
        int index = current != null && eras.contains(current) ? depths + eras.indexOf(current)
                : current != null ? depths - 1 : depth(stack).ordinal();
        int next = (index + 1) % (depths + eras.size());
        if (next < depths) {
            stack.set(ModDataComponents.LENS_DEPTH.get(), LensDepth.values()[next]);
            stack.remove(ModDataComponents.LENS_ERA.get());
        } else {
            stack.set(ModDataComponents.LENS_ERA.get(), eras.get(next - depths));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                cycle(player, stack);
                player.displayClientMessage(Component.translatable("message.chronogenesis.lens.depth", setting(stack, level))
                        .withStyle(ChatFormatting.DARK_AQUA), true);
            }
            player.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0f, 1.4f);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (timeAvailable(player, stack) < cost(stack)) {
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
            if (!pay(player, stack, cost(stack))) {
                player.displayClientMessage(Component.translatable("message.chronogenesis.lens.no_time")
                        .withStyle(ChatFormatting.GRAY), true);
                player.stopUsingItem();
                return;
            }
            // The ruins in view stand whole in the past; seeing one for the first time is worth a note.
            ServerLevel past = PastDimension.level(player.server);
            if (era(stack) == null && past != null && PastDimension.hasPast(serverLevel)) {
                for (PastViews.Found found : PastViews.nearby(serverLevel, player.blockPosition(), depth(stack))) {
                    PastStamping.stamp(past, found.start(), found.view());
                    found.view().clue().ifPresent(clue -> Clues.grant(player, clue));
                }
            }
        }
        if (used % SYNC_INTERVAL == 0) {
            ResourceLocation id = era(stack);
            if (id != null) {
                // An era once stood in: the same ground, as it was then.
                Era era = Eras.get(serverLevel.registryAccess(), id);
                ServerLevel there = era == null ? null : player.server.getLevel(era.dimension());
                if (there != null) {
                    LensSync.tick(player, there);
                }
            } else if (depth(stack).reaches(LensDepth.DECADES)) {
                LensSync.tick(player);
            }
        }
        scanTick(player, depth(stack));
        obeliskTick(player);
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
            EDGE_READING.remove(player.getUUID());
        }
        entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0f, 0.7f);
    }

    // ---------------------------------------------------------------- Time

    public static int timeAvailable(Player player, ItemStack lens) {
        if (EternalHourglassItem.carried(player)) {
            return Integer.MAX_VALUE;
        }
        EssenceContents own = EssenceItems.get(lens);
        return (own == null ? 0 : own.amount()) + Powers.get(player).pool(Aspect.TIME);
    }

    /** Burns Time: the lens's own store first, then the wearer's pool. Free with an Eternal Hourglass. */
    public static boolean pay(Player player, ItemStack lens, int cost) {
        if (EternalHourglassItem.carried(player)) {
            return true;
        }
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

    // ---------------------------------------------------------------- obelisks

    /**
     * An obelisk in sight through the Lens is noted. Standing at the edge of its event horizon and holding
     * the gaze on it for {@value #INSCRIPTION_TICKS} ticks, the older inscription can be read. Returns true then.
     */
    public static boolean obeliskTick(ServerPlayer player) {
        List<BlockPos> obelisks = Obelisks.near(player.level(), player.getEyePosition(), OBELISK_SIGHT);
        if (obelisks.isEmpty()) {
            EDGE_READING.remove(player.getUUID());
            return false;
        }
        Clues.grant(player, Clues.NAIL_IN_TIME);
        for (BlockPos capstone : obelisks) {
            Vec3 heart = Obelisks.heart(capstone);
            double distance = heart.distanceTo(player.getEyePosition());
            Vec3 toHeart = heart.subtract(player.getEyePosition()).normalize();
            boolean atEdge = distance >= Obelisks.HORIZON && distance <= Obelisks.HORIZON + Obelisks.EDGE_WIDTH;
            // Looking at the shaft: within about 20 degrees of its heart.
            if (atEdge && player.getViewVector(1.0f).dot(toHeart) > 0.94) {
                int ticks = EDGE_READING.merge(player.getUUID(), 1, Integer::sum);
                if (ticks >= INSCRIPTION_TICKS) {
                    EDGE_READING.remove(player.getUUID());
                    Clues.grant(player, Clues.OLDER_INSCRIPTION);
                    return true;
                }
                return false;
            }
        }
        EDGE_READING.remove(player.getUUID());
        return false;
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
        // The pen scratches the lines into the journal as the Lens makes them out.
        player.level().playSound(null, player.blockPosition(), ModSounds.LENS_SCRIBBLE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
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


    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component setting = context.level() == null ? depth(stack).displayName() : setting(stack, context.level());
        tooltip.add(Component.translatable("tooltip.chronogenesis.lens.depth", setting).withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.chronogenesis.lens.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
