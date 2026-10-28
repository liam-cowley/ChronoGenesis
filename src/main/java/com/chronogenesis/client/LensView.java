package com.chronogenesis.client;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.Obelisks;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.network.PastViewPayload;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * What the Temporal Lens shows. A round lens in the middle of the screen looks into the past; around it, the
 * present goes on, dimmed.
 * <ol>
 *   <li>During the world render, the frame (colour and depth) is copied into the {@code past} target of the
 *       {@code temporal_lens} post chain. In that copy, every present block that was not there in the past is
 *       erased (its depth pushed to the far plane, so nothing it hid stays hidden), and then the past's own
 *       blocks are drawn over it, solid, overriding whatever stands there now. A floor seen through the lens
 *       may be a hole in the present, and rubble the lens hides is still there to trip over.</li>
 *   <li>During HUD rendering (where vanilla runs its own screen blur), one shader pass composites the two:
 *       the past inside the lens in sepia, the present outside, a brass rim between. Near an obelisk the same
 *       pass bends everything into an event horizon, strongest through the glass.</li>
 * </ol>
 * If the shader cannot be loaded, the Lens just shows the present and says so in the log, once.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class LensView {
    private static final ResourceLocation SHADER = ChronoGenesis.id("shaders/post/temporal_lens.json");
    /** The lens's radius, as a fraction of the screen's height. */
    private static final float LENS_RADIUS = 0.36f;
    private static final float ZOOM = 0.6f;
    /** Past blocks are drawn this much larger, so they win over the present block in the same place. */
    private static final float OVERRIDE_SCALE = 1.003f;
    /** Obelisk distortion: full within this distance of the heart, gone by the next. */
    private static final double OBELISK_FULL = 16.0;
    private static final double OBELISK_FADE = 64.0;
    private static final int OBELISK_RESCAN_TICKS = 10;

    private static List<BlockPos> positions = List.of();
    private static List<BlockState> states = List.of();
    private static List<BlockPos> erased = List.of();

    @Nullable
    private static PostChain chain;
    private static boolean broken;
    private static int width;
    private static int height;
    /** Whether this frame's past has been drawn, so the HUD pass may composite it. */
    private static boolean pastReady;

    // The nearest obelisk on screen this frame (heart in screen UV, horizon radius in screen heights).
    private static float obeliskX;
    private static float obeliskY;
    private static float obeliskRadius;
    private static float obeliskStrength;
    private static List<BlockPos> obelisks = List.of();
    private static long lastObeliskScan = Long.MIN_VALUE;
    private static boolean loggedObelisk;

    private LensView() {}

    public static void receive(PastViewPayload payload) {
        positions = payload.positions();
        states = payload.states();
        erased = payload.erased();
    }

    public static boolean looking() {
        var player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem().getItem() instanceof TemporalLensItem;
    }

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        // Shaders are resources: drop the chain on reload and build it again on next use.
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            if (chain != null) {
                chain.close();
                chain = null;
            }
            broken = false;
        });
    }

    /** The Lens narrows the view, like a spyglass. */
    @SubscribeEvent
    static void onFov(ComputeFovModifierEvent event) {
        if (looking()) {
            event.setNewFovModifier(event.getNewFovModifier() * ZOOM);
        }
    }

    // ---------------------------------------------------------------- 1. the past, drawn into its own copy of the frame

    /**
     * Runs at the end of the level's own rendering ({@code AFTER_WEATHER}), while the camera's matrices are still
     * applied; at {@code AFTER_LEVEL} they are not (and there is no pose stack).
     */
    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        pastReady = false;
        if (!looking()) {
            loggedObelisk = false;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        PostChain effect = chain(minecraft);
        if (effect == null || minecraft.level == null) {
            return;
        }
        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget past = effect.getTempTarget("past");
        copyFrame(main, past);
        past.bindWrite(false);

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        eraseNewerBlocks(poses, camera);
        drawPast(minecraft, poses, camera);

        main.bindWrite(false);
        locateObelisk(minecraft, event, camera);
        pastReady = true;
    }

    /** Colour and depth of the present into the past target, which the past is then drawn over. */
    private static void copyFrame(RenderTarget from, RenderTarget to) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, from.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, from.width, from.height, 0, 0, to.width, to.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        to.copyDepthFrom(from);
    }

    /**
     * Blocks that stand now but did not then: their cube is drawn with its depth forced to the far plane, so in
     * the past target they no longer hide anything, and the past's own floor and walls behind them show through.
     */
    private static void eraseNewerBlocks(PoseStack poses, Vec3 camera) {
        if (erased.isEmpty()) {
            return;
        }
        float[] fog = RenderSystem.getShaderFogColor();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.depthMask(true);
        GL11.glDepthRange(1.0, 1.0);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose = poses.last().pose();
        for (BlockPos pos : erased) {
            float x0 = (float) (pos.getX() - camera.x) - 0.002f;
            float y0 = (float) (pos.getY() - camera.y) - 0.002f;
            float z0 = (float) (pos.getZ() - camera.z) - 0.002f;
            cube(buffer, pose, x0, y0, z0, 1.004f, fog[0], fog[1], fog[2]);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        GL11.glDepthRange(0.0, 1.0);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
    }

    private static void cube(BufferBuilder b, Matrix4f m, float x, float y, float z, float s, float r, float g, float bl) {
        float x1 = x + s;
        float y1 = y + s;
        float z1 = z + s;
        float[][] faces = {
                {x, y, z, x1, y, z, x1, y, z1, x, y, z1},       // down
                {x, y1, z, x, y1, z1, x1, y1, z1, x1, y1, z},   // up
                {x, y, z, x, y1, z, x1, y1, z, x1, y, z},       // north
                {x, y, z1, x1, y, z1, x1, y1, z1, x, y1, z1},   // south
                {x, y, z, x, y, z1, x, y1, z1, x, y1, z},       // west
                {x1, y, z, x1, y1, z, x1, y1, z1, x1, y, z1},   // east
        };
        for (float[] face : faces) {
            for (int i = 0; i < 12; i += 3) {
                b.addVertex(m, face[i], face[i + 1], face[i + 2]).setColor(r, g, bl, 1.0f);
            }
        }
    }

    /** The past's blocks, solid and a touch larger than a block, so they override whatever is there now. */
    private static void drawPast(Minecraft minecraft, PoseStack poses, Vec3 camera) {
        if (positions.isEmpty()) {
            return;
        }
        BlockRenderDispatcher blocks = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        var consumer = buffers.getBuffer(RenderType.cutout());
        for (int i = 0; i < positions.size(); i++) {
            BlockState state = states.get(i);
            if (state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            BlockPos pos = positions.get(i);
            poses.pushPose();
            poses.translate(pos.getX() - camera.x + 0.5, pos.getY() - camera.y + 0.5, pos.getZ() - camera.z + 0.5);
            poses.scale(OVERRIDE_SCALE, OVERRIDE_SCALE, OVERRIDE_SCALE);
            poses.translate(-0.5, -0.5, -0.5);
            blocks.getModelRenderer().renderModel(poses.last(), consumer, state, blocks.getBlockModel(state), 1.0f, 1.0f, 1.0f,
                    LevelRenderer.getLightColor(minecraft.level, pos), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
            poses.popPose();
        }
        buffers.endBatch(RenderType.cutout());
    }

    /** Where the nearest obelisk's heart is on screen, how big its horizon looks, and how strongly it pulls. */
    private static void locateObelisk(Minecraft minecraft, RenderLevelStageEvent event, Vec3 camera) {
        long now = minecraft.level.getGameTime();
        if (now - lastObeliskScan >= OBELISK_RESCAN_TICKS || now < lastObeliskScan) {
            obelisks = Obelisks.near(minecraft.level, camera, OBELISK_FADE);
            lastObeliskScan = now;
        }
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (BlockPos capstone : obelisks) {
            double distance = Obelisks.heart(capstone).distanceTo(camera);
            if (distance < best) {
                best = distance;
                nearest = capstone;
            }
        }
        obeliskStrength = nearest == null ? 0.0f
                : (float) Mth.clamp(1.0 - (best - OBELISK_FULL) / (OBELISK_FADE - OBELISK_FULL), 0.0, 1.0);
        if (obeliskStrength <= 0.0f) {
            return;
        }
        Vec3 heart = Obelisks.heart(nearest).subtract(camera);
        Matrix4f view = event.getModelViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
        Vector4f clip = projection.transform(view.transform(new Vector4f((float) heart.x, (float) heart.y, (float) heart.z, 1.0f)));
        if (clip.w > 0.05f) {
            obeliskX = clip.x / clip.w * 0.5f + 0.5f;
            obeliskY = clip.y / clip.w * 0.5f + 0.5f;
            obeliskRadius = Mth.clamp((float) (0.5 * projection.m11() * Obelisks.HORIZON / clip.w), 0.002f, 2.0f);
        } else {
            // Behind you: the horizon is off screen, but the view still tears.
            obeliskX = -2.0f;
            obeliskY = -2.0f;
            obeliskRadius = 0.2f;
            obeliskStrength *= 0.35f;
        }
        if (!loggedObelisk) {
            loggedObelisk = true;
            ChronoGenesis.LOGGER.info("Temporal Lens: obelisk at {} in view, {} blocks away (strength {}, screen {} {})",
                    nearest, (int) best, obeliskStrength, obeliskX, obeliskY);
        }
    }

    // ---------------------------------------------------------------- 2. the lens itself, composited over the HUD layer

    /** A HUD layer, drawn first: composites the past (inside the lens) over the present. */
    public static void composite(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!pastReady || chain == null || !looking()) {
            return;
        }
        pastReady = false;
        Minecraft minecraft = Minecraft.getInstance();
        graphics.flush();
        chain.setUniform("LensRadius", LENS_RADIUS);
        chain.setUniform("AspectRatio", (float) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight()));
        long gameTime = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        chain.setUniform("Seconds", (gameTime % 24000 + deltaTracker.getGameTimeDeltaPartialTick(false)) / 20.0f);
        chain.setUniform("CenterX", obeliskX);
        chain.setUniform("CenterY", obeliskY);
        chain.setUniform("Radius", obeliskRadius);
        chain.setUniform("Strength", obeliskStrength);
        // As vanilla does for its screen blur: no depth test, process, then back to the main target.
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        chain.process(deltaTracker.getGameTimeDeltaTicks());
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    @Nullable
    private static PostChain chain(Minecraft minecraft) {
        if (broken) {
            return null;
        }
        int w = minecraft.getWindow().getWidth();
        int h = minecraft.getWindow().getHeight();
        if (chain == null) {
            try {
                chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), SHADER);
                chain.resize(w, h);
                width = w;
                height = h;
                ChronoGenesis.LOGGER.info("Temporal Lens shader loaded");
            } catch (IOException | RuntimeException e) {
                ChronoGenesis.LOGGER.error("Could not load the Temporal Lens shader; the Lens will show only the present", e);
                broken = true;
                chain = null;
                return null;
            }
        } else if (w != width || h != height) {
            chain.resize(w, h);
            width = w;
            height = h;
        }
        return chain;
    }
}
