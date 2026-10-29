package com.chronogenesis.world;

import java.util.List;
import java.util.Optional;

import com.chronogenesis.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * One template, centred on its chunk's middle (rotation turns it about its own centre), so that structures sharing a
 * structure set's placement stand on exactly the same spot in every dimension of the same seed:
 * <ul>
 *   <li>the overworld's obelisks and the Jurassic temples built around them;</li>
 *   <li>the obelisks again in the Ice Age, round the Jurassic temples' remains, and in the Precursor era, each
 *       in a shrine of the kingdom of its land.</li>
 * </ul>
 * The template and its rotation are the first things drawn from the chunk's random, so two structures with the same
 * {@code templates} list choose the same one, turned the same way, wherever they are. A {@code suffix} then places a
 * variant of it instead (a city's {@code _ruin}). Everything drawn after (whether it survives, how deep it lies) may
 * differ without moving it.
 *
 * @param templates what may stand here, weighted (when no choice below matches)
 * @param choices   what may stand here by the biome at the spot: the first whose biomes hold it decides
 * @param biomesOf  whose biomes decide: another dimension of the same seed (the Precursor era, for its ruins in the
 *                  present: both share the overworld's noise, so it can be asked what grows here there). Absent: this
 *                  dimension
 * @param rotate    turned at random, or always as drawn
 * @param suffix    appended to the chosen template's path: the variant actually placed
 * @param ground    the template's own y of its ground level (anything below it is foundations, cellars, a pit)
 * @param top       for buried templates: how far above its ground the template's top lies
 * @param buried    blocks of earth over its top (absent: it stands on the surface)
 * @param chance    how often it is there at all
 * @param foundations whether what it builds on its bottom layer reaches down to the ground (see {@link PlacedTemplatePiece})
 */
public class PlacedTemplateStructure extends Structure {
    /** Templates for a set of biomes. */
    public record Choice(HolderSet<Biome> biomes, SimpleWeightedRandomList<ResourceLocation> templates) {
        public static final Codec<Choice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(Choice::biomes),
                SimpleWeightedRandomList.wrappedCodec(ResourceLocation.CODEC).fieldOf("templates").forGetter(Choice::templates)
        ).apply(instance, Choice::new));
    }

    public static final MapCodec<PlacedTemplateStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            SimpleWeightedRandomList.wrappedCodecAllowingEmpty(ResourceLocation.CODEC)
                    .optionalFieldOf("templates", SimpleWeightedRandomList.empty()).forGetter(s -> s.templates),
            Choice.CODEC.listOf().optionalFieldOf("choices", List.of()).forGetter(s -> s.choices),
            ResourceKey.codec(Registries.LEVEL_STEM).optionalFieldOf("biomes_of").forGetter(s -> s.biomesOf),
            Codec.BOOL.optionalFieldOf("rotate", true).forGetter(s -> s.rotate),
            Codec.STRING.optionalFieldOf("suffix", "").forGetter(s -> s.suffix),
            Codec.INT.optionalFieldOf("ground", 0).forGetter(s -> s.ground),
            Codec.INT.optionalFieldOf("top", 0).forGetter(s -> s.top),
            IntProvider.CODEC.optionalFieldOf("buried").forGetter(s -> s.buried),
            Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 1.0f).forGetter(s -> s.chance),
            Codec.BOOL.optionalFieldOf("foundations", false).forGetter(s -> s.foundations)
    ).apply(instance, PlacedTemplateStructure::new));

    private final SimpleWeightedRandomList<ResourceLocation> templates;
    private final List<Choice> choices;
    private final Optional<ResourceKey<LevelStem>> biomesOf;
    private final boolean rotate;
    private final String suffix;
    private final int ground;
    private final int top;
    private final Optional<IntProvider> buried;
    private final float chance;
    private final boolean foundations;

    public PlacedTemplateStructure(StructureSettings settings, SimpleWeightedRandomList<ResourceLocation> templates,
            List<Choice> choices, Optional<ResourceKey<LevelStem>> biomesOf, boolean rotate, String suffix, int ground, int top,
            Optional<IntProvider> buried, float chance, boolean foundations) {
        super(settings);
        this.templates = templates;
        this.choices = choices;
        this.biomesOf = biomesOf;
        this.rotate = rotate;
        this.suffix = suffix;
        this.ground = ground;
        this.top = top;
        this.buried = buried;
        this.chance = chance;
        this.foundations = foundations;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        // First the template and its turn: the same in every dimension of this seed.
        Optional<ResourceLocation> chosen = templatesAt(context, x, surface, z).getRandomValue(random);
        Rotation rotation = rotate ? Rotation.getRandom(random) : Rotation.NONE;
        if (chosen.isEmpty() || (chance < 1.0f && random.nextFloat() >= chance)) {
            return Optional.empty();
        }
        ResourceLocation id = suffix.isEmpty() ? chosen.get() : chosen.get().withSuffix(suffix);
        int groundY = buried.map(depth -> surface - top - depth.sample(random)).orElse(surface);
        StructureTemplate template = context.structureTemplateManager().getOrCreate(id);
        Vec3i size = template.getSize();
        if (size.getX() == 0) {
            return Optional.empty(); // missing template
        }
        BlockPos origin = new BlockPos(x - size.getX() / 2, groundY - ground, z - size.getZ() / 2);
        return Optional.of(new GenerationStub(new BlockPos(x, groundY, z), builder -> builder.addPiece(
                new PlacedTemplatePiece(context.structureTemplateManager(), id, origin, rotation, foundations))));
    }

    /** The templates for this spot: by the biome there (in {@code biomesOf}'s dimension, if set). */
    private SimpleWeightedRandomList<ResourceLocation> templatesAt(GenerationContext context, int x, int y, int z) {
        if (choices.isEmpty()) {
            return templates;
        }
        BiomeSource source = context.biomeSource();
        if (biomesOf.isPresent()) {
            Optional<LevelStem> stem = context.registryAccess().registry(Registries.LEVEL_STEM).flatMap(r -> r.getOptional(biomesOf.get()));
            if (stem.isEmpty()) {
                return templates;
            }
            source = stem.get().generator().getBiomeSource();
        }
        Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                context.randomState().sampler());
        for (Choice choice : choices) {
            if (choice.biomes().contains(biome)) {
                return choice.templates();
            }
        }
        return templates;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.PLACED_TEMPLATE.get();
    }
}
