package com.chronogenesis.client;

import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.player.PlayerGraft;
import com.chronogenesis.player.PlayerGrafts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The Unbegotten's grafts on a player's body ({@link PlayerGrafts}), drawn from the organ parts ({@link OrganParts})
 * in the model's own space, so they move with the body: a third eye in the forehead (blinking, watching), tendrils
 * writhing from the spine, a tentacle wound round the right arm with its tip curling past the hand, plates of hide over
 * the chest and shoulders.
 */
public class PlayerGraftLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int FLESH = 0xFF6E2E7E;
    private static final int TIP = 0x6AE09A;
    private static final int HIDE = 0xFF3A2440;

    public PlayerGraftLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        List<PlayerGraft> grafts = PlayerGrafts.get(player);
        if (grafts.isEmpty() || player.isInvisible()) {
            return;
        }
        VertexConsumer buffer = OrganParts.buffer(buffers);
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        long seed = player.getUUID().getLeastSignificantBits();
        if (grafts.contains(PlayerGraft.UNBLINKING_EYE)) {
            pose.pushPose();
            model.head.translateAndRotate(pose);
            OrganParts.draw(OrganParts.FLESH, pose, buffer, light, 0, -5.6f, -4.05f, 0.42f, 0.36f, 0.08f, FLESH);
            pose.translate(0, -5.6f / 16.0f, -4.4f / 16.0f);
            pose.scale(0.62f, 0.62f * OrganParts.blink(ageInTicks, seed), 0.62f);
            OrganParts.EYE.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, OrganParts.WHITE);
            pose.popPose();
        }
        if (grafts.contains(PlayerGraft.DORSAL_TENDRILS)) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            float[][] roots = {{-1.6f, 2.5f}, {1.6f, 2.5f}, {-1.2f, 6.0f}, {1.2f, 6.0f}};
            for (int i = 0; i < roots.length; i++) {
                pose.pushPose();
                pose.translate(roots[i][0] / 16.0f, roots[i][1] / 16.0f, 2.2f / 16.0f);
                // Out of the back, up and away from the spine.
                pose.mulPose(Axis.XP.rotationDegrees(125 - i * 6));
                pose.mulPose(Axis.ZP.rotationDegrees(Math.signum(roots[i][0]) * (25 + 10 * Mth.sin(ageInTicks * 0.05f + i))));
                OrganParts.tentacle(pose, buffer, light, 6, 0.75f, ageInTicks, i * 1.9f, 0.25f, OrganParts.mix(FLESH, TIP, 0.15f * i));
                pose.popPose();
            }
            pose.popPose();
        }
        if (grafts.contains(PlayerGraft.GRASPING_TENTACLE)) {
            pose.pushPose();
            model.rightArm.translateAndRotate(pose);
            boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
            float cx = slim ? -0.5f : -1.0f;
            float r = slim ? 2.1f : 2.6f;
            for (int k = 0; k < 14; k++) {
                float a = k * 0.85f + ageInTicks * 0.02f;
                OrganParts.draw(OrganParts.SEGMENT, pose, buffer, light, cx + Mth.cos(a) * r, -1.5f + k * 0.8f, Mth.sin(a) * r * 0.9f,
                        0.75f, 0.35f, 0.75f, OrganParts.mix(FLESH, TIP, k / 20.0f));
            }
            pose.translate(cx / 16.0f, 10.5f / 16.0f, 0);
            pose.mulPose(Axis.XP.rotationDegrees(15));
            OrganParts.tentacle(pose, buffer, light, 5, 0.7f, ageInTicks, 0.3f, 0.3f, OrganParts.mix(FLESH, TIP, 0.6f));
            pose.popPose();
        }
        if (grafts.contains(PlayerGraft.UNBEGOTTEN_HIDE)) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            for (int k = 0; k < 4; k++) {
                float y = 1.5f + k * 2.4f;
                OrganParts.draw(OrganParts.FLESH, pose, buffer, light, 0, y, -2.35f, 1.05f - 0.06f * k, 0.28f, 0.12f, HIDE);
                OrganParts.draw(OrganParts.FLESH, pose, buffer, light, 0, y, 2.35f, 1.05f - 0.06f * k, 0.28f, 0.12f, HIDE);
            }
            pose.popPose();
            for (boolean right : new boolean[] {true, false}) {
                pose.pushPose();
                (right ? model.rightArm : model.leftArm).translateAndRotate(pose);
                OrganParts.draw(OrganParts.FLESH, pose, buffer, light, right ? -1.0f : 1.0f, -1.8f, 0, 0.62f, 0.32f, 0.62f, HIDE);
                OrganParts.draw(OrganParts.SHARD, pose, buffer, light, right ? -1.5f : 1.5f, -3.4f, 0, 0.6f, OrganParts.mix(HIDE, TIP, 0.2f));
                pose.popPose();
            }
        }
    }

    @EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
    static final class Registration {
        private Registration() {}

        @SubscribeEvent
        static void layers(EntityRenderersEvent.AddLayers event) {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                    renderer.addLayer(new PlayerGraftLayer(renderer));
                }
            }
        }
    }
}
