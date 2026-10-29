package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Unbegotten;
import com.chronogenesis.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The Unbegotten at twice its model's size, its eye, tentacle tips and eye-stalks glowing in the dark. */
public class UnbegottenRenderer extends MobRenderer<Unbegotten, UnbegottenModel> {
    private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/unbegotten.png");
    private static final RenderType EYES = RenderType.eyes(ChronoGenesis.id("textures/entity/unbegotten_eyes.png"));

    public UnbegottenRenderer(EntityRendererProvider.Context context) {
        super(context, new UnbegottenModel(context.bakeLayer(UnbegottenModel.LAYER)), 3.0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(Unbegotten entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2.0f, 2.0f, 2.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Unbegotten entity) {
        return TEXTURE;
    }

    @EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
    static final class Registration {
        private Registration() {}

        @SubscribeEvent
        static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(UnbegottenModel.LAYER, UnbegottenModel::createLayer);
        }

        @SubscribeEvent
        static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.UNBEGOTTEN.get(), UnbegottenRenderer::new);
        }
    }
}
