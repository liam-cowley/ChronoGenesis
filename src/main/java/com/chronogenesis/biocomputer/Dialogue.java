package com.chronogenesis.biocomputer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * Something the Chronocortex says (core-systems.md section 5). Data-driven, server-side only:
 * {@code data/<ns>/chronogenesis/dialogue/<name>.json}, so packs can give it another voice.
 * <p>
 * A line is a lang key or plain text; {@code %1$s} is the listener's name (or what they asked to be called),
 * {@code %2$s} the Chronocortex's own name, {@code %3$s} the {@link #answer} (if any).
 *
 * @param kind     when it is said: in reply to chat ({@code keywords} match), muttered unprompted, when it did not
 *                 understand, or when it hears speech before it has words
 * @param keywords words or phrases in what was said (any one matches; more matches win)
 * @param priority breaks ties between replies that match equally
 * @param requires a lesson it must have learned to say this
 * @param answer   something it works out to say: its state, what it wants, research, the time, its name
 * @param lines    by stage: the lines of the highest stage it has reached are used
 */
public record Dialogue(Kind kind, List<String> keywords, int priority, Optional<ResourceLocation> requires, Answer answer,
                       List<Lines> lines) {
    public enum Kind implements StringRepresentable {
        CHAT, IDLE, CONFUSED, WORDLESS;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Answer implements StringRepresentable {
        NONE, STATUS, WANT, RESEARCH, TIME, NAME, STAGE;

        public static final Codec<Answer> CODEC = StringRepresentable.fromEnum(Answer::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Lines(int stage, List<String> say) {
        public static final Codec<Lines> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 16).fieldOf("stage").forGetter(Lines::stage),
                Codec.STRING.listOf().fieldOf("say").forGetter(Lines::say)
        ).apply(instance, Lines::new));
    }

    public static final Codec<Dialogue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.optionalFieldOf("kind", Kind.CHAT).forGetter(Dialogue::kind),
            Codec.STRING.listOf().optionalFieldOf("keywords", List.of()).forGetter(Dialogue::keywords),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Dialogue::priority),
            ResourceLocation.CODEC.optionalFieldOf("requires").forGetter(Dialogue::requires),
            Answer.CODEC.optionalFieldOf("answer", Answer.NONE).forGetter(Dialogue::answer),
            Lines.CODEC.listOf().fieldOf("lines").forGetter(Dialogue::lines)
    ).apply(instance, Dialogue::new));

    /** The lines for a stage: those of the highest stage reached, or none. */
    public List<String> linesFor(int stage) {
        Lines best = null;
        for (Lines group : lines) {
            if (group.stage() <= stage && (best == null || group.stage() > best.stage())) {
                best = group;
            }
        }
        return best == null ? List.of() : best.say();
    }

    /** How many of its keywords (as whole words or phrases) appear in what was said. */
    public int matches(String said) {
        int count = 0;
        for (String keyword : keywords) {
            String k = keyword.toLowerCase(Locale.ROOT);
            int at = said.indexOf(k);
            while (at >= 0) {
                boolean startOk = at == 0 || !Character.isLetterOrDigit(said.charAt(at - 1));
                int end = at + k.length();
                boolean endOk = end >= said.length() || !Character.isLetterOrDigit(said.charAt(end));
                if (startOk && endOk) {
                    count++;
                    break;
                }
                at = said.indexOf(k, at + 1);
            }
        }
        return count;
    }
}
