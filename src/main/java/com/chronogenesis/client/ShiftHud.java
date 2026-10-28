package com.chronogenesis.client;

import com.chronogenesis.network.ShiftStatusPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * While shifted into another era: its name and how long the shift still holds, top centre. The client counts down
 * between the server's updates; under half a minute it turns red and flickers.
 */
public final class ShiftHud {
    private static int ticksLeft;
    private static Component era = Component.empty();

    private ShiftHud() {}

    public static void receive(ShiftStatusPayload payload) {
        ticksLeft = payload.ticksLeft();
        era = payload.era();
    }

    static void tick() {
        if (ticksLeft > 0 && !Minecraft.getInstance().isPaused()) {
            ticksLeft--;
        }
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ticksLeft <= 0 || minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        int seconds = ticksLeft / 20;
        boolean thin = seconds < 30;
        if (thin && (minecraft.player.tickCount / 5) % 2 == 0) {
            return;
        }
        Component line = Component.translatable("screen.chronogenesis.shift.hud", era, seconds / 60, String.format("%02d", seconds % 60));
        graphics.drawCenteredString(minecraft.font, line, graphics.guiWidth() / 2, 6, thin ? 0xFFE05050 : 0xFF6FE0D0);
    }
}
