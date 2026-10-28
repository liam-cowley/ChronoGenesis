package com.chronogenesis.client;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.lens.Obelisks;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Seen through the Temporal Lens, an obelisk bends the view like a black hole (core-systems.md section 7).
 * A full-screen post-processing pass ({@code shaders/post/event_horizon.json}) lenses, swirls, smears and
 * glitches the image around the obelisk's position on screen, with a black event horizon at its heart.
 * If the shader cannot be loaded, the effect is simply skipped.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class EventHorizonEffect {
    private static final ResourceLocation SHADER = ChronoGenesis.id("shaders/post/event_horizon.json");
    /** Full effect within this distance of an obelisk... */
    private static final double FULL_RANGE = 16.0;
    /** ...fading out completely by this one. */
    private static final double FADE_RANGE = 64.0;
    private static final int RESCAN_TICKS = 10;

    @Nullable
    private static PostChain chain;
    private static boolean broken;
    private static int width;
    private static int height;
    private static List<BlockPos> obelisks = List.of();
    private static long lastScan = Long.MIN_VALUE;

    private EventHorizonEffect() {}

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        // Shaders are resources: throw the chain away on reload and build it again on next use.
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            close();
            broken = false;
        });
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !PastViewRenderer.looking()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        long now = minecraft.level.getGameTime();
        if (now - lastScan >= RESCAN_TICKS || now < lastScan) {
            obelisks = Obelisks.near(minecraft.level, camera, FADE_RANGE);
            lastScan = now;
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
        if (nearest == null) {
            return;
        }
        float strength = (float) Mth.clamp(1.0 - (best - FULL_RANGE) / (FADE_RANGE - FULL_RANGE), 0.0, 1.0);
        if (strength <= 0.0f) {
            return;
        }
        PostChain effect = chain(minecraft);
        if (effect == null) {
            return;
        }

        // Where the obelisk's heart is on screen, and how big its horizon looks from here.
        Vec3 heart = Obelisks.heart(nearest).subtract(camera);
        Matrix4f view = event.getModelViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
        Vector4f clip = projection.transform(view.transform(new Vector4f((float) heart.x, (float) heart.y, (float) heart.z, 1.0f)));
        float centreX;
        float centreY;
        float radius;
        if (clip.w > 0.05f) {
            centreX = clip.x / clip.w * 0.5f + 0.5f;
            centreY = clip.y / clip.w * 0.5f + 0.5f;
            radius = (float) (0.5 * projection.m11() * Obelisks.HORIZON / clip.w);
        } else {
            // Behind you: the horizon is off screen, but the view still tears.
            centreX = -2.0f;
            centreY = -2.0f;
            radius = 0.2f;
            strength *= 0.35f;
        }

        effect.setUniform("CenterX", centreX);
        effect.setUniform("CenterY", centreY);
        effect.setUniform("Radius", Mth.clamp(radius, 0.002f, 2.0f));
        effect.setUniform("Strength", strength);
        effect.setUniform("Seconds", (now % 24000 + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20.0f);
        effect.setUniform("AspectRatio", (float) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight()));

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        effect.process(event.getPartialTick().getGameTimeDeltaTicks());
        minecraft.getMainRenderTarget().bindWrite(false);
        RenderSystem.enableDepthTest();
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
            } catch (IOException | RuntimeException e) {
                ChronoGenesis.LOGGER.error("Could not load the event horizon shader; obelisks will not distort the Lens", e);
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

    private static void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
    }
}
