package com.chronogenesis.biocomputer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;

import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The Chronocortex listens and talks back (core-systems.md section 5). Whatever is said in chat near it, if it is
 * said to it (its name, "cortex" or "brain", a question, or said standing close), it hears, and a moment later
 * answers, out loud, to everyone near. What it says is data ({@link Dialogue}).
 * <ul>
 *   <li>Before it has been taught words, it can only react: a twitch, a flicker.</li>
 *   <li>It remembers what you ask it to call you ("call me ...", "my name is ...").</li>
 *   <li>As it grows (stage 1 to 3), the same questions get older answers: childlike, then demanding, then something
 *       else.</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class CortexSpeech {
    public static final ResourceKey<Registry<Dialogue>> REGISTRY = ResourceKey.createRegistryKey(ChronoGenesis.id("dialogue"));
    /** How far it hears, by stage. */
    private static final int[] HEARING = {16, 24, 32, 48};
    /** Standing this close, anything you say is said to it. */
    private static final double CLOSE = 6.0;
    private static final Pattern CALL_ME = Pattern.compile(
            "(?:my name is|call me|i am called|i'm called)\\s+([\\p{L}\\p{N}_'\\- ]{1,24}?)[.!?]*$", Pattern.CASE_INSENSITIVE);

    /** Chronocortexes loaded on the server. */
    static final Set<ChronocortexBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private CortexSpeech() {}

    @SubscribeEvent
    static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
        // Not synced: only the server speaks.
        event.dataPackRegistry(REGISTRY, Dialogue.CODEC);
    }

    public static int hearing(int stage) {
        return HEARING[Math.min(stage, HEARING.length - 1)];
    }

    @SubscribeEvent
    static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String said = event.getRawText();
        ChronocortexBlockEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (ChronocortexBlockEntity cortex : List.copyOf(LOADED)) {
            if (cortex.isRemoved() || cortex.getLevel() != player.level()) {
                continue;
            }
            double distance = player.distanceToSqr(cortex.getBlockPos().getCenter());
            int range = hearing(cortex.stage());
            if (distance <= (double) range * range && distance < best) {
                best = distance;
                nearest = cortex;
            }
        }
        if (nearest != null && addressed(nearest, said, Math.sqrt(best))) {
            nearest.hear(player, said);
        }
    }

    private static boolean addressed(ChronocortexBlockEntity cortex, String said, double distance) {
        String lower = said.toLowerCase(Locale.ROOT);
        return distance <= CLOSE || lower.contains("cortex") || lower.contains("brain") || said.trim().endsWith("?")
                || (cortex.name() != null && lower.contains(cortex.name().toLowerCase(Locale.ROOT)));
    }

    /** Its reply, a moment after hearing. */
    static void reply(ChronocortexBlockEntity cortex, ServerPlayer player, String said) {
        ServerLevel level = (ServerLevel) cortex.getLevel();
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        if (!cortex.canSpeak()) {
            String line = pick(level, Dialogue.Kind.WORDLESS, cortex, random);
            if (line != null) {
                cortex.say(format(line, cortex, player, Component.empty()));
            }
            return;
        }
        Matcher callMe = CALL_ME.matcher(said.trim());
        if (callMe.find()) {
            String nickname = callMe.group(1).trim();
            cortex.nickname(player, nickname);
            String line = pick(level, Dialogue.Kind.CHAT, cortex, random, List.of("remember my name"));
            cortex.say(format(line != null ? line : "%1$s.", cortex, player, Component.literal(nickname)));
            return;
        }
        String lower = said.toLowerCase(Locale.ROOT);
        Dialogue chosen = null;
        int bestScore = 0;
        for (Dialogue dialogue : dialogues(level)) {
            if (dialogue.kind() != Dialogue.Kind.CHAT || !allowed(dialogue, cortex) || dialogue.linesFor(cortex.stage()).isEmpty()) {
                continue;
            }
            int matched = dialogue.matches(lower);
            int score = matched == 0 ? 0 : matched * 10 + dialogue.priority();
            if (score > bestScore) {
                bestScore = score;
                chosen = dialogue;
            }
        }
        if (chosen == null) {
            String line = pick(level, Dialogue.Kind.CONFUSED, cortex, random);
            if (line != null) {
                cortex.say(format(line, cortex, player, Component.empty()));
            }
            return;
        }
        List<String> lines = chosen.linesFor(cortex.stage());
        cortex.say(format(lines.get(random.nextInt(lines.size())), cortex, player, answer(chosen.answer(), cortex, player)));
    }

    /** Muttering to itself, now and then, while someone is near. */
    static void idle(ChronocortexBlockEntity cortex, ServerLevel level, ServerPlayer listener) {
        String line = pick(level, Dialogue.Kind.IDLE, cortex, level.random);
        if (line != null) {
            cortex.say(format(line, cortex, listener, answer(Dialogue.Answer.WANT, cortex, listener)));
        } else {
            cortex.say(Component.translatable("whisper.chronogenesis." + Whispers.randomIdle(level.random)));
        }
    }

    private static List<Dialogue> dialogues(ServerLevel level) {
        return level.registryAccess().registry(REGISTRY).map(registry -> registry.stream().toList()).orElse(List.of());
    }

    private static boolean allowed(Dialogue dialogue, ChronocortexBlockEntity cortex) {
        return dialogue.requires().map(cortex::knows).orElse(true);
    }

    /** A random line of a kind for its stage (optionally only from dialogues with one of these keywords). */
    @Nullable
    private static String pick(ServerLevel level, Dialogue.Kind kind, ChronocortexBlockEntity cortex, RandomSource random,
            List<String> keywords) {
        List<String> pool = new ArrayList<>();
        for (Dialogue dialogue : dialogues(level)) {
            if (dialogue.kind() == kind && allowed(dialogue, cortex)
                    && (keywords.isEmpty() || dialogue.keywords().stream().anyMatch(keywords::contains))) {
                pool.addAll(dialogue.linesFor(cortex.stage()));
            }
        }
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    @Nullable
    private static String pick(ServerLevel level, Dialogue.Kind kind, ChronocortexBlockEntity cortex, RandomSource random) {
        return pick(level, kind, cortex, random, List.of());
    }

    private static Component format(String line, ChronocortexBlockEntity cortex, ServerPlayer listener, Component answer) {
        return Component.translatable(line, cortex.callName(listener), cortex.displayName(), answer);
    }

    /** What it works out to say. */
    static Component answer(Dialogue.Answer answer, ChronocortexBlockEntity cortex, ServerPlayer player) {
        ServerLevel level = (ServerLevel) cortex.getLevel();
        return switch (answer) {
            case NONE -> Component.empty();
            case NAME -> cortex.displayName();
            case STAGE -> Component.translatable("screen.chronogenesis.chronocortex.stage_name." + cortex.stage());
            case STATUS -> {
                Map<Aspect, Integer> essence = cortex.availableEssence();
                int total = essence.values().stream().mapToInt(Integer::intValue).sum();
                yield Component.translatable("whisper.chronogenesis.answer.status", cortex.wiredCores().size(), total,
                        cortex.lobes(), cortex.awareness());
            }
            case WANT -> {
                Map.Entry<ResourceLocation, Lesson> next = cortex.nextLesson();
                if (next != null) {
                    var items = next.getValue().item().getItems();
                    yield items.length > 0 ? items[0].getHoverName() : next.getValue().title();
                }
                yield Component.translatable("whisper.chronogenesis.answer.want_room");
            }
            case RESEARCH -> {
                for (var entry : level.registryAccess().registryOrThrow(Research.REGISTRY).entrySet()) {
                    ResourceLocation id = entry.getKey().location();
                    if (entry.getValue().stage() <= cortex.stage() && !Research.has(player, id)) {
                        yield entry.getValue().title();
                    }
                }
                yield Component.translatable("whisper.chronogenesis.answer.research_done");
            }
            case TIME -> {
                if (!cortex.knows(Lessons.TIME)) {
                    yield Component.translatable("whisper.chronogenesis.answer.time_unknown");
                }
                long day = (level.getDayTime() + 6000) % 24000;
                int hours = (int) (day / 1000);
                int minutes = (int) (day % 1000 * 60 / 1000);
                yield Component.literal(String.format(Locale.ROOT, "%02d:%02d", hours, minutes));
            }
        };
    }
}
