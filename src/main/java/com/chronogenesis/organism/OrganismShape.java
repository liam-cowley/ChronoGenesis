package com.chronogenesis.organism;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Aspect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The shape an organism of one essence must be grown into to work (core-systems.md section 2, "Shapes"). A seedling
 * (fewer than {@code minSize} blocks, core included) works whatever its shape; past that, an organism that is not in
 * its essence's shape is <b>malformed</b>: it lives, eats and grows, but nothing can draw on it (Taps, Glands, the
 * Chronocortex, lockets get nothing). Data: {@code data/<ns>/chronogenesis/organism_shape/<name>.json}.
 * <ul>
 *   <li>{@code compact}: solid and squat; at least {@code value} of its bounding box filled, no side more than twice
 *       another (Density: weight packed tight).</li>
 *   <li>{@code elongated}: its longest side at least {@code value} times the next (Speed: a tendon, a track).</li>
 *   <li>{@code broad}: spread flat, both widths at least {@code value} times its height (Size).</li>
 *   <li>{@code branching}: at least {@code value} tips, blocks with one neighbour (Awareness: dendrites).</li>
 *   <li>{@code spire}: at least {@code value} times taller than it is wide (Time: grown like a Nail).</li>
 * </ul>
 */
public record OrganismShape(Aspect aspect, Kind shape, double value, int minSize) {
    public enum Kind implements StringRepresentable {
        COMPACT, ELONGATED, BROAD, BRANCHING, SPIRE;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<OrganismShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Aspect.CODEC.fieldOf("aspect").forGetter(OrganismShape::aspect),
            Kind.CODEC.fieldOf("shape").forGetter(OrganismShape::shape),
            Codec.DOUBLE.optionalFieldOf("value", 2.0).forGetter(OrganismShape::value),
            Codec.intRange(1, 1000).optionalFieldOf("min_size", 5).forGetter(OrganismShape::minSize)
    ).apply(instance, OrganismShape::new));

    public static final ResourceKey<Registry<OrganismShape>> REGISTRY = ResourceKey.createRegistryKey(ChronoGenesis.id("organism_shape"));

    @EventBusSubscriber(modid = ChronoGenesis.MODID)
    static final class Registration {
        private Registration() {}

        @SubscribeEvent
        static void registerRegistry(DataPackRegistryEvent.NewRegistry event) {
            event.dataPackRegistry(REGISTRY, CODEC, CODEC);
        }
    }

    /** The shape an essence's organisms must take, if the data names one. */
    @Nullable
    public static OrganismShape of(RegistryAccess access, Aspect aspect) {
        return access.registry(REGISTRY).flatMap(registry -> registry.stream().filter(shape -> shape.aspect == aspect).findFirst())
                .orElse(null);
    }

    /** Whether a body (its core and flesh) is in this shape, or still small enough not to need one. */
    public boolean fits(Collection<BlockPos> body) {
        if (body.size() < minSize) {
            return true;
        }
        Measure m = Measure.of(body);
        return switch (shape) {
            case COMPACT -> m.fill() >= value && m.longest() <= 2 * m.shortest();
            case ELONGATED -> m.longest() >= value * m.middle();
            case BROAD -> Math.min(m.dx(), m.dz()) >= value * m.dy();
            case BRANCHING -> m.tips() >= value;
            case SPIRE -> m.dy() >= value * Math.max(m.dx(), m.dz());
        };
    }

    /** What the shape asks for, in words (the status line). */
    public Component describe() {
        return Component.translatable("organism_shape.chronogenesis." + shape.getSerializedName(),
                value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value), minSize);
    }

    /** A body's bounding sides, how full its box is, and how many tips it has. */
    public record Measure(int dx, int dy, int dz, double fill, int tips) {
        static Measure of(Collection<BlockPos> body) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            Set<BlockPos> set = new HashSet<>(body);
            for (BlockPos p : set) {
                minX = Math.min(minX, p.getX());
                minY = Math.min(minY, p.getY());
                minZ = Math.min(minZ, p.getZ());
                maxX = Math.max(maxX, p.getX());
                maxY = Math.max(maxY, p.getY());
                maxZ = Math.max(maxZ, p.getZ());
            }
            int dx = maxX - minX + 1;
            int dy = maxY - minY + 1;
            int dz = maxZ - minZ + 1;
            int tips = 0;
            for (BlockPos p : set) {
                int neighbours = 0;
                for (Direction direction : Direction.values()) {
                    if (set.contains(p.relative(direction))) {
                        neighbours++;
                    }
                }
                if (neighbours == 1) {
                    tips++;
                }
            }
            return new Measure(dx, dy, dz, set.size() / (double) (dx * dy * dz), tips);
        }

        int longest() {
            return Math.max(dx, Math.max(dy, dz));
        }

        int shortest() {
            return Math.min(dx, Math.min(dy, dz));
        }

        int middle() {
            return dx + dy + dz - longest() - shortest();
        }
    }
}
