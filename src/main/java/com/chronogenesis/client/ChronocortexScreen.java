package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.biocomputer.OrganismReport;
import com.chronogenesis.biocomputer.PlayerResearch;
import com.chronogenesis.biocomputer.Research;
import com.chronogenesis.biocomputer.ResearchTopic;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.network.OpenChronocortexPayload;
import com.chronogenesis.network.ResearchPayload;
import com.chronogenesis.registry.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Chronocortex's view. Two pages: the organisms wired to it by Ganglions (store, hunger, stability),
 * and research (topics, what each costs, and which of those costs are already met).
 */
public class ChronocortexScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int ROW = 22;
    private static final int OK = 0xFF88DD88;
    private static final int MISSING = 0xFFDD7777;
    private static final int TEXT = 0xFFE0D8C8;
    private static final int DIM = 0xFF9A9080;

    private OpenChronocortexPayload data;
    private boolean researchPage;
    @Nullable
    private ResourceLocation selected;
    private Button researchButton;

    public ChronocortexScreen(OpenChronocortexPayload data) {
        super(Component.translatable("screen.chronogenesis.chronocortex.title"));
        this.data = data;
    }

    /** New data arrived for the open screen (after researching): keep the page, refresh the numbers. */
    public void update(OpenChronocortexPayload newData) {
        this.data = newData;
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();
        addRenderableWidget(Button.builder(Component.translatable("screen.chronogenesis.chronocortex.organisms"),
                b -> { researchPage = false; rebuildWidgets(); }).bounds(x, y - 22, 80, 20).build()).active = researchPage;
        addRenderableWidget(Button.builder(Component.translatable("screen.chronogenesis.chronocortex.research"),
                b -> { researchPage = true; rebuildWidgets(); }).bounds(x + 84, y - 22, 80, 20).build()).active = !researchPage;

        if (researchPage) {
            List<Map.Entry<ResourceLocation, ResearchTopic>> topics = topics();
            for (int i = 0; i < topics.size() && i < 8; i++) {
                ResourceLocation id = topics.get(i).getKey();
                ResearchTopic topic = topics.get(i).getValue();
                Component label = topic.title().copy().withStyle(research().isCompleted(id) ? ChatFormatting.GREEN : ChatFormatting.RESET);
                addRenderableWidget(Button.builder(label, b -> { selected = id; rebuildWidgets(); })
                        .bounds(x + 6, y + 6 + i * ROW, 110, 20).build());
            }
            researchButton = addRenderableWidget(Button.builder(Component.translatable("screen.chronogenesis.chronocortex.begin"),
                    b -> {
                        if (selected != null) {
                            PacketDistributor.sendToServer(new ResearchPayload(data.pos(), selected));
                        }
                    }).bounds(x + WIDTH - 96, y + HEIGHT - 26, 90, 20).build());
            researchButton.active = false;
        }
    }

    private PlayerResearch research() {
        return minecraft.player.getData(ModAttachments.PLAYER_RESEARCH);
    }

    private List<Map.Entry<ResourceLocation, ResearchTopic>> topics() {
        List<Map.Entry<ResourceLocation, ResearchTopic>> list = new ArrayList<>();
        minecraft.level.registryAccess().registry(Research.REGISTRY).ifPresent(registry ->
                registry.entrySet().forEach(entry -> {
                    if (entry.getValue().stage() <= data.stage()) {
                        list.add(Map.entry(entry.getKey().location(), entry.getValue()));
                    }
                }));
        list.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, ResearchTopic> e) -> e.getValue().order())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.stage", data.stage()),
                x + WIDTH - 70, y - 16, DIM, false);
        if (researchPage) {
            renderResearch(graphics, x, y);
        } else {
            renderOrganisms(graphics, x, y);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        // Dark glass in a brass frame, under the buttons.
        int x = left();
        int y = top();
        graphics.fill(x - 2, y - 2, x + WIDTH + 2, y + HEIGHT + 2, 0xFFB08A3E);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xF0141A16);
    }

    private void renderOrganisms(GuiGraphics graphics, int x, int y) {
        if (data.organisms().isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("screen.chronogenesis.chronocortex.no_organisms"),
                    x + 10, y + 10, WIDTH - 20, DIM);
            return;
        }
        int row = 0;
        for (OrganismReport report : data.organisms()) {
            if (row >= 8) {
                break;
            }
            int ry = y + 8 + row * ROW;
            int color = 0xFF000000 | report.aspect().color();
            graphics.fill(x + 8, ry, x + 14, ry + 14, color);
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.organism",
                    report.aspect().displayName(), report.size(), report.pos().getX(), report.pos().getY(), report.pos().getZ()),
                    x + 20, ry, TEXT, false);
            int barX = x + 20;
            int barW = 120;
            graphics.fill(barX, ry + 11, barX + barW, ry + 15, 0xFF2A2A2A);
            graphics.fill(barX, ry + 11, barX + barW * report.stored() / Math.max(1, report.capacity()), ry + 15, color);
            Component status = Component.translatable("message.chronogenesis.organism.hunger." + report.hunger());
            graphics.drawString(font, Component.literal(report.stored() + "/" + report.capacity() + "  ").append(status),
                    barX + barW + 6, ry + 8, DIM, false);
            if (report.aspect() == Aspect.TIME && report.instability() > 0) {
                graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.instability",
                        report.instability(), report.burstAt()), x + WIDTH - 90, ry, MISSING, false);
            }
            row++;
        }
    }

    private void renderResearch(GuiGraphics graphics, int x, int y) {
        List<Map.Entry<ResourceLocation, ResearchTopic>> topics = topics();
        if (topics.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("screen.chronogenesis.chronocortex.no_research"),
                    x + 10, y + 10, WIDTH - 20, DIM);
            return;
        }
        ResearchTopic topic = selected == null ? null : topics.stream()
                .filter(e -> e.getKey().equals(selected)).map(Map.Entry::getValue).findFirst().orElse(null);
        int dx = x + 124;
        if (topic == null) {
            graphics.drawWordWrap(font, Component.translatable("screen.chronogenesis.chronocortex.pick"), dx, y + 8, WIDTH - 130, DIM);
            return;
        }
        Research.Check check = Research.check(minecraft.player, selected, topic, data.stage(), data.essence());
        researchButton.active = check.ok();

        int line = y + 8;
        graphics.renderItem(new ItemStack(topic.icon()), dx, line - 2);
        graphics.drawString(font, topic.title(), dx + 20, line + 2, TEXT, false);
        line += 18;
        graphics.drawWordWrap(font, topic.description(), dx, line, WIDTH - 130, DIM);
        line += font.wordWrapHeight(topic.description(), WIDTH - 130) + 6;

        if (check.completed()) {
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.done"), dx, line, OK, false);
            return;
        }
        if (!check.prerequisites()) {
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.needs_first"), dx, line, MISSING, false);
            line += 10;
        }
        for (Map.Entry<Aspect, Integer> cost : topic.essence().entrySet()) {
            int have = data.essence().getOrDefault(cost.getKey(), 0);
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.essence_cost",
                    cost.getKey().displayName(), have, cost.getValue()), dx, line, have >= cost.getValue() ? OK : MISSING, false);
            line += 10;
        }
        for (SizedIngredient material : topic.materials()) {
            int have = Research.count(minecraft.player.getInventory(), material);
            ItemStack[] options = material.getItems();
            Component name = options.length > 0 ? options[0].getHoverName() : Component.literal("?");
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.material_cost",
                    name, have, material.count()), dx, line, have >= material.count() ? OK : MISSING, false);
            line += 10;
        }
        if (!topic.clues().isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.clues"), dx, line, TEXT, false);
            line += 10;
            PlayerResearch research = research();
            for (List<ResourceLocation> set : topic.clues()) {
                Component setLine = Component.empty();
                boolean first = true;
                for (ResourceLocation clue : set) {
                    if (!first) {
                        setLine = setLine.copy().append(Component.literal(" + "));
                    }
                    setLine = setLine.copy().append(Clues.name(clue).copy()
                            .withStyle(research.hasClue(clue) ? ChatFormatting.GREEN : ChatFormatting.RED));
                    first = false;
                }
                graphics.drawString(font, Component.literal(" - ").append(setLine), dx, line, DIM, false);
                line += 10;
            }
        }
    }
}
