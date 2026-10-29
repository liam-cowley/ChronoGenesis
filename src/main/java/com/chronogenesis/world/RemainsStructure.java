package com.chronogenesis.world;

import java.util.Optional;

import com.chronogenesis.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * A Precursor city where the present has its remains: it stands exactly where (another dimension of the same seed,
 * the overworld) one of the {@code remains} structures stands. Its structure set must copy the remains' placement;
 * this asks the overworld's own generator whether the remains would really generate there (biome, terrain, the
 * monument's sea), and where.
 * <ul>
 *   <li>With {@code templates}: a city template is centred on the remains (on the land there, or {@code on_water}
 *       on the sea's surface), its bottom layer carried down to the ground ({@code foundations}).</li>
 *   <li>With {@code restore}: the remains themselves, as they were before they fell (the ancient cities: the same
 *       jigsaw, the same seed, so the same streets; then {@link RestoredPiece} mends them and brings their people and
 *       their guards back).</li>
 * </ul>
 *
 * @param remains    the present's structures this stands over (tried in order; the first that generates there)
 * @param of         the dimension whose remains they are
 * @param templates  the city placed over them (template mode)
 * @param ground     the template's own y of its ground (or of the sea's surface)
 * @param onWater    stand on the sea's surface where the ground is under water
 * @param restore    restore the remains instead of placing a template
 */
public class RemainsStructure extends Structure {
    /**
     * Mending fallen remains: their kingdom (for the folk who come back to them), and their guards.
     *
     * @param kingdom    the kingdom of the Precursors put back in them
     * @param guard      the creature that kept them, friendly to whoever keeps the peace (see PrecursorGuards)
     * @param folkChance chance per chunk of the city for each of a few townsfolk
     * @param guardChance chance per chunk of the city for a guard
     */
    public record Restoration(String kingdom, EntityType<?> guard, float folkChance, float guardChance) {
        public static final Codec<Restoration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("kingdom").forGetter(Restoration::kingdom),
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("guard").forGetter(Restoration::guard),
                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("folk_chance", 0.5f).forGetter(Restoration::folkChance),
                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("guard_chance", 0.1f).forGetter(Restoration::guardChance)
        ).apply(instance, Restoration::new));
    }

    public static final MapCodec<RemainsStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("remains").forGetter(s -> s.remains),
            ResourceKey.codec(Registries.LEVEL_STEM).optionalFieldOf("of", LevelStem.OVERWORLD).forGetter(s -> s.of),
            SimpleWeightedRandomList.wrappedCodecAllowingEmpty(ResourceLocation.CODEC)
                    .optionalFieldOf("templates", SimpleWeightedRandomList.empty()).forGetter(s -> s.templates),
            Codec.INT.optionalFieldOf("ground", 0).forGetter(s -> s.ground),
            Codec.BOOL.optionalFieldOf("on_water", false).forGetter(s -> s.onWater),
            Restoration.CODEC.optionalFieldOf("restore").forGetter(s -> s.restore)
    ).apply(instance, RemainsStructure::new));

    private final HolderSet<Structure> remains;
    private final ResourceKey<LevelStem> of;
    private final SimpleWeightedRandomList<ResourceLocation> templates;
    private final int ground;
    private final boolean onWater;
    private final Optional<Restoration> restore;

    public RemainsStructure(StructureSettings settings, HolderSet<Structure> remains, ResourceKey<LevelStem> of,
            SimpleWeightedRandomList<ResourceLocation> templates, int ground, boolean onWater, Optional<Restoration> restore) {
        super(settings);
        this.remains = remains;
        this.of = of;
        this.templates = templates;
        this.ground = ground;
        this.onWater = onWater;
        this.restore = restore;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Optional<StructureStart> found = remainsHere(context);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        StructureStart start = found.get();
        BoundingBox box = start.getBoundingBox();
        if (restore.isPresent()) {
            Restoration restoration = restore.get();
            BlockPos centre = box.getCenter();
            return Optional.of(new GenerationStub(centre, builder -> {
                start.getPieces().forEach(builder::addPiece);
                builder.addPiece(new RestoredPiece(box, restoration.kingdom(), restoration.guard(), restoration.folkChance(),
                        restoration.guardChance()));
            }));
        }
        Optional<ResourceLocation> chosen = templates.getRandomValue(context.random());
        Rotation rotation = Rotation.getRandom(context.random());
        if (chosen.isEmpty()) {
            return Optional.empty();
        }
        int x = (box.minX() + box.maxX()) / 2;
        int z = (box.minZ() + box.maxZ()) / 2;
        int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        if (onWater) {
            surface = Math.max(surface, context.chunkGenerator().getSeaLevel());
        }
        StructureTemplate template = context.structureTemplateManager().getOrCreate(chosen.get());
        Vec3i size = template.getSize();
        if (size.getX() == 0) {
            return Optional.empty(); // missing template
        }
        BlockPos origin = new BlockPos(x - size.getX() / 2, surface - ground, z - size.getZ() / 2);
        return Optional.of(new GenerationStub(new BlockPos(x, surface, z), builder -> builder.addPiece(
                new PlacedTemplatePiece(context.structureTemplateManager(), chosen.get(), origin, rotation, true))));
    }

    /** The remains as {@code of}'s own generator would make them at this chunk, if they stand here at all. */
    private Optional<StructureStart> remainsHere(GenerationContext context) {
        Optional<LevelStem> stem = context.registryAccess().registry(Registries.LEVEL_STEM).flatMap(r -> r.getOptional(of));
        if (stem.isEmpty()) {
            return Optional.empty();
        }
        ChunkGenerator generator = stem.get().generator();
        for (Holder<Structure> structure : remains) {
            // The same seed and the same noise: the random state of this dimension is the one of the remains'.
            StructureStart start = structure.value().generate(context.registryAccess(), generator, generator.getBiomeSource(),
                    context.randomState(), context.structureTemplateManager(), context.seed(), context.chunkPos(), 0,
                    context.heightAccessor(), structure.value().biomes()::contains);
            if (start.isValid()) {
                return Optional.of(start);
            }
        }
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.REMAINS.get();
    }
}
