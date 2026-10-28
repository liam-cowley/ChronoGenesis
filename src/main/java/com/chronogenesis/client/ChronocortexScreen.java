package com.chronogenesis.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.Clues;
import com.chronogenesis.biocomputer.Lesson;
import com.chronogenesis.biocomputer.Lessons;
import com.chronogenesis.biocomputer.OrganismReport;
import com.chronogenesis.biocomputer.PlayerResearch;
import com.chronogenesis.biocomputer.Research;
import com.chronogenesis.biocomputer.ResearchTopic;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.item.EternalHourglassItem;
import com.chronogenesis.network.OpenChronocortexPayload;
import com.chronogenesis.network.ResearchPayload;
import com.chronogenesis.network.ShiftPayload;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.timetravel.Era;
import com.chronogenesis.timetravel.Eras;
import com.chronogenesis.timetravel.TemporalShift;

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
 * The Chronocortex's view. Three pages: the organisms wired to it by Ganglions (store, hunger, stability),
 * research (topics, what each costs, and which of those costs are already met), and its mind (stage, lobes,
 * awareness, the lessons it has learned and could learn, and what it needs to grow up), and eras (the times it can
 * shift a player into, and what each still needs).
 */
public class ChronocortexScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int ROW = 22;
    private static final int OK = 0xFF88DD88;
    private static final int MISSING = 0xFFDD7777;
    private static final int TEXT = 0xFFE0D8C8;
    private static final int DIM = 0xFF9A9080;

    private static final int ORGANISMS = 0;
    private static final int RESEARCH = 1;
    private static final int MIND = 2;
    private static final int ERAS = 3;

    private OpenChronocortexPayload data;
    private int page;
    @Nullable
    private ResourceLocation selected;
    private Button researchButton;
    @Nullable
    private ResourceLocation selectedEra;
    private Button shiftButton;

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
                b -> { page = ORGANISMS; rebuildWidgets(); }).bounds(x, y - 22, 70, 20).build()).active = page != ORGANISMS;
        addRenderableWidget(Button.builder(Component.translatable("screen.chronogenesis.chronocortex.research"),
                b -> { page = RESEARCH; rebuildWidgets(); }).bounds(x + 74, y - 22, 70, 20).build()).active = page != RESEARCH;
        addRenderableWidget(Button.builder(Component.translatable("screen.chronogenesis.chronocortex.mind"),
                b -> { page = MIND; rebuildWidgets(); }).bounds(x + 148, y - 22, 70, 20).build()).active = page != MIND;
        addRenderableWidget(Button.builder(Component.translatable("screen.chronogenesis.chronocortex.eras"),
                b -> { page = ERAS; rebuildWidgets(); }).bounds(x + 222, y - 22, 70, 20).build()).active = page != ERAS;

        if (page == ERAS) {
            List<Map.Entry<ResourceLocation, Era>> eras = Eras.all(minecraft.level.registryAccess());
            for (int i = 0; i < eras.size() && i < 8; i++) {
                ResourceLocation id = eras.get(i).getKey();
                addRenderableWidget(Button.builder(eras.get(i).getValue().title(), b -> { selectedEra = id; rebuildWidgets(); })
                        .bounds(x + 6, y + 6 + i * ROW, 110, 20).build());
            }
            shiftButton = addRenderableWidget(Button.builder(Component.translatable("screen.chronogenesis.chronocortex.shift"),
                    b -> {
                        if (selectedEra != null) {
                            PacketDistributor.sendToServer(new ShiftPayload(data.pos(), selectedEra));
                            onClose();
                        }
                    }).bounds(x + WIDTH - 96, y + HEIGHT - 26, 90, 20).build());
            shiftButton.active = false;
        }

        if (page == RESEARCH) {
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
        switch (page) {
            case RESEARCH -> renderResearch(graphics, x, y);
            case MIND -> renderMind(graphics, x, y);
            case ERAS -> renderEras(graphics, x, y);
            default -> renderOrganisms(graphics, x, y);
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

    /** Its mind: who it is, how far it has grown, what it needs next, and its lessons. */
    private void renderMind(GuiGraphics graphics, int x, int y) {
        OpenChronocortexPayload.Mind mind = data.mind();
        int stage = data.stage();
        int line = y + 8;
        Component who = mind.name().isEmpty() ? Component.translatable("screen.chronogenesis.chronocortex.unnamed")
                : Component.literal(mind.name());
        graphics.drawString(font, who.copy().withStyle(ChatFormatting.LIGHT_PURPLE), x + 8, line, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.stage_name." + stage),
                x + 8 + font.width(who) + 8, line, DIM, false);
        line += 14;
        graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.lobes", mind.lobes()), x + 8, line, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.awareness", mind.awareness()),
                x + 110, line, TEXT, false);
        line += 12;

        // What the next stage needs.
        if (stage < ChronocortexBlockEntity.MAX_STAGE) {
            int next = stage + 1;
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.next", next), x + 8, line, DIM, false);
            line += 10;
            line = requirement(graphics, x + 14, line, mind.lobes() >= ChronocortexBlockEntity.STAGE_LOBES[next],
                    Component.translatable("screen.chronogenesis.chronocortex.need_lobes", ChronocortexBlockEntity.STAGE_LOBES[next]));
            line = requirement(graphics, x + 14, line, mind.awareness() >= ChronocortexBlockEntity.STAGE_AWARENESS[next],
                    Component.translatable("screen.chronogenesis.chronocortex.need_awareness", ChronocortexBlockEntity.STAGE_AWARENESS[next]));
            if (next == 1) {
                line = requirement(graphics, x + 14, line, mind.lessons().contains(Lessons.WORDS),
                        Component.translatable("screen.chronogenesis.chronocortex.need_words"));
            } else if (next == 2) {
                line = requirement(graphics, x + 14, line, mind.lessons().contains(Lessons.SELF),
                        Component.translatable("screen.chronogenesis.chronocortex.need_name"));
            } else {
                line = requirement(graphics, x + 14, line, mind.cranium(),
                        Component.translatable("screen.chronogenesis.chronocortex.need_cranium"));
            }
        } else {
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.complete"), x + 8, line, OK, false);
            line += 10;
        }
        line += 4;

        // Lessons: learned, ready to learn (show it the item), or not yet.
        graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.lessons"), x + 8, line, TEXT, false);
        line += 11;
        int column = 0;
        int top = line;
        for (Map.Entry<ResourceLocation, Lesson> entry : Lessons.all(minecraft.level.registryAccess())) {
            Lesson lesson = entry.getValue();
            boolean learned = mind.lessons().contains(entry.getKey());
            boolean ready = !learned && stage >= lesson.stage() && mind.lobes() >= lesson.capacity();
            int lx = x + 8 + column * (WIDTH / 2);
            ItemStack[] items = lesson.item().getItems();
            if (items.length > 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(lx, line - 2, 0);
                graphics.pose().scale(0.6f, 0.6f, 1.0f);
                graphics.renderItem(items[0], 0, 0);
                graphics.pose().popPose();
            }
            Component label;
            int color;
            if (learned) {
                label = lesson.title();
                color = OK;
            } else if (ready) {
                label = Component.translatable("screen.chronogenesis.chronocortex.lesson_ready", lesson.title());
                color = TEXT;
            } else if (stage < lesson.stage()) {
                label = Component.translatable("screen.chronogenesis.chronocortex.lesson_stage", lesson.title(), lesson.stage());
                color = DIM;
            } else {
                label = Component.translatable("screen.chronogenesis.chronocortex.lesson_room", lesson.title(), lesson.capacity());
                color = DIM;
            }
            graphics.drawString(font, label, lx + 12, line, color, false);
            line += 10;
            if (line > y + HEIGHT - 12) {
                if (column == 1) {
                    break;
                }
                column = 1;
                line = top;
            }
        }
    }

    /** The eras it can reach, and what a shift into the selected one still needs. */
    private void renderEras(GuiGraphics graphics, int x, int y) {
        int dx = x + 124;
        Era era = selectedEra == null ? null : Eras.get(minecraft.level.registryAccess(), selectedEra);
        if (era == null) {
            graphics.drawWordWrap(font, Component.translatable("screen.chronogenesis.chronocortex.pick_era"), dx, y + 8, WIDTH - 130, DIM);
            return;
        }
        OpenChronocortexPayload.Mind mind = data.mind();
        TemporalShift.Check check = TemporalShift.check(minecraft.player, era, data.stage(), mind.lessons(), data.essence());
        shiftButton.active = check.ok();
        int line = y + 8;
        graphics.renderItem(new ItemStack(era.icon()), dx, line - 2);
        graphics.drawString(font, era.title(), dx + 20, line + 2, TEXT, false);
        line += 18;
        graphics.drawWordWrap(font, era.description(), dx, line, WIDTH - 130, DIM);
        line += font.wordWrapHeight(era.description(), WIDTH - 130) + 6;
        line = requirement(graphics, dx, line, check.stage(), Component.translatable("screen.chronogenesis.chronocortex.need_stage", era.stage()));
        if (era.lesson().isPresent()) {
            Lesson lesson = Lessons.get(minecraft.level.registryAccess(), era.lesson().get());
            line = requirement(graphics, dx, line, check.lesson(), Component.translatable("screen.chronogenesis.chronocortex.need_lesson",
                    lesson == null ? Component.literal(era.lesson().get().toString()) : lesson.title()));
        }
        line = requirement(graphics, dx, line, check.tether(), Component.translatable("screen.chronogenesis.chronocortex.need_tether"));
        line = requirement(graphics, dx, line, check.time(), EternalHourglassItem.carried(minecraft.player)
                ? Component.translatable("screen.chronogenesis.chronocortex.essence_from_hourglass", Aspect.TIME.displayName(), era.cost())
                : Component.translatable("screen.chronogenesis.chronocortex.essence_cost", Aspect.TIME.displayName(),
                        data.essence().getOrDefault(Aspect.TIME, 0), era.cost()));
        if (!check.here()) {
            line = requirement(graphics, dx, line, false, Component.translatable("screen.chronogenesis.chronocortex.already_shifted"));
        }
        graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.shift_minutes", era.minutes()),
                dx, line + 4, DIM, false);
    }

    private int requirement(GuiGraphics graphics, int x, int y, boolean met, Component text) {
        graphics.drawString(font, Component.literal(met ? "+ " : "- ").append(text), x, y, met ? OK : MISSING, false);
        return y + 10;
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
        researchButton.setMessage(Component.translatable(check.regrow()
                ? "screen.chronogenesis.chronocortex.regrow" : "screen.chronogenesis.chronocortex.begin"));

        int line = y + 8;
        graphics.renderItem(new ItemStack(topic.icon()), dx, line - 2);
        graphics.drawString(font, topic.title(), dx + 20, line + 2, TEXT, false);
        line += 18;
        graphics.drawWordWrap(font, topic.description(), dx, line, WIDTH - 130, DIM);
        line += font.wordWrapHeight(topic.description(), WIDTH - 130) + 6;

        if (check.completed()) {
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.done"), dx, line, OK, false);
            if (!check.regrow()) {
                return;
            }
            // It can grow the reward again: only the essence and materials are asked for.
            line += 12;
        } else if (!check.prerequisites()) {
            graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.needs_first"), dx, line, MISSING, false);
            line += 10;
        }
        boolean hourglass = EternalHourglassItem.carried(minecraft.player);
        for (Map.Entry<Aspect, Integer> cost : topic.essence().entrySet()) {
            if (hourglass) {
                graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.essence_from_hourglass",
                        cost.getKey().displayName(), cost.getValue()), dx, line, OK, false);
            } else {
                int have = data.essence().getOrDefault(cost.getKey(), 0);
                graphics.drawString(font, Component.translatable("screen.chronogenesis.chronocortex.essence_cost",
                        cost.getKey().displayName(), have, cost.getValue()), dx, line, have >= cost.getValue() ? OK : MISSING, false);
            }
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
        if (!check.regrow() && !topic.clues().isEmpty()) {
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
