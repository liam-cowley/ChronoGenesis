package com.chronogenesis.essence;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.item.ChronoSyringeItem;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Routes syringe clicks on creatures before the creature handles the click itself. */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class SyringeEvents {
    private SyringeEvents() {}

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ChronoSyringeItem syringe) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        InteractionResult result = syringe.drawFrom(stack, event.getEntity(), target, event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
