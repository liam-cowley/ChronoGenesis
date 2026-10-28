package com.chronogenesis.temple;

import com.chronogenesis.ChronoGenesis;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * The temporal barrier around a running Temporal Core: inside it, time is held so still that nothing can be placed,
 * broken, poured or blown up. The way in is the temple's seals, each answering a different gift of the glands; or
 * its doors, stairs and crawlspaces, which are already there. Creative players are not held.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class TempleBarrier {
    private TempleBarrier() {}

    /** Whether a running core holds this position still. */
    public static boolean holds(Level level, BlockPos pos) {
        return !TemporalCoreBlockEntity.LOADED.isEmpty() && TemporalCoreBlockEntity.holding(level, pos) != null;
    }

    private static boolean blocks(Player player, Level level, BlockPos pos) {
        if (player != null && player.isCreative()) {
            return false;
        }
        if (!holds(level, pos)) {
            return false;
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.temple.barrier").withStyle(ChatFormatting.DARK_AQUA), true);
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
        }
        return true;
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && blocks(event.getPlayer(), level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Player player = event.getEntity() instanceof Player p ? p : null;
        if (event.getLevel() instanceof Level level && (player == null || !player.isCreative()) && holds(level, event.getPos())) {
            blocks(player, level, event.getPos());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onExplode(ExplosionEvent.Detonate event) {
        if (!TemporalCoreBlockEntity.LOADED.isEmpty()) {
            event.getAffectedBlocks().removeIf(pos -> holds(event.getLevel(), pos));
        }
    }

    /** Buckets pour without "placing" a block: they are held too. */
    @SubscribeEvent
    static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof BucketItem
                && blocks(event.getEntity(), event.getLevel(), event.getPos().relative(event.getFace() == null
                        ? net.minecraft.core.Direction.UP : event.getFace()))) {
            event.setCanceled(true);
        }
    }
}
