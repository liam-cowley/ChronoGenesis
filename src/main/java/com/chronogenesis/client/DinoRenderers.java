package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Raptor;
import com.chronogenesis.entity.Titanosaur;
import com.chronogenesis.player.Power;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModEntities;
import com.chronogenesis.temple.SealBlock;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers and model layers of the Jurassic Age's creatures, and the client's half of the temples. */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class DinoRenderers {
    private DinoRenderers() {}

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RaptorModel.LAYER, RaptorModel::createLayer);
        event.registerLayerDefinition(TitanosaurModel.LAYER, TitanosaurModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RAPTOR.get(), RaptorRenderer::new);
        event.registerEntityRenderer(ModEntities.TITANOSAUR.get(), TitanosaurRenderer::new);
        event.registerEntityRenderer(ModEntities.RIDER_SEAT.get(), NoopRenderer::new);
    }

    /** Veiled seals show themselves to Glowing Sight. */
    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        SealBlock.veiledSight = () -> {
            var player = Minecraft.getInstance().player;
            return player != null && player.getData(ModAttachments.PLAYER_ESSENCE).isOn(Power.GLOWING_SIGHT);
        };
    }

    static class RaptorRenderer extends MobRenderer<Raptor, RaptorModel> {
        private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/raptor.png");

        RaptorRenderer(EntityRendererProvider.Context context) {
            super(context, new RaptorModel(context.bakeLayer(RaptorModel.LAYER)), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(Raptor entity) {
            return TEXTURE;
        }
    }

    static class TitanosaurRenderer extends MobRenderer<Titanosaur, TitanosaurModel> {
        private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/titanosaur.png");
        private static final float SCALE = 2.4f;

        TitanosaurRenderer(EntityRendererProvider.Context context) {
            super(context, new TitanosaurModel(context.bakeLayer(TitanosaurModel.LAYER)), 2.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(Titanosaur entity) {
            return TEXTURE;
        }

        @Override
        protected void scale(Titanosaur entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(SCALE, SCALE, SCALE);
        }
    }
}
