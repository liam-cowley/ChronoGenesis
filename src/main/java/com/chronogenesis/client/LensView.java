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
import com.chronogenesis.lens.Obelisks;
import com.chronogenesis.lens.TemporalLensItem;
import com.chronogenesis.network.PastViewPayload;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * What the Temporal Lens shows. A round lens in the middle of the screen looks into the past; around it, the
 * present goes on, dimmed. Through the lens the past is not laid over the present: it is what the world
 * looks like.
 * <ol>
 *   <li>The past is rebuilt as world geometry ({@link PastMesh}): each structure's area tessellated through a
 *       {@link PastWorld}, where the past's blocks stand and what is newer is air. Built when the server sends
 *       a view, kept on the GPU.</li>
 *   <li>Each frame, during the world render, the frame (colour, depth) is copied into the {@code past} target
 *       of the {@code temporal_lens} post chain. A stencil pass marks every pixel whose visible surface lies
 *       inside one of those areas; those pixels are cleared, and the past geometry is drawn there, depth
 *       tested. Things in front of an area (a tree before the ruin) stay; inside it, only the past remains.
 *       What the lens hides is still there: rubble still blocks, a floor seen through it may be a hole.</li>
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
    /** Obelisk distortion: full within this distance of the heart, gone by the next. */
    private static final double OBELISK_FULL = 16.0;
    private static final double OBELISK_FADE = 64.0;
    private static final int OBELISK_RESCAN_TICKS = 10;

    @Nullable
    private static PastViewPayload view;
    @Nullable
    private static PastMesh mesh;
    private static boolean meshDirty;

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
        view = payload.boxes().isEmpty() ? null : payload;
        meshDirty = true;
    }

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
            meshDirty = true;
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
        if (!looking()) {
            loggedObelisk = false;
            if (mesh != null) {
                mesh.close();
                mesh = null;
            }
            return;
        }
        PostChain effect = chain(minecraft);
        if (effect == null || minecraft.level == null) {
            return;
        }
        if (meshDirty) {
            meshDirty = false;
            if (mesh != null) {
                mesh.close();
            }
            mesh = view == null ? null
                    : PastMesh.build(minecraft.level, view.positions(), view.states(), view.erased(), view.boxes());
        }

        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget past = effect.getTempTarget("past");
        // Both need stencil bits (and so the same depth format, or the depth copy fails).
        main.enableStencil();
        past.enableStencil();
        copyFrame(main, past);
        past.bindWrite(false);

        if (mesh != null) {
            Vec3 camera = event.getCamera().getPosition();
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            markPastVolumes(mesh.volumes(), camera);
            clearMarked();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            mesh.draw(event.getModelViewMatrix(), event.getProjectionMatrix(), camera);
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
     * Stencil marks every pixel whose visible surface lies inside one of the past's volumes. From outside a box:
     * its front faces count up and its back faces count down where they are in front of the surface, leaving 1
     * only where the surface is between them. From inside: its back faces mark wherever the surface is nearer.
     */
    private static void markPastVolumes(List<AABB> volumes, Vec3 camera) {
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        GL11.glStencilMask(0xFF);
        for (AABB volume : volumes) {
            AABB box = volume.inflate(0.01);
            if (box.inflate(0.3).contains(camera)) {
                GL11.glCullFace(GL11.GL_FRONT);
                RenderSystem.depthFunc(GL11.GL_GEQUAL);
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                drawBox(box, camera);
            } else {
                GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                GL11.glCullFace(GL11.GL_BACK);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INCR);
                drawBox(box, camera);
                GL11.glCullFace(GL11.GL_FRONT);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_DECR);
                drawBox(box, camera);
            }
        }
        GL11.glCullFace(GL11.GL_BACK);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
    }

    /** A box's six faces, wound counter-clockwise seen from outside, relative to the camera. */
    private static void drawBox(AABB box, Vec3 camera) {
        float x0 = (float) (box.minX - camera.x);
        float y0 = (float) (box.minY - camera.y);
        float z0 = (float) (box.minZ - camera.z);
        float x1 = (float) (box.maxX - camera.x);
        float y1 = (float) (box.maxY - camera.y);
        float z1 = (float) (box.maxZ - camera.z);
        float[][] faces = {
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1},   // down
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},   // up
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0},   // north
                {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},   // south
                {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0},   // west
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1},   // east
        };
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (float[] face : faces) {
            for (int i = 0; i < 12; i += 3) {
                buffer.addVertex(face[i], face[i + 1], face[i + 2]);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    /**
     * Where the stencil is marked, the present is wiped: colour to the fog's (what lies beyond the past's area
     * fades into haze), depth to the far plane, so the past geometry drawn next is all that stands there.
     */
    private static void clearMarked() {
        float[] fog = RenderSystem.getShaderFogColor();
        GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
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
