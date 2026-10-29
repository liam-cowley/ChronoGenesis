package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The machines' animated parts: organism cores, grafts, and the Chronocortex's brain. */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class MachineRenderers {
    private MachineRenderers() {}

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ORGANISM_CORE.get(), OrganismCoreRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GRAFT.get(), GraftRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHRONOCORTEX.get(), CortexBrainRenderer::new);
    }
}
