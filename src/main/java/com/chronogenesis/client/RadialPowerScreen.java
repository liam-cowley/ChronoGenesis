package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.network.SelectPowerPayload;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.registry.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The radial power menu: shown while the power key is held. Every unlocked power sits on a ring around
 * the centre; pointing the mouse at one highlights it, and releasing the key (or clicking) selects it.
 */
public class RadialPowerScreen extends Screen {
    private static final int RADIUS = 56;
    private static final int DEAD_ZONE = 14;
    private static final int SLOT = 12;

    private final KeyMapping holdKey;
    private final List<Aspect> powers = new ArrayList<>();
    private int hovered = -1;

    public RadialPowerScreen(KeyMapping holdKey) {
        super(Component.translatable("screen.chronogenesis.radial.title"));
        this.holdKey = holdKey;
    }

    @Override
    protected void init() {
        powers.clear();
        if (minecraft != null && minecraft.player != null) {
            PlayerEssence essence = minecraft.player.getData(ModAttachments.PLAYER_ESSENCE);
            for (Aspect aspect : Aspect.values()) {
                if (essence.level(aspect) > 0) {
                    powers.add(aspect);
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // Screens swallow key events, so poll the key directly: letting go of it picks the hovered power.
        if (minecraft != null && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), holdKey.getKey().getValue())) {
            choose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        choose();
        return true;
    }

    private void choose() {
        if (hovered >= 0 && hovered < powers.size()) {
            PacketDistributor.sendToServer(new SelectPowerPayload(powers.get(hovered)));
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        if (powers.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.chronogenesis.radial.none"), cx, cy, 0xFFAAAAAA);
            return;
        }
        hovered = hoveredIndex(mouseX - cx, mouseY - cy);
        PlayerEssence essence = minecraft.player.getData(ModAttachments.PLAYER_ESSENCE);

        for (int i = 0; i < powers.size(); i++) {
            Aspect aspect = powers.get(i);
            double angle = angleOf(i);
            int x = cx + (int) Math.round(Math.cos(angle) * RADIUS);
            int y = cy + (int) Math.round(Math.sin(angle) * RADIUS);
            boolean isHovered = i == hovered;
            boolean selected = essence.selected() == aspect;
            int size = isHovered ? SLOT + 3 : SLOT;

            // A disc of the essence's colour, ringed brass when hovered, with its pool as a bar beneath.
            int ring = isHovered ? 0xFFD6A850 : selected ? 0xFFFFFFFF : 0xFF222222;
            graphics.fill(x - size - 1, y - size - 1, x + size + 1, y + size + 1, ring);
            graphics.fill(x - size, y - size, x + size, y + size, 0xFF000000 | aspect.color());
            int max = Math.max(1, essence.max(aspect));
            graphics.fill(x - SLOT, y + size + 3, x + SLOT, y + size + 5, 0x80000000);
            graphics.fill(x - SLOT, y + size + 3, x - SLOT + 2 * SLOT * essence.pool(aspect) / max, y + size + 5, 0xFF000000 | aspect.color());
            graphics.drawCenteredString(font, Component.translatable(aspect.translationKey()), x, y + size + 7,
                    isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            if (essence.isActive(aspect)) {
                graphics.drawCenteredString(font, "*", x, y - 4, 0xFFFFFFFF);
            }
        }
        graphics.drawCenteredString(font, Component.translatable("screen.chronogenesis.radial.hint"), cx, cy - 4, 0xFFCCCCCC);
    }

    /** Slots start at the top and go clockwise. */
    private double angleOf(int index) {
        return -Math.PI / 2 + 2 * Math.PI * index / powers.size();
    }

    private int hoveredIndex(double dx, double dy) {
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2;
        double step = 2 * Math.PI / powers.size();
        return Mth.positiveModulo((int) Math.round(angle / step), powers.size());
    }
}
