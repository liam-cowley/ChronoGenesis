package com.chronogenesis.client;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Power;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Essence pools in the top-left corner: one thin bar per aspect the player has unlocked, with its power level.
 * Beneath them, the selected active power with its strength, and the icons of the passive powers that are on.
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
        boolean any = false;
        for (Aspect aspect : Aspect.values()) {
            int level = essence.level(aspect);
            if (level == 0) {
                continue;
            }
            any = true;
            int max = Math.max(1, essence.max(aspect));
            int pool = essence.pool(aspect);
            boolean drinking = essence.passives().stream().anyMatch(power -> power.aspects().contains(aspect));
            int color = aspect.color();
            int alpha = drinking ? 0xFF : 0x90;
            graphics.fill(x, y + 3, x + BAR_WIDTH, y + 7, 0x80000000);
            graphics.fill(x, y + 3, x + BAR_WIDTH * pool / max, y + 7, (alpha << 24) | color);
            graphics.drawString(font, Component.translatable("screen.chronogenesis.hud.pool", aspect.displayName(), level),
                    x + 4 + BAR_WIDTH, y, drinking ? 0xFFFFFFFF : 0xFFAAAAAA, true);
            y += ROW_HEIGHT;
        }
        if (!any) {
            return;
        }
        Power selected = essence.selected();
        if (essence.knows(selected)) {
            y += 2;
            graphics.blit(selected.icon(), x, y, 0, 0, 12, 12, 12, 12);
            graphics.drawString(font, Component.translatable("screen.chronogenesis.hud.selected", selected.displayName(),
                    essence.strength(selected)), x + 15, y + 2, 0xFFFFE0A0, true);
            y += 14;
        }
        int ix = x;
        for (Power power : essence.passives()) {
            graphics.blit(power.icon(), ix, y, 0, 0, 10, 10, 10, 10);
            ix += 12;
        }
    }
}
