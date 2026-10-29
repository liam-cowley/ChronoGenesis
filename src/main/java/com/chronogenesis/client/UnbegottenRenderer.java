package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Unbegotten;
import com.chronogenesis.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The Unbegotten's body at four and a half times its model's size, its eye, tentacle tips and eye-stalks glowing in the
 * dark, the folds of the Chronocortex it was heaped over its crown. Its limbs: {@link UnbegottenLimbRenderer}.
 */
public class UnbegottenRenderer extends MobRenderer<Unbegotten, UnbegottenModel> {
    private static final float SCALE = 4.5f;
    private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/unbegotten.png");
    private static final RenderType EYES = RenderType.eyes(ChronoGenesis.id("textures/entity/unbegotten_eyes.png"));

    public UnbegottenRenderer(EntityRendererProvider.Context context) {
        super(context, new UnbegottenModel(context.bakeLayer(UnbegottenModel.LAYER)), 0.0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(Unbegotten entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    /** Over its crown, the folds of the brain it was, heaped up and gone green. */
    @Override
    public void render(Unbegotten entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partialTick, pose, buffers, light);
        float time = entity.tickCount + partialTick;
        com.mojang.blaze3d.vertex.VertexConsumer buffer = OrganParts.buffer(buffers);
        float top = (1.5f + 0.75f + 0.08f * net.minecraft.util.Mth.sin(time * 0.05f)) * SCALE * 16;
        for (int i = 0; i < 14; i++) {
            float a = i * 2.39996f;
            float ring = net.minecraft.util.Mth.sqrt(i / 14.0f);
            float r = 30 * ring;
            float s = (3.2f - 1.2f * ring) * (1 + 0.04f * net.minecraft.util.Mth.sin(time * 0.07f + i));
            OrganParts.draw(OrganParts.GYRI, pose, buffer, light, net.minecraft.util.Mth.cos(a) * r, top + 6 - 8 * ring,
                    net.minecraft.util.Mth.sin(a) * r, s, OrganParts.mix(0x9A5AB4, 0x6AE09A, ring));
        }
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
            event.registerEntityRenderer(ModEntities.UNBEGOTTEN_LIMB.get(), UnbegottenLimbRenderer::new);
        }
    }
}
