package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.client.IceAgeModels.MammothModel;
import com.chronogenesis.client.IceAgeModels.RhinoModel;
import com.chronogenesis.client.IceAgeModels.SmilodonModel;
import com.chronogenesis.entity.Mammoth;
import com.chronogenesis.entity.PrecursorGolem;
import com.chronogenesis.entity.Smilodon;
import com.chronogenesis.entity.WoollyRhino;
import com.chronogenesis.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Renderers of the Ice Age's creatures, and the Precursors' golems. */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class IceAgeRenderers {
    private IceAgeRenderers() {}

    @SubscribeEvent
    static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MammothModel.LAYER, MammothModel::createLayer);
        event.registerLayerDefinition(SmilodonModel.LAYER, SmilodonModel::createLayer);
        event.registerLayerDefinition(RhinoModel.LAYER, RhinoModel::createLayer);
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MAMMOTH.get(), context -> new Simple<Mammoth, MammothModel>(context,
                new MammothModel(context.bakeLayer(MammothModel.LAYER)), 2.0f, "mammoth", 1.6f));
        event.registerEntityRenderer(ModEntities.SMILODON.get(), context -> new Simple<Smilodon, SmilodonModel>(context,
                new SmilodonModel(context.bakeLayer(SmilodonModel.LAYER)), 0.7f, "smilodon", 1.0f));
        event.registerEntityRenderer(ModEntities.WOOLLY_RHINO.get(), context -> new Simple<WoollyRhino, RhinoModel>(context,
                new RhinoModel(context.bakeLayer(RhinoModel.LAYER)), 1.1f, "woolly_rhino", 1.0f));
        event.registerEntityRenderer(ModEntities.PRECURSOR_GOLEM.get(), GolemRenderer::new);
    }

    static class Simple<T extends net.minecraft.world.entity.Mob, M extends net.minecraft.client.model.EntityModel<T>> extends MobRenderer<T, M> {
        private final ResourceLocation texture;
        private final float scale;

        Simple(EntityRendererProvider.Context context, M model, float shadow, String name, float scale) {
            super(context, model, shadow);
            this.texture = ChronoGenesis.id("textures/entity/" + name + ".png");
            this.scale = scale;
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }

        @Override
        protected void scale(T entity, PoseStack poseStack, float partialTick) {
            float s = scale * entity.getAgeScale();
            poseStack.scale(s, s, s);
        }
    }

    /** An iron golem as the Hearth kept them: bright iron, no vines, no flowers, no cracks however hurt. */
    static class GolemRenderer extends MobRenderer<PrecursorGolem, IronGolemModel<PrecursorGolem>> {
        private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/precursor_golem.png");

        GolemRenderer(EntityRendererProvider.Context context) {
            super(context, new IronGolemModel<>(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7f);
        }

        @Override
        public ResourceLocation getTextureLocation(PrecursorGolem entity) {
            return TEXTURE;
        }

        @Override
        protected void setupRotations(PrecursorGolem entity, PoseStack poseStack, float bob, float yRot, float partialTick, float scale) {
            super.setupRotations(entity, poseStack, bob, yRot, partialTick, scale);
            if (entity.walkAnimation.speed() >= 0.01) {
                float pos = entity.walkAnimation.position(partialTick) + 6.0f;
                float sway = (Math.abs(pos % 13.0f - 6.5f) - 3.25f) / 3.25f;
                poseStack.mulPose(Axis.ZP.rotationDegrees(6.5f * sway));
            }
        }
    }
}
