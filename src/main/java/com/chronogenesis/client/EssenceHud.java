package com.chronogenesis.client;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Essence pools in the top-left corner: one thin bar per aspect the player has unlocked.
 * The selected power is marked with an arrow, active powers are drawn brighter.
 */
public final class EssenceHud {
    private static final int BAR_WIDTH = 60;
    private static final int ROW_HEIGHT = 11;

    private EssenceHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        PlayerEssence essence = minecraft.player.getData(ModAttachments.PLAYER_ESSENCE);
        Font font = minecraft.font;
        int x = 6;
        int y = 6;
        for (Aspect aspect : Aspect.values()) {
            int level = essence.level(aspect);
            if (level == 0) {
                continue;
            }
            int max = Math.max(1, essence.max(aspect));
            int pool = essence.pool(aspect);
            boolean active = essence.isActive(aspect);
            boolean selected = essence.selected() == aspect;

            int color = aspect.color();
            int alpha = active ? 0xFF : 0x90;
            graphics.fill(x + 8, y + 3, x + 8 + BAR_WIDTH, y + 7, 0x80000000);
            graphics.fill(x + 8, y + 3, x + 8 + BAR_WIDTH * pool / max, y + 7, (alpha << 24) | color);
            if (selected) {
                graphics.drawString(font, ">", x, y, 0xFFFFFFFF, true);
            }
            graphics.drawString(font, Component.translatable(aspect.translationKey()), x + 12 + BAR_WIDTH, y,
                    active ? 0xFFFFFFFF : 0xFFAAAAAA, true);
            y += ROW_HEIGHT;
        }
    }
}
