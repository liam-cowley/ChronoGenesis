package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the Essence Manipulator as its 3D gauntlet ({@link GauntletModel}) wherever the item is drawn: in hand, on
 * the ground, in frames and in the inventory. Its tendrils move with the game's time (and the partial tick).
 */
public class GauntletRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/item/essence_manipulator_gauntlet.png");
    /** The gauntlet is about 19 model pixels tall: a little smaller fits the item's block. */
    private static final float SCALE = 0.8f;

    private GauntletModel model;

    public GauntletRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        if (model == null) {
            model = new GauntletModel();
        }
        Minecraft minecraft = Minecraft.getInstance();
        float time = minecraft.level == null ? 0
                : minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        model.animate(time);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(SCALE, -SCALE, -SCALE);
        model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        poseStack.popPose();
    }
}
