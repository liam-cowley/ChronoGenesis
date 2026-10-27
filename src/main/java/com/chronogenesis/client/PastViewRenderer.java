package com.chronogenesis.client;

import java.util.List;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.network.PastViewPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws what the Temporal Lens shows: the past blocks the server sent, as translucent sepia ghosts over the
 * present, under a hazy vignette, zoomed in like a spyglass. Only while the Lens is in use.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class PastViewRenderer {
    private static final float GHOST_ALPHA = 0.55f;
    /** Sepia: the past is faded. */
    private static final float SEPIA_R = 1.0f;
    private static final float SEPIA_G = 0.86f;
    private static final float SEPIA_B = 0.62f;
    private static final float ZOOM = 0.5f;

    private static List<BlockPos> positions = List.of();
    private static List<BlockState> states = List.of();

    private PastViewRenderer() {}

    public static void receive(PastViewPayload payload) {
        positions = payload.positions();
        states = payload.states();
    }

    public static boolean looking() {
        var player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem().getItem() instanceof TemporalLensItem;
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !looking() || positions.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blocks = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer ghost = new GhostConsumer(buffers.getBuffer(RenderType.translucent()));
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        PoseStack poses = event.getPoseStack();
        for (int i = 0; i < positions.size(); i++) {
            BlockState state = states.get(i);
            if (state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            BlockPos pos = positions.get(i);
            poses.pushPose();
            poses.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);
            blocks.getModelRenderer().renderModel(poses.last(), ghost, state, blocks.getBlockModel(state), 1.0f, 1.0f, 1.0f,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
            poses.popPose();
        }
        buffers.endBatch(RenderType.translucent());
    }

    /** The Lens narrows the view, like a spyglass. */
    @SubscribeEvent
    static void onFov(ComputeFovModifierEvent event) {
        if (looking()) {
            event.setNewFovModifier(event.getNewFovModifier() * ZOOM);
        }
    }

    /** A sepia haze with a dark vignette while looking: the past is faded and hard to see. */
    public static void renderHaze(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!looking()) {
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        graphics.fill(0, 0, w, h, 0x38704A20);
        int band = Math.min(w, h) / 6;
        for (int i = 0; i < band; i++) {
            int alpha = (int) (0xB0 * (1.0 - (double) i / band));
            int color = alpha << 24 | 0x100804;
            graphics.fill(0, i, w, i + 1, color);
            graphics.fill(0, h - i - 1, w, h - i, color);
            graphics.fill(i, 0, i + 1, h, color);
            graphics.fill(w - i - 1, 0, w - i, h, color);
        }
    }

    /** Tints every quad sepia and makes it see-through. */
    private record GhostConsumer(VertexConsumer delegate) implements VertexConsumer {
        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue,
                                float alpha, int[] lightmap, int packedOverlay, boolean readAlpha) {
            VertexConsumer.super.putBulkData(pose, quad, brightness, red * SEPIA_R, green * SEPIA_G, blue * SEPIA_B,
                    GHOST_ALPHA, lightmap, packedOverlay, false);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }
}
