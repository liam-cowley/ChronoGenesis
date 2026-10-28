package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.essence.Aspect;
import com.chronogenesis.network.PowerActionPayload;
import com.chronogenesis.player.PlayerEssence;
import com.chronogenesis.player.Power;
import com.chronogenesis.registry.ModAttachments;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Essence Manipulator's wheel, opened with Shift + R and held open while Shift is down.
 * <ul>
 *   <li>Outer ring: the active powers. Point at one, or turn the mouse wheel to step through them; letting go of
 *       Shift chooses the highlighted one (so does clicking it).</li>
 *   <li>Inner ring: the passive powers. Clicking one switches it on or off; several can be on at once.</li>
 *   <li>Right-clicking any power steps its strength (1 up to its maximum, then back to 1).</li>
 * </ul>
 */
public class PowerWheelScreen extends Screen {
    private static final int OUTER = 74;
    private static final int INNER = 36;
    private static final int SLOT = 11;
    private static final int SMALL = 9;
    private static final int HOVER_REACH = 18;
    private static final int BRASS = 0xFFD6A850;
    private static final int BRASS_DK = 0xFF7A5A26;

    private record Slot(Power power, int x, int y, int size) {}

    private final List<Power> actives = new ArrayList<>();
    private final List<Power> passives = new ArrayList<>();
    private final List<Slot> slots = new ArrayList<>();
    /** The highlighted active power (index into actives), moved by pointing or scrolling. */
    private int highlight = -1;
    /** The slot under the mouse (either ring), or null. */
    @Nullable
    private Slot hovered;
    private double lastMouseX = Double.NaN;
    private double lastMouseY = Double.NaN;

    public PowerWheelScreen() {
        super(Component.translatable("screen.chronogenesis.wheel.title"));
    }

    private PlayerEssence essence() {
        return minecraft.player.getData(ModAttachments.PLAYER_ESSENCE);
    }

    @Override
    protected void init() {
        actives.clear();
        passives.clear();
        PlayerEssence essence = essence();
        for (Power power : Power.values()) {
            if (essence.knows(power)) {
                (power.isPassive() ? passives : actives).add(power);
            }
        }
        highlight = actives.indexOf(essence.selected());
        layout();
    }

    private void layout() {
        slots.clear();
        int cx = width / 2;
        int cy = height / 2;
        for (int i = 0; i < actives.size(); i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / actives.size();
            slots.add(new Slot(actives.get(i), cx + (int) Math.round(Math.cos(angle) * OUTER),
                    cy + (int) Math.round(Math.sin(angle) * OUTER), SLOT));
        }
        for (int i = 0; i < passives.size(); i++) {
            double angle = -Math.PI / 2 + Math.PI / Math.max(1, passives.size()) + 2 * Math.PI * i / passives.size();
            slots.add(new Slot(passives.get(i), cx + (int) Math.round(Math.cos(angle) * INNER),
                    cy + (int) Math.round(Math.sin(angle) * INNER), SMALL));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        // Screens swallow key events, so poll Shift directly: letting go of it chooses the highlighted power.
        if (minecraft != null && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(),
                minecraft.options.keyShift.getKey().getValue())) {
            choose();
        }
    }

    private void choose() {
        if (highlight >= 0 && highlight < actives.size() && actives.get(highlight) != essence().selected()) {
            PacketDistributor.sendToServer(new PowerActionPayload(PowerActionPayload.Action.SELECT, actives.get(highlight), 0));
        }
        onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!actives.isEmpty() && scrollY != 0) {
            int step = scrollY > 0 ? -1 : 1;
            highlight = Mth.positiveModulo((highlight < 0 ? 0 : highlight) + step, actives.size());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = slotAt(mouseX, mouseY);
        if (slot == null) {
            return true;
        }
        PlayerEssence essence = essence();
        if (button == 1) {
            int max = slot.power.maxStrength(essence);
            int next = essence.strength(slot.power) % max + 1;
            PacketDistributor.sendToServer(new PowerActionPayload(PowerActionPayload.Action.STRENGTH, slot.power, next));
        } else if (slot.power.isPassive()) {
            PacketDistributor.sendToServer(new PowerActionPayload(PowerActionPayload.Action.TOGGLE, slot.power, 0));
        } else {
            highlight = actives.indexOf(slot.power);
            choose();
        }
        return true;
    }

    @Nullable
    private Slot slotAt(double mouseX, double mouseY) {
        Slot best = null;
        double bestDistance = HOVER_REACH * HOVER_REACH;
        for (Slot slot : slots) {
            double dx = mouseX - slot.x;
            double dy = mouseY - slot.y;
            double distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = slot;
            }
        }
        return best;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        PlayerEssence essence = essence();
        if (actives.isEmpty() && passives.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.chronogenesis.wheel.none"), cx, cy, 0xFFAAAAAA);
            return;
        }
        // Moving the mouse points at things; scrolling alone keeps the scrolled highlight.
        boolean moved = mouseX != lastMouseX || mouseY != lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        hovered = slotAt(mouseX, mouseY);
        if (moved && hovered != null && !hovered.power.isPassive()) {
            highlight = actives.indexOf(hovered.power);
        }

        // A brass ring for the actives, a thinner one for the passives.
        ring(graphics, cx, cy, OUTER, 0x90D6A850);
        ring(graphics, cx, cy, INNER, 0x60D6A850);
        for (Slot slot : slots) {
            boolean lit = slot.power.isPassive() ? essence.isOn(slot.power)
                    : highlight >= 0 && highlight < actives.size() && actives.get(highlight) == slot.power;
            boolean selected = !slot.power.isPassive() && essence.selected() == slot.power;
            drawSlot(graphics, slot, essence, lit, selected, slot == hovered);
        }

        // The centre: what is highlighted (or under the mouse), its strength and what it costs.
        Power shown = hovered != null ? hovered.power
                : highlight >= 0 && highlight < actives.size() ? actives.get(highlight) : null;
        if (shown != null) {
            int strength = essence.strength(shown);
            int max = shown.maxStrength(essence);
            graphics.drawCenteredString(font, shown.displayName(), cx, cy - 10, 0xFFFFFFFF);
            graphics.drawCenteredString(font, Component.translatable("screen.chronogenesis.wheel.strength", strength, max),
                    cx, cy + 2, BRASS);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(shown.description(), 200);
            int y = cy + OUTER + SLOT + 12;
            for (var line : lines) {
                graphics.drawCenteredString(font, line, cx, y, 0xFFCCCCCC);
                y += 10;
            }
        }
        graphics.drawCenteredString(font, Component.translatable("screen.chronogenesis.wheel.hint"), cx,
                cy - OUTER - SLOT - 20, 0xFF9A9080);
    }

    private void drawSlot(GuiGraphics graphics, Slot slot, PlayerEssence essence, boolean lit, boolean selected, boolean hover) {
        int s = slot.size + (hover ? 2 : 0);
        int x = slot.x;
        int y = slot.y;
        int frame = lit ? BRASS : selected ? 0xFFFFFFFF : BRASS_DK;
        graphics.fill(x - s - 1, y - s - 1, x + s + 1, y + s + 1, frame);
        // Background split between the power's essences (one colour, or two halves for a composite).
        List<Aspect> aspects = slot.power.aspects();
        int dark = lit ? 0xFF000000 : 0xC0000000;
        graphics.fill(x - s, y - s, x + s, y + s, dark);
        int bandTop = y + s - 3;
        for (int i = 0; i < aspects.size(); i++) {
            int x0 = x - s + 2 * s * i / aspects.size();
            int x1 = x - s + 2 * s * (i + 1) / aspects.size();
            graphics.fill(x0, bandTop, x1, y + s, 0xFF000000 | aspects.get(i).color());
        }
        int iconSize = slot.size == SLOT ? 16 : 12;
        graphics.pose().pushPose();
        graphics.pose().translate(x - iconSize / 2.0f, y - iconSize / 2.0f - 1, 0);
        graphics.pose().scale(iconSize / 16.0f, iconSize / 16.0f, 1.0f);
        graphics.blit(slot.power.icon(), 0, 0, 0, 0, 16, 16, 16, 16);
        graphics.pose().popPose();
        if (!lit && slot.power.isPassive()) {
            graphics.fill(x - s, y - s, x + s, y + s, 0x70000000);
        }
        // Strength pips above the slot.
        int strength = essence.strength(slot.power);
        int max = slot.power.maxStrength(essence);
        for (int i = 0; i < max; i++) {
            int px = x - (max * 4) / 2 + i * 4;
            graphics.fill(px, y - s - 4, px + 3, y - s - 2, i < strength ? BRASS : 0xFF333333);
        }
        // How full the pools it drinks from are, as a bar beneath.
        int barY = y + s + 3;
        for (int i = 0; i < aspects.size(); i++) {
            Aspect aspect = aspects.get(i);
            int max2 = Math.max(1, essence.max(aspect));
            int x0 = x - s + 2 * s * i / aspects.size();
            int x1 = x - s + 2 * s * (i + 1) / aspects.size();
            graphics.fill(x0, barY, x1, barY + 2, 0x80000000);
            graphics.fill(x0, barY, x0 + (x1 - x0) * essence.pool(aspect) / max2, barY + 2, 0xFF000000 | aspect.color());
        }
    }

    /** A thin ring of brass dots. */
    private static void ring(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        int steps = radius * 3;
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}
