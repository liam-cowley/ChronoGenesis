package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.DinoCargo;
import com.chronogenesis.entity.Raptor;
import com.chronogenesis.entity.Titanosaur;
import com.chronogenesis.player.Power;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.registry.ModEntities;
import com.chronogenesis.temple.SealBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

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

        @Override
        protected void scale(Raptor entity, PoseStack poseStack, float partialTick) {
            float scale = entity.getAgeScale();
            poseStack.scale(scale, scale, scale);
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
            float scale = SCALE * entity.getAgeScale();
            poseStack.scale(scale, scale, scale);
        }

        /** Its cargo, drawn block by block on its back, turning with it (and cracking where it is being broken). */
        @Override
        public void render(Titanosaur entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
            super.render(entity, yaw, partialTick, poseStack, buffers, light);
            DinoCargo cargo = entity.cargo();
            if (!cargo.carrying()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            BlockRenderDispatcher blocks = mc.getBlockRenderer();
            float bodyYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            Vec3 at = entity.getPosition(partialTick);
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-cargo.turn(bodyYaw)));
            for (DinoCargo.Carried block : cargo.carried()) {
                Vec3 local = cargo.anchor().add(Vec3.atLowerCornerOf(block.offset()));
                BlockPos lightAt = BlockPos.containing(cargo.toWorld(Vec3.atCenterOf(block.offset()), at, bodyYaw));
                poseStack.pushPose();
                poseStack.translate(local.x, local.y, local.z);
                blocks.renderSingleBlock(block.state(), poseStack, buffers, LevelRenderer.getLightColor(entity.level(), lightAt),
                        OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                int cracks = CargoControls.cracks(entity, block.offset());
                if (cracks >= 0 && mc.level != null) {
                    VertexConsumer crumbling = new SheetedDecalTextureGenerator(
                            buffers.getBuffer(ModelBakery.DESTROY_TYPES.get(cracks)), poseStack.last(), 1.0f);
                    blocks.renderBreakingTexture(block.state(), lightAt, mc.level, poseStack, crumbling);
                }
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }
}
