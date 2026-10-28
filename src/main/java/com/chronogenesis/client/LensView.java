package com.chronogenesis.client;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.LensDepth;
import com.chronogenesis.lens.Obelisks;
import com.chronogenesis.lens.TemporalLensItem;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * What the Temporal Lens shows. A round lens in the middle of the screen looks into the past; around it, the
 * present goes on, dimmed. Through the lens the past is not laid over the present: it is a world of its own,
 * the past dimension, as the server sends it ({@link PastSections}).
 * <ol>
 *   <li>The past's sections are built into world geometry ({@link PastMesh}) a few per frame, through a
 *       {@link PastWorld}, the way chunk sections are.</li>
 *   <li>Each frame, during the world render, the frame is copied into the {@code past} target of the
 *       {@code temporal_lens} post chain. Everything the present drew there is wiped (to the fog's colour, at the
 *       far plane); the sky is kept. Then the past's geometry is drawn. Inside the lens only the past stands:
 *       a floor seen through it may be a hole now, and what the past lacks may still block the way.</li>
 *   <li>During HUD rendering (where vanilla runs its own screen blur), one shader pass composites the copy
 *       inside the lens (in sepia) over the present, with a brass rim. Near an obelisk the same pass bends
 *       everything into an event horizon, strongest through the glass.</li>
 * </ol>
 * If the shader cannot be loaded, the Lens just shows the present and says so in the log, once.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class LensView {
    private static final ResourceLocation SHADER = ChronoGenesis.id("shaders/post/temporal_lens.json");
    /** The lens's radius, as a fraction of the screen's height. */
    private static final float LENS_RADIUS = 0.36f;
    private static final float ZOOM = 0.6f;
    /** Past sections built into meshes per frame. */
    private static final int BUILDS_PER_FRAME = 4;
    /** Obelisk distortion: full within this distance of the heart, gone by the next. */
    private static final double OBELISK_FULL = 16.0;
    private static final double OBELISK_FADE = 64.0;
    private static final int OBELISK_RESCAN_TICKS = 10;

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

    public static boolean looking() {
        var player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem() && player.getUseItem().getItem() instanceof TemporalLensItem;
    }

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        // Shaders and models are resources: drop what was built from them, and build again on next use.
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            if (chain != null) {
                chain.close();
                chain = null;
            }
            broken = false;
            PastMesh.clear();
            PastSections.positions().forEach(PastSections::markDirty);
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
        Minecraft minecraft = Minecraft.getInstance();
        if (!looking() || minecraft.level == null || minecraft.player == null) {
            loggedObelisk = false;
            return;
        }
        PostChain effect = chain(minecraft);
        if (effect == null) {
            return;
        }
        PastSections.prune(minecraft.player.chunkPosition());
        PastMesh.buildSome(new PastWorld(minecraft.level), BUILDS_PER_FRAME);

        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget past = effect.getTempTarget("past");
        copyFrame(main, past);
        past.bindWrite(false);
        LensDepth depth = TemporalLensItem.depth(minecraft.player.getUseItem());
        if (depth.reaches(LensDepth.DECADES)) {
            wipePresent();
            PastMesh.draw(event.getModelViewMatrix(), event.getProjectionMatrix(), event.getCamera().getPosition());
        }
        main.bindWrite(false);
        locateObelisk(minecraft, event, event.getCamera().getPosition());
        pastReady = true;
    }

    /** Colour and depth of the present into the past target. */
    private static void copyFrame(RenderTarget from, RenderTarget to) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, from.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, from.width, from.height, 0, 0, to.width, to.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        to.copyDepthFrom(from);
    }

    /**
     * Wipes everything the present drew, keeping the sky: a full-screen quad at the far plane that only passes
     * where something nearer stands (depth test GREATER), painting it the fog's colour and pushing its depth to
     * the far plane. What the past does not cover then fades into the haze.
     */
    private static void wipePresent() {
        float[] fog = RenderSystem.getShaderFogColor();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_GREATER);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        GL11.glDepthRange(1.0, 1.0);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(-1, -1, 0).setColor(fog[0], fog[1], fog[2], 1.0f);
        buffer.addVertex(1, -1, 0).setColor(fog[0], fog[1], fog[2], 1.0f);
        buffer.addVertex(1, 1, 0).setColor(fog[0], fog[1], fog[2], 1.0f);
        buffer.addVertex(-1, 1, 0).setColor(fog[0], fog[1], fog[2], 1.0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        GL11.glDepthRange(0.0, 1.0);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
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
