package com.chronogenesis.client;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.time.TimeZone;
import com.chronogenesis.time.TimeZones;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Chunks with their own time, on the client.
 * <ul>
 *   <li>Each tick: the zones count their own whole ticks (so creatures inside stop or crawl here too, not only on
 *       the server), zones catching up run their creatures' extra ticks, and the sky follows the clock of the
 *       chunk the player stands in ({@code ClientLevelMixin}).</li>
 *   <li>Each frame: one post pass ({@code shaders/post/time_zone.json}) shows them the way slowed light would
 *       look (see {@code time_zone.fsh}). The frame's depth is copied during the world render, where the camera's
 *       matrices are known; the pass runs from a HUD layer, as the Temporal Lens's does.</li>
 * </ul>
 * If the shader cannot be loaded, the zones still work; they just do not look like anything.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class TimeZoneView {
    private static final ResourceLocation SHADER = ChronoGenesis.id("shaders/post/time_zone.json");
    private static final int MAX_ZONES = 4;

    @Nullable
    private static PostChain chain;
    private static boolean broken;
    private static int width;
    private static int height;
    private static boolean ready;
    private static final Matrix4f INV_VIEW_PROJ = new Matrix4f();
    private static Vec3 camera = Vec3.ZERO;

    private TimeZoneView() {}

    @SubscribeEvent
    static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            if (chain != null) {
                chain.close();
                chain = null;
            }
            broken = false;
        });
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            TimeZones.clientClear();
            return;
        }
        if (!minecraft.isPaused()) {
            TimeZones.clientTick(minecraft.player.blockPosition());
        }
    }

    @SubscribeEvent
    static void onClientTickPost(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !TimeZones.clientZones().isEmpty() && !Minecraft.getInstance().isPaused()) {
            TimeZones.extraTicks(level, TimeZones.clientZones(), level::tickNonPassenger);
        }
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        TimeZones.clientClear();
    }

    // ---------------------------------------------------------------- the look

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        ready = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (TimeZones.clientZones().isEmpty() || minecraft.level == null) {
            return;
        }
        PostChain effect = chain(minecraft);
        if (effect == null) {
            return;
        }
        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget depth = effect.getTempTarget("depth");
        depth.copyDepthFrom(main);
        main.bindWrite(false);
        new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert(INV_VIEW_PROJ);
        camera = event.getCamera().getPosition();
        ready = true;
    }

    /** A HUD layer, drawn first: the slowed and stopped chunks, over the world. */
    public static void composite(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ready || chain == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        ready = false;
        graphics.flush();
        ClientLevel level = minecraft.level;
        // The nearest zones (the shader takes four).
        List<TimeZone> zones = TimeZones.clientZones().values().stream()
                .sorted(Comparator.comparingDouble(zone -> zone.chunk.getMiddleBlockPosition((int) camera.y)
                        .distToCenterSqr(camera)))
                .limit(MAX_ZONES).toList();
        float viewer = (float) Math.max(0.0, TimeZones.playerRate(minecraft.player));
        boolean goggles = minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CHRONO_GOGGLES.get());
        long gameTime = level.getGameTime();
        float seconds = (gameTime % 24000 + deltaTracker.getGameTimeDeltaPartialTick(false)) / 20.0f;
        for (PostPass pass : chain.passes) {
            var effect = pass.getEffect();
            effect.safeGetUniform("InvViewProj").set(INV_VIEW_PROJ);
            effect.safeGetUniform("Seconds").set(seconds);
            effect.safeGetUniform("ViewerRate").set(viewer);
            effect.safeGetUniform("Goggles").set(goggles ? 1.0f : 0.0f);
            effect.safeGetUniform("ZoneCount").set((float) zones.size());
            for (int i = 0; i < zones.size(); i++) {
                TimeZone zone = zones.get(i);
                effect.safeGetUniform("Zone" + i + "Min").set((float) (zone.chunk.getMinBlockX() - camera.x),
                        (float) (level.getMinBuildHeight() - camera.y), (float) (zone.chunk.getMinBlockZ() - camera.z));
                effect.safeGetUniform("Zone" + i + "Max").set((float) (zone.chunk.getMaxBlockX() + 1 - camera.x),
                        (float) (level.getMaxBuildHeight() - camera.y), (float) (zone.chunk.getMaxBlockZ() + 1 - camera.z));
                effect.safeGetUniform("Zone" + i + "Rate").set((float) zone.rate);
            }
        }
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
            } catch (IOException | RuntimeException e) {
                ChronoGenesis.LOGGER.error("Could not load the time zone shader; slowed chunks will not look slowed", e);
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
