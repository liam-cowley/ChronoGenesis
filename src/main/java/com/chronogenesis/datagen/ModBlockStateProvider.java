package com.chronogenesis.datagen;

import java.util.EnumMap;
import java.util.Map;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlock;
import com.chronogenesis.biocomputer.CortexLobeBlock;
import com.chronogenesis.biocomputer.DissectionTableBlock;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.organism.CorePhase;
import com.chronogenesis.organism.EssenceJugBlock;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.GlandBlock;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.temple.SealBlock;
import com.chronogenesis.temple.TemporalCoreBlock;
import com.chronogenesis.timetravel.FossilCortexBlock;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Block states and 3D block models.
 * <p>
 * Organisms are built from shared parts: a brass frame (the twelve edges of the armature, textured
 * with the Block of Brass),
 * flesh masses tinted by the organism's aspect (tint index 0), and untinted details (brass,
 * eyes, pustules). Flesh has four growth stages and cores have four phases; see {@link FleshBlock}
 * and {@link CorePhase}. Grafts are modelled pointing up; {@code directionalBlock} rotates them.
 */
public class ModBlockStateProvider extends BlockStateProvider {
    private static final String FLESH = "#flesh";
    private static final String FRAME = "#frame";
    /** Frames are made of brass: the same texture as the Block of Brass. */
    private static final net.minecraft.resources.ResourceLocation BRASS =
            ChronoGenesis.id("block/brass_block");

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, ChronoGenesis.MODID, fileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.BRASS_BLOCK.get(), cubeAll(ModBlocks.BRASS_BLOCK.get()));

        // Frame: the bare armature is the same twelve brass beams every organism grows on.
        BlockModelBuilder armature = models().withExistingParent("brass_armature", mcLoc("block/block"))
                .texture("frame", BRASS)
                .texture("particle", BRASS)
                .renderType("cutout");
        frame(armature);
        simpleBlockWithItem(ModBlocks.BRASS_ARMATURE.get(), armature);

        fleshStages();
        corePhases();
        grafts();
        conduit();
        jug();
        timeTear();
        chronocortex();
        fadedBlueprint();
        obelisk();
    }

    // ---------------------------------------------------------------- flesh

    /**
     * Flesh looks different for every essence, not only in colour: each grows over its frame in its own way.
     * <ul>
     *   <li>Density: heavy, it sags to the bottom of the frame, cinched with iron straps.</li>
     *   <li>Speed: taut tendons strung corner to corner, then a striated, ribbed muscle.</li>
     *   <li>Size: bubbles that swell and spill out past the frame.</li>
     *   <li>Awareness: folded like a brain, with eyes opening in it.</li>
     *   <li>Time: amethyst shards grow through it; its skin shows growth rings, like a tree's.</li>
     * </ul>
     */
    private void fleshStages() {
        Map<Aspect, ModelFile[]> byAspect = new EnumMap<>(Aspect.class);
        for (Aspect aspect : Aspect.values()) {
            byAspect.put(aspect, fleshFor(aspect));
        }
        getVariantBuilder(ModBlocks.CHRONOFLESH.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(byAspect.get(state.getValue(FleshBlock.ASPECT))[state.getValue(FleshBlock.GROWTH)]).build());
        itemModels().withExistingParent("chronoflesh", modLoc("block/chronoflesh_density_growth_3"));
    }

    private ModelFile[] fleshFor(Aspect aspect) {
        String a = aspect.getSerializedName();
        ModelFile[] stages = new ModelFile[4];
        for (int growth = 0; growth < 4; growth++) {
            BlockModelBuilder model = organismModel("chronoflesh_" + a + "_growth_" + growth, "chronoflesh_" + a);
            parts(model);
            if (growth < FleshBlock.MATURE) {
                frame(model);
            } else {
                model.texture("overlay", modLoc("block/chronoflesh_" + a + "_overlay"));
                model.element().allFaces((dir, face) -> face.texture(FLESH).cullface(dir).tintindex(0)).end();
                model.element().allFaces((dir, face) -> face.texture("#overlay").cullface(dir)).end();
            }
            switch (aspect) {
                case DENSITY -> densityFlesh(model, growth);
                case SPEED -> speedFlesh(model, growth);
                case SIZE -> sizeFlesh(model, growth);
                case AWARENESS -> awarenessFlesh(model, growth);
                case TIME -> timeFlesh(model, growth);
            }
            stages[growth] = model;
        }
        return stages;
    }

    /** Textures for the parts some essences grow: iron straps, amethyst shards, small eyes, pustules. */
    private BlockModelBuilder parts(BlockModelBuilder model) {
        return model.texture("steel", modLoc("block/steel"))
                .texture("crystal", mcLoc("block/amethyst_block"))
                .texture("eye", modLoc("block/eye"))
                .texture("pustule", modLoc("block/flesh_pustule"));
    }

    private static void densityFlesh(BlockModelBuilder m, int growth) {
        switch (growth) {
            case 0 -> {
                box(m, 1, 0, 1, 15, 1.5f, 15, FLESH, true);         // a puddle, pooled on the floor
                box(m, 0, 0, 0, 16, 3, 3, FLESH, true);
                box(m, 0, 0, 13, 16, 3, 16, FLESH, true);
            }
            case 1 -> {
                box(m, 1, 0, 1, 15, 6, 15, FLESH, true);            // a heavy slab
                box(m, 3, 6, 3, 6, 7.5f, 6, FLESH, true);
                box(m, 0.5f, 2, 0.5f, 15.5f, 3, 15.5f, "#steel", false);
            }
            case 2 -> {
                box(m, 1, 0, 1, 15, 11, 15, FLESH, true);           // squat, straining sideways at its base
                box(m, 0.5f, 0, 3, 1, 5, 13, FLESH, true);
                box(m, 15, 0, 3, 15.5f, 5, 13, FLESH, true);
                box(m, 0.5f, 4, 0.5f, 15.5f, 5, 15.5f, "#steel", false);
                box(m, 0.5f, 8, 0.5f, 15.5f, 9, 15.5f, "#steel", false);
            }
            default -> {
                box(m, -0.5f, 3, -0.5f, 16.5f, 4.5f, 16.5f, "#steel", false);   // iron straps round the block
                box(m, -0.5f, 11, -0.5f, 16.5f, 12.5f, 16.5f, "#steel", false);
            }
        }
    }

    private static void speedFlesh(BlockModelBuilder m, int growth) {
        switch (growth) {
            case 0 -> {
                strand(m, 8, 1, Direction.Axis.Z, 45);
                strand(m, 8, 15, Direction.Axis.Z, -45);
                strand(m, 1, 8, Direction.Axis.X, 45);
            }
            case 1 -> {
                strand(m, 4, 4, Direction.Axis.Z, 45);
                strand(m, 12, 12, Direction.Axis.Z, -45);
                strand(m, 4, 12, Direction.Axis.X, 45);
                strand(m, 12, 4, Direction.Axis.X, -45);
                box(m, 6, 6, 6, 10, 10, 10, FLESH, true);           // where the tendons knot
            }
            case 2 -> {
                box(m, 3, 1, 3, 13, 15, 13, FLESH, true);
                for (float x : new float[] {4, 7.5f, 11}) {           // striated ribs down every side
                    box(m, x, 1, 2, x + 1, 15, 3, FLESH, true);
                    box(m, x, 1, 13, x + 1, 15, 14, FLESH, true);
                    box(m, 2, 1, x, 3, 15, x + 1, FLESH, true);
                    box(m, 13, 1, x, 14, 15, x + 1, FLESH, true);
                }
            }
            default -> {
                for (float x : new float[] {3, 7.5f, 12}) {           // raised tendons, taut, top to bottom
                    box(m, x, 0, -0.75f, x + 1, 16, 0, FLESH, true);
                    box(m, x, 0, 16, x + 1, 16, 16.75f, FLESH, true);
                    box(m, -0.75f, 0, x, 0, 16, x + 1, FLESH, true);
                    box(m, 16, 0, x, 16.75f, 16, x + 1, FLESH, true);
                }
            }
        }
    }

    /** A thin tendon strung diagonally across the frame. */
    private static void strand(BlockModelBuilder m, float a, float b, Direction.Axis axis, float angle) {
        var element = axis == Direction.Axis.Z ? m.element().from(a - 0.75f, 0.5f, b - 0.75f).to(a + 0.75f, 15.5f, b + 0.75f)
                : m.element().from(b - 0.75f, 0.5f, a - 0.75f).to(b + 0.75f, 15.5f, a + 0.75f);
        element.rotation().angle(angle).axis(axis).origin(8, 8, 8).end()
                .allFaces((dir, face) -> face.texture(FLESH).tintindex(0)).end();
    }

    private static void sizeFlesh(BlockModelBuilder m, int growth) {
        switch (growth) {
            case 0 -> {
                box(m, 4, 0, 4, 10, 6, 10, FLESH, true);            // a few bubbles
                box(m, 10, 1, 9, 13, 4, 12, FLESH, true);
                box(m, 2, 13, 3, 6, 16, 7, FLESH, true);
            }
            case 1 -> {
                box(m, 2, 1, 2, 14, 13, 14, FLESH, true);
                box(m, 5, 13, 5, 10, 15.5f, 10, FLESH, true);
                box(m, 9, 9, 13.5f, 13, 13, 15, FLESH, true);
            }
            case 2 -> {
                box(m, 1, 0, 1, 15, 15, 15, FLESH, true);           // spilling out between the beams
                box(m, 4, 4, -1.5f, 12, 12, 1, FLESH, true);
                box(m, 15, 3, 4, 17.5f, 11, 12, FLESH, true);
                box(m, 5, 15, 5, 11, 17, 11, FLESH, true);
            }
            default -> {
                box(m, 3, 16, 3, 9, 18.5f, 9, FLESH, true);         // bulbs swelling out of the block
                box(m, -2, 3, 8, 0, 8, 13, FLESH, true);
                box(m, 10, 5, 16, 14, 9, 18, FLESH, true);
                box(m, 16, 10, 2, 17.5f, 13, 5, FLESH, true);
            }
        }
    }

    private static void awarenessFlesh(BlockModelBuilder m, int growth) {
        switch (growth) {
            case 0 -> {
                box(m, 0, 0, 0, 16, 2.5f, 2.5f, FLESH, true);       // nerve fibres along the beams
                box(m, 0, 0, 0, 2.5f, 16, 2.5f, FLESH, true);
                box(m, 13.5f, 13.5f, 0, 16, 16, 16, FLESH, true);
                box(m, 7, 0, 7, 9, 2, 9, "#eye", false);
            }
            case 1 -> {
                box(m, 3, 0, 3, 13, 4, 13, FLESH, true);            // folds stacked like gyri
                box(m, 4, 4, 2, 12, 8, 14, FLESH, true);
                box(m, 2, 8, 4, 14, 11, 12, FLESH, true);
                box(m, 6, 11, 6, 10, 12.5f, 10, "#eye", false);
            }
            case 2 -> {
                box(m, 2, 1, 2, 14, 14, 14, FLESH, true);
                box(m, 3, 14, 5, 13, 15, 7, FLESH, true);
                box(m, 3, 14, 9, 13, 15, 11, FLESH, true);
                box(m, 5, 8, 13.5f, 8, 11, 15, "#eye", false);
                box(m, 13.5f, 4, 7, 15, 7, 10, "#eye", false);
            }
            default -> {
                box(m, 4, 9, -1, 7, 12, 0, "#eye", false);          // eyes opening on every side
                box(m, 16, 4, 9, 17, 7, 12, "#eye", false);
                box(m, 9, 16, 4, 12, 17, 7, "#eye", false);
                box(m, 10, 3, 16, 13, 6, 17, "#eye", false);
                box(m, -1, 10, 3, 0, 13, 6, "#eye", false);
            }
        }
    }

    private static void timeFlesh(BlockModelBuilder m, int growth) {
        switch (growth) {
            case 0 -> {
                box(m, 3, 0, 3, 8, 3, 8, FLESH, true);
                shard(m, 5, 0, 5, 7);
                shard(m, 12, 0, 11, 5);
            }
            case 1 -> {
                box(m, 4, 0, 4, 12, 7, 12, FLESH, true);
                shard(m, 6, 7, 6, 6);
                shard(m, 11, 4, 10, 7);
                shard(m, 3, 0, 12, 6);
            }
            case 2 -> {
                box(m, 2, 0, 2, 14, 13, 14, FLESH, true);
                shard(m, 5, 13, 5, 5);
                shard(m, 10, 13, 9, 4);
                shard(m, 8, 10, 13, 6);
            }
            default -> {
                shard(m, 4, 15, 4, 6);                              // shards breaking through the skin
                shard(m, 11, 15, 10, 4);
                shard(m, 8, 15, 13, 5);
                shard(m, 13, 7, 3, 5);
            }
        }
    }

    /** An amethyst shard: two crossed crystal planes. */
    private static void shard(BlockModelBuilder m, float x, float y, float z, float height) {
        for (float angle : new float[] {45, -45}) {
            m.element().from(x - 2, y, z - 0.5f).to(x + 2, y + height, z + 0.5f)
                    .rotation().angle(angle).axis(Direction.Axis.Y).origin(x, y, z).end()
                    .allFaces((dir, face) -> face.texture("#crystal")).end();
        }
    }

    // ---------------------------------------------------------------- core

    private void corePhases() {
        // The cores' living parts (the beating mass, the eye, each essence's growths) are drawn animated by
        // client/OrganismCoreRenderer; the model is what does not move: the frame, and the seed's brass cradle.
        BlockModelBuilder seed = organismModel("organism_core_seed", "organism_core")
                .texture("brass", modLoc("block/brass_fitting"));
        frame(seed);
        box(seed, 3, 0, 3, 13, 2, 13, "#brass", false);
        Map<Aspect, ModelFile> grown = new EnumMap<>(Aspect.class);
        for (Aspect aspect : Aspect.values()) {
            String a = aspect.getSerializedName();
            BlockModelBuilder frameOnly = organismModel("organism_core_" + a + "_awake", "organism_core_" + a)
                    .texture("brass", modLoc("block/brass_fitting"));
            frame(frameOnly);
            // A cradle of brass the mass sits in, and veins down the frame's posts.
            box(frameOnly, 3, 0, 3, 13, 1, 13, "#brass", false);
            grown.put(aspect, frameOnly);
        }
        BlockModelBuilder necrotic = organismModel("organism_core_necrotic", "organism_core_necrotic");
        frame(necrotic);
        box(necrotic, 3, 0, 3, 13, 1, 13, FLESH, false);

        getVariantBuilder(ModBlocks.ORGANISM_CORE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(switch (state.getValue(OrganismCoreBlock.PHASE)) {
                    case SEED -> seed;
                    case AWAKE, HUNGRY -> grown.get(state.getValue(FleshBlock.ASPECT));
                    case NECROTIC -> necrotic;
                }).build());
    }

    // ---------------------------------------------------------------- grafts

    private void grafts() {
        // Tap: a brass pipe out of a flesh mound, with a spout and a valve wheel.
        BlockModelBuilder tap = graftModel("tap", "graft_flesh");
        box(tap, 4, 0, 4, 12, 3, 12, FLESH, false);
        box(tap, 7, 3, 7, 9, 11, 9, "#brass", false);
        box(tap, 9, 8, 7, 13, 10, 9, "#brass", false);
        box(tap, 11, 6, 7, 13, 8, 9, "#brass", false);
        box(tap, 5, 11, 5, 11, 12, 6, "#brass", false);
        box(tap, 5, 11, 10, 11, 12, 11, "#brass", false);
        box(tap, 5, 11, 6, 6, 12, 10, "#brass", false);
        box(tap, 10, 11, 6, 11, 12, 10, "#brass", false);
        box(tap, 6, 11, 7.5f, 10, 12, 8.5f, "#brass", false);
        box(tap, 7.5f, 11, 6, 8.5f, 12, 10, "#brass", false);
        box(tap, 7, 11, 7, 9, 12.5f, 9, "#brass", false);
        graftBlock(ModBlocks.TAP.get(), tap);

        // Leech Tendril: a ringed tendril bending out of the flesh, ending in a lamprey maw.
        BlockModelBuilder leech = graftModel("leech_tendril", "leech_skin")
                .texture("maw", modLoc("block/leech_tendril"));
        box(leech, 4, 0, 4, 12, 3, 12, FLESH, false);
        box(leech, 5.5f, 3, 5.5f, 10.5f, 7, 10.5f, FLESH, false);
        leech.element().from(6, 7, 6).to(10, 10, 10)
                .rotation().angle(22.5f).axis(Direction.Axis.X).origin(8, 7, 8).end()
                .allFaces((dir, face) -> face.texture(FLESH)).end();
        leech.element().from(5, 10, 6.5f).to(11, 13, 12.5f)
                .rotation().angle(22.5f).axis(Direction.Axis.X).origin(8, 10, 9.5f).end()
                .allFaces((dir, face) -> face.texture(dir == Direction.UP ? "#maw" : FLESH)).end();
        // Gulping: the stalk bulges and the maw gapes as the stream of essence arrives.
        BlockModelBuilder gulping = graftModel("leech_tendril_gulping", "leech_skin", "leech_tendril")
                .texture("maw", modLoc("block/leech_tendril"));
        box(gulping, 4, 0, 4, 12, 3, 12, FLESH, false);
        box(gulping, 4.5f, 3, 4.5f, 11.5f, 8, 11.5f, FLESH, false);
        gulping.element().from(5.5f, 7, 5.5f).to(10.5f, 10.5f, 10.5f)
                .rotation().angle(22.5f).axis(Direction.Axis.X).origin(8, 7, 8).end()
                .allFaces((dir, face) -> face.texture(FLESH)).end();
        gulping.element().from(4, 10, 5.5f).to(12, 14, 13.5f)
                .rotation().angle(22.5f).axis(Direction.Axis.X).origin(8, 10, 9.5f).end()
                .allFaces((dir, face) -> face.texture(dir == Direction.UP ? "#maw" : FLESH)).end();
        graftBlock(ModBlocks.LEECH_TENDRIL.get(), leech, gulping);

        glands();

        // Maw: a puckered mouth on a mound, ringed with brass, open towards the vein it drinks from.
        BlockModelBuilder maw = graftModel("maw", "graft_flesh")
                .texture("mouth", modLoc("block/maw"));
        box(maw, 3, 0, 3, 13, 3, 13, FLESH, false);
        box(maw, 4, 3, 4, 12, 6, 12, FLESH, false);
        maw.element().from(4, 6, 4).to(12, 7, 12)
                .allFaces((dir, face) -> face.texture(dir == Direction.UP ? "#mouth" : "#brass")).end();
        graftBlock(ModBlocks.MAW.get(), maw);

        // Stasis Bladder: a swollen membrane sac, veined and dark, cinched with a brass band.
        BlockModelBuilder bladder = graftModel("stasis_bladder", "graft_flesh")
                .texture("sac", modLoc("block/stasis_bladder"));
        box(bladder, 4, 0, 4, 12, 2, 12, FLESH, false);
        box(bladder, 3, 2, 3, 13, 9, 13, "#sac", false);
        box(bladder, 4, 9, 4, 12, 12, 12, "#sac", false);
        box(bladder, 2.5f, 5, 2.5f, 13.5f, 6, 13.5f, "#brass", false);
        box(bladder, 6, 12, 6, 10, 13, 10, "#sac", false);
        graftBlock(ModBlocks.STASIS_BLADDER.get(), bladder);

        // Starting Gate: a brass gate on a fleshy plinth, with a pace gauge.
        BlockModelBuilder gate = graftModel("starting_gate", "graft_flesh")
                .texture("gauge", modLoc("block/starting_gate"));
        box(gate, 3, 0, 3, 13, 3, 13, FLESH, false);
        box(gate, 3, 3, 7, 5, 13, 9, "#brass", false);
        box(gate, 11, 3, 7, 13, 13, 9, "#brass", false);
        box(gate, 3, 11, 7, 13, 13, 9, "#brass", false);
        box(gate, 5, 5, 7.5f, 11, 11, 8.5f, "#gauge", false);
        graftBlock(ModBlocks.STARTING_GATE.get(), gate);

        // Finish: a brass bell-mouthed funnel on a plinth, where time condenses.
        BlockModelBuilder finish = graftModel("finish", "graft_flesh")
                .texture("gauge", modLoc("block/finish"));
        box(finish, 3, 0, 3, 13, 3, 13, FLESH, false);
        box(finish, 6, 3, 6, 10, 7, 10, "#brass", false);
        box(finish, 4, 7, 4, 12, 9, 12, "#brass", false);
        box(finish, 3, 9, 3, 13, 10, 13, "#brass", false);
        box(finish, 5, 8.5f, 5, 11, 9.5f, 11, "#gauge", false);
        graftBlock(ModBlocks.FINISH.get(), finish);

        // Heart: a clenched lump of muscle on a mound, a brass valve stitched into its top.
        BlockModelBuilder heart = graftModel("heart", "graft_flesh")
                .texture("muscle", modLoc("block/heart"));
        box(heart, 4, 0, 4, 12, 2, 12, FLESH, false);
        box(heart, 3, 2, 4, 13, 9, 12, "#muscle", false);
        box(heart, 4, 9, 5, 11, 11, 11, "#muscle", false);
        box(heart, 9, 9, 7, 11, 13, 9, "#brass", false);
        box(heart, 5, 9, 6, 7, 12, 8, "#muscle", false);
        graftBlock(ModBlocks.HEART.get(), heart);

        // Eye: a lidded eyeball on a fleshy stalk, staring out of the organism.
        BlockModelBuilder eye = graftModel("eye", "graft_flesh")
                .texture("eye", modLoc("block/eye"));
        box(eye, 4, 0, 4, 12, 2, 12, FLESH, false);
        box(eye, 6, 2, 6, 10, 5, 10, FLESH, false);
        eye.element().from(3.5f, 5, 3.5f).to(12.5f, 10, 12.5f)
                .allFaces((dir, face) -> face.texture(dir == Direction.UP ? "#eye" : FLESH)).end();
        graftBlock(ModBlocks.EYE.get(), eye);

        // Clock: a brass-cased clock face on a plinth. Bury one in Density flesh, keep its twin free.
        BlockModelBuilder clock = graftModel("clock", "graft_flesh")
                .texture("face", modLoc("block/clock"));
        box(clock, 3, 0, 3, 13, 3, 13, FLESH, false);
        clock.element().from(3, 3, 3).to(13, 6, 13)
                .allFaces((dir, face) -> face.texture(dir == Direction.UP ? "#face" : "#brass")).end();
        box(clock, 7.5f, 6, 7.5f, 8.5f, 7, 8.5f, "#brass", false);
        graftBlock(ModBlocks.CLOCK.get(), clock);

        // Ganglion: a knot of nerve tissue on a mound, fibres reaching out across the flesh.
        BlockModelBuilder ganglion = graftModel("ganglion", "graft_flesh")
                .texture("nerve", modLoc("block/ganglion"));
        box(ganglion, 4, 0, 4, 12, 2, 12, FLESH, false);
        box(ganglion, 5, 2, 5, 11, 7, 11, "#nerve", false);
        box(ganglion, 6, 7, 6, 10, 9, 10, "#nerve", false);
        box(ganglion, 1, 0, 7, 4, 1, 9, "#nerve", false);
        box(ganglion, 12, 0, 7, 15, 1, 9, "#nerve", false);
        box(ganglion, 7, 0, 1, 9, 1, 4, "#nerve", false);
        graftBlock(ModBlocks.GANGLION.get(), ganglion);

        // Temporal Field: a teal dome ringed with brass, a slowly turning spiral of time inside.
        BlockModelBuilder field = graftModel("temporal_field", "graft_flesh")
                .texture("dome", modLoc("block/temporal_field"));
        box(field, 3, 0, 3, 13, 2, 13, FLESH, false);
        box(field, 2.5f, 2, 2.5f, 13.5f, 3, 13.5f, "#brass", false);
        box(field, 4, 3, 4, 12, 7, 12, "#dome", false);
        box(field, 5.5f, 7, 5.5f, 10.5f, 9, 10.5f, "#dome", false);
        box(field, 7.5f, 9, 7.5f, 8.5f, 11, 8.5f, "#brass", false);
        graftBlock(ModBlocks.TEMPORAL_FIELD.get(), field);
    }

    /**
     * The Enhancing Gland: a sac on a mound with a brass port and a needle. It swells with each level (a second
     * lobe, then a third, thick veins over it); a Confluence Needle in its port is a twin needle in a brass Y.
     */
    private void glands() {
        ModelFile[][] models = new ModelFile[GlandBlock.MAX_LEVEL + 1][2];
        for (int level = 1; level <= GlandBlock.MAX_LEVEL; level++) {
            for (int needle = 0; needle < 2; needle++) {
                String name = "enhancing_gland" + (level > 1 ? "_" + level : "") + (needle == 1 ? "_needle" : "");
                BlockModelBuilder gland = graftModel(name, "graft_flesh", "enhancing_gland")
                        .texture("sac", modLoc("block/gland_sac"))
                        .texture("vein", modLoc("block/vein"));
                float top = glandSac(gland, level);
                box(gland, 6.5f, top, 6.5f, 9.5f, top + 1.5f, 9.5f, "#brass", false);
                if (needle == 0) {
                    box(gland, 7.5f, top + 1.5f, 7.5f, 8.5f, top + 4, 8.5f, "#steel", false);
                } else {
                    // The confluence: two needles splayed from a brass joint.
                    box(gland, 7, top + 1.5f, 7, 9, top + 2.5f, 9, "#brass", false);
                    for (float angle : new float[] {22.5f, -22.5f}) {
                        gland.element().from(7.5f, top + 2.5f, 7.5f).to(8.5f, top + 6, 8.5f)
                                .rotation().angle(angle).axis(Direction.Axis.X).origin(8, top + 2.5f, 8).end()
                                .allFaces((dir, face) -> face.texture("#steel")).end();
                    }
                }
                models[level][needle] = gland;
            }
        }
        getVariantBuilder(ModBlocks.ENHANCING_GLAND.get()).forAllStates(state -> {
            Direction dir = state.getValue(GraftBlock.FACING);
            return ConfiguredModel.builder()
                    .modelFile(models[state.getValue(GlandBlock.LEVEL)][state.getValue(GlandBlock.NEEDLE) ? 1 : 0])
                    .rotationX(dir == Direction.DOWN ? 180 : dir.getAxis().isHorizontal() ? 90 : 0)
                    .rotationY(dir.getAxis().isVertical() ? 0 : ((int) dir.toYRot() + 180) % 360)
                    .build();
        });
        itemModels().withExistingParent("enhancing_gland", modLoc("block/enhancing_gland"));
    }

    /** The gland's sac at a level; returns the height of its top, where the port sits. */
    private static float glandSac(BlockModelBuilder gland, int level) {
        box(gland, 3, 0, 3, 13, 2, 13, FLESH, false);
        switch (level) {
            case 1 -> {
                box(gland, 4, 2, 4, 12, 4, 12, "#sac", false);
                box(gland, 3, 4, 3, 13, 8, 13, "#sac", false);
                box(gland, 4, 8, 4, 12, 10, 12, "#sac", false);
                return 10;
            }
            case 2 -> {
                box(gland, 3, 2, 3, 13, 9, 13, "#sac", false);
                box(gland, 2, 4, 5, 3, 8, 11, "#sac", false);        // a second lobe bulging out
                box(gland, 9, 3, 12.5f, 14, 7, 14.5f, "#sac", false);
                box(gland, 4, 9, 4, 12, 11, 12, "#sac", false);
                box(gland, 7.5f, 2, 2.75f, 8.5f, 9, 3, "#vein", false);
                return 11;
            }
            default -> {
                box(gland, 2, 2, 2, 14, 10, 14, "#sac", false);
                box(gland, 1, 4, 4, 2, 9, 12, "#sac", false);
                box(gland, 14, 3, 4, 15, 9, 12, "#sac", false);
                box(gland, 4, 3, 14, 12, 8, 15, "#sac", false);
                box(gland, 3, 10, 3, 13, 12, 13, "#sac", false);
                box(gland, 5, 12, 5, 11, 13, 11, "#sac", false);
                box(gland, 7.5f, 2, 1.75f, 8.5f, 11, 2, "#vein", false);
                box(gland, 13.75f, 2, 7.5f, 14, 10, 8.5f, "#vein", false);
                box(gland, 1.75f, 3, 7.5f, 2, 10, 8.5f, "#vein", false);
                return 13;
            }
        }
    }

    // ---------------------------------------------------------------- the Chronocortex

    /** The Dissection Table (with or without its brain in a jar) and the woken Chronocortex built on it. */
    private void chronocortex() {
        BlockModelBuilder empty = tableModel("dissection_table");
        BlockModelBuilder withBrain = tableModel("dissection_table_brain");
        jar(withBrain, 4.5f, 11.5f, 15);
        getVariantBuilder(ModBlocks.DISSECTION_TABLE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(DissectionTableBlock.BRAIN) ? withBrain : empty).build());
        simpleBlockItem(ModBlocks.DISSECTION_TABLE.get(), empty);

        // Stage 0: a great jar, the brain swollen inside, electrodes through the brass lid.
        BlockModelBuilder cortex = tableModel("chronocortex");
        jar(cortex, 3, 13, 16);
        box(cortex, 5, 16, 7.5f, 6, 19, 8.5f, "#steel", false);
        box(cortex, 10, 16, 7.5f, 11, 19, 8.5f, "#steel", false);
        // Stage 1: the brain fills the jar to the glass; more electrodes.
        BlockModelBuilder child = tableModel("chronocortex_stage_1");
        box(child, 3.4f, 9, 3.4f, 12.6f, 14.5f, 12.6f, "#brain", false);
        box(child, 3.25f, 9, 3.25f, 12.75f, 14.5f, 12.75f, "#fluid", false);
        box(child, 3, 9, 3, 13, 15, 13, "#glass", false);
        box(child, 2.5f, 15, 2.5f, 13.5f, 16, 13.5f, "#brass", false);
        for (float[] e : new float[][] {{5, 7.5f}, {10, 7.5f}, {7.5f, 5}, {7.5f, 10}}) {
            box(child, e[0], 16, e[1], e[0] + 1, 19, e[1] + 1, "#steel", false);
        }
        // Stage 2 and 3: the lid is gone; the brain swells up out of the jar, wired every which way.
        BlockModelBuilder[] grown = new BlockModelBuilder[2];
        for (int i = 0; i < 2; i++) {
            BlockModelBuilder model = tableModel("chronocortex_stage_" + (i + 2));
            float crown = i == 0 ? 19 : 22;
            box(model, 3, 9, 3, 13, 14, 13, "#glass", false);
            box(model, 3.25f, 9, 3.25f, 12.75f, 14, 12.75f, "#brain", false);
            box(model, 2, 14, 2, 14, crown - 2, 14, "#brain", false);
            box(model, 4, crown - 2, 4, 12, crown, 12, "#brain", false);
            box(model, 2.5f, 13, 2.5f, 13.5f, 14, 13.5f, "#brass", false);
            for (float[] e : new float[][] {{1, 7.5f}, {14, 7.5f}, {7.5f, 1}, {7.5f, 14}}) {
                box(model, e[0], 13, e[1], e[0] + 1, crown - 1, e[1] + 1, "#steel", false);
            }
            grown[i] = model;
        }
        ModelFile[] stages = {cortex, child, grown[0], grown[1]};
        getVariantBuilder(ModBlocks.CHRONOCORTEX.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(stages[state.getValue(ChronocortexBlock.STAGE)]).build());
        simpleBlockItem(ModBlocks.CHRONOCORTEX.get(), cortex);
        lobes();
        offshoots();
        jurassic();
    }

    /**
     * Cortex lobes: a soft fold of brain in its brass frame, then firm grey-pink gyri. Formed into the Cerebrum,
     * the folds deepen and are stapled with brass, and an eye opens in the middle of each face of the cranium.
     */
    private void lobes() {
        BlockModelBuilder fresh = organismModel("cortex_lobe_fresh", "cortex_lobe");
        frame(fresh);
        box(fresh, 1.5f, 1.5f, 1.5f, 14.5f, 14.5f, 14.5f, FLESH, false);
        ModelFile mature = models().cubeAll("cortex_lobe", modLoc("block/cortex_lobe"));
        ModelFile cerebrum = models().cubeAll("cerebrum", modLoc("block/cerebrum"));
        ModelFile eye = models().cubeAll("cerebrum_eye", modLoc("block/cerebrum_eye"));
        getVariantBuilder(ModBlocks.CORTEX_LOBE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(switch (state.getValue(CortexLobeBlock.PART)) {
                    case FRESH -> fresh;
                    case MATURE -> mature;
                    case CEREBRUM -> cerebrum;
                    case CEREBRUM_EYE -> eye;
                }).build());
    }

    /**
     * The Cortex Offshoot (a bud of brain in a small jar, roots through the glass) and the Fossil Cortex it becomes:
     * a petrified brain on a slab of stone, its veins dark until it is linked, then lit.
     */
    private void offshoots() {
        BlockModelBuilder offshoot = models().withExistingParent("cortex_offshoot", mcLoc("block/block"))
                .texture("brain", modLoc("block/brain_tissue"))
                .texture("fluid", modLoc("block/cortex_fluid"))
                .texture("glass", modLoc("block/cortex_glass"))
                .texture("brass", modLoc("block/brass_fitting"))
                .texture("root", modLoc("block/vein"))
                .texture("particle", modLoc("block/cortex_glass"))
                .renderType("translucent");
        box(offshoot, 6, 1, 6, 10, 5, 10, "#brain", false);
        box(offshoot, 5.25f, 0.25f, 5.25f, 10.75f, 6.5f, 10.75f, "#fluid", false);
        box(offshoot, 5, 0, 5, 11, 7, 11, "#glass", false);
        box(offshoot, 4.5f, 7, 4.5f, 11.5f, 8, 11.5f, "#brass", false);
        box(offshoot, 7.5f, 8, 7.5f, 8.5f, 10, 8.5f, "#root", false);
        box(offshoot, 3, 0, 7.5f, 5, 0.5f, 8.5f, "#root", false);
        box(offshoot, 11, 0, 7.5f, 13, 0.5f, 8.5f, "#root", false);
        box(offshoot, 7.5f, 0, 2, 8.5f, 0.5f, 5, "#root", false);
        simpleBlock(ModBlocks.CORTEX_OFFSHOOT.get(), offshoot);

        BlockModelBuilder[] fossils = new BlockModelBuilder[2];
        for (int awake = 0; awake < 2; awake++) {
            BlockModelBuilder fossil = models().withExistingParent(awake == 1 ? "fossil_cortex_awake" : "fossil_cortex", mcLoc("block/block"))
                    .texture("slab", mcLoc("block/chiseled_deepslate"))
                    .texture("side", mcLoc("block/polished_deepslate"))
                    .texture("brain", modLoc(awake == 1 ? "block/fossil_cortex_awake" : "block/fossil_cortex"))
                    .texture("particle", modLoc("block/fossil_cortex"));
            fossil.element().from(0, 0, 0).to(16, 6, 16)
                    .allFaces((dir, face) -> face.texture(dir.getAxis().isVertical() ? "#slab" : "#side")).end();
            box(fossil, 2, 6, 2, 14, 13, 14, "#brain", false);
            box(fossil, 4, 13, 3, 12, 15, 13, "#brain", false);
            box(fossil, 6.5f, 15, 6, 9.5f, 16, 10, "#brain", false);
            fossils[awake] = fossil;
        }
        getVariantBuilder(ModBlocks.FOSSIL_CORTEX.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(fossils[state.getValue(FossilCortexBlock.AWAKE) ? 1 : 0]).build());
        simpleBlockItem(ModBlocks.FOSSIL_CORTEX.get(), fossils[0]);
    }

    /**
     * The Jurassic Age's blocks: the buckled harness (leather straps and brass buckles), the Rider's Seat, the
     * temple's Temporal Core (an amethyst engine in a gold cage; dull once stilled) and its five seals (each with its
     * glyph; the veiled one is plain temple wall). A broken seal is cracked blackstone.
     */
    /** Aeon Ice, and the dinosaur egg: an egg on the ground, cracking as it fills with Time. */
    private void iceAndEggs() {
        simpleBlockWithItem(ModBlocks.AEON_ICE.get(), models().cubeAll("aeon_ice", modLoc("block/aeon_ice")));
        ModelFile[] eggs = new ModelFile[4];
        for (int i = 0; i < 4; i++) {
            BlockModelBuilder egg = models().withExistingParent("dino_egg_" + i, mcLoc("block/block"))
                    .texture("egg", modLoc("block/dino_egg_" + i)).texture("particle", modLoc("block/dino_egg_" + i));
            box(egg, 5, 0, 5, 11, 1, 11, "#egg", false);
            box(egg, 4, 1, 4, 12, 7, 12, "#egg", false);
            box(egg, 5, 7, 5, 11, 10, 11, "#egg", false);
            box(egg, 6.5f, 10, 6.5f, 9.5f, 11, 9.5f, "#egg", false);
            eggs[i] = egg;
        }
        getVariantBuilder(ModBlocks.DINO_EGG.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(eggs[state.getValue(com.chronogenesis.entity.DinoEggBlock.CRACKS)]).build());
    }

    private void jurassic() {
        iceAndEggs();
        simpleBlock(ModBlocks.BUCKLED_HARNESS.get(), models().cubeBottomTop("buckled_harness", modLoc("block/harness_side"),
                modLoc("block/harness_side"), modLoc("block/harness_top")));

        BlockModelBuilder seat = models().withExistingParent("riders_seat", mcLoc("block/block"))
                .texture("cushion", modLoc("block/riders_seat"))
                .texture("brass", modLoc("block/brass_fitting"))
                .texture("particle", modLoc("block/riders_seat"));
        box(seat, 1, 0, 1, 15, 3, 15, "#brass", false);
        box(seat, 1, 3, 1, 15, 6, 15, "#cushion", false);
        box(seat, 1, 6, 12, 15, 14, 15, "#cushion", false);
        box(seat, 0, 3, 1, 1, 9, 15, "#brass", false);
        box(seat, 15, 3, 1, 16, 9, 15, "#brass", false);
        horizontalBlock(ModBlocks.RIDERS_SEAT.get(), seat);
        simpleBlockItem(ModBlocks.RIDERS_SEAT.get(), seat);

        BlockModelBuilder[] cores = new BlockModelBuilder[2];
        for (int active = 0; active < 2; active++) {
            BlockModelBuilder core = models().withExistingParent(active == 1 ? "temporal_core" : "temporal_core_still", mcLoc("block/block"))
                    .texture("heart", modLoc(active == 1 ? "block/temporal_core" : "block/temporal_core_still"))
                    .texture("cage", mcLoc("block/gold_block"))
                    .texture("particle", modLoc("block/temporal_core"))
                    .renderType("cutout");
            box(core, 4, 3, 4, 12, 13, 12, "#heart", false);
            box(core, 2, 0, 2, 14, 2, 14, "#cage", false);
            box(core, 2, 14, 2, 14, 16, 14, "#cage", false);
            for (int[] bar : new int[][] {{2, 2}, {12, 2}, {2, 12}, {12, 12}}) {
                box(core, bar[0], 2, bar[1], bar[0] + 2, 14, bar[1] + 2, "#cage", false);
            }
            cores[active] = core;
        }
        getVariantBuilder(ModBlocks.TEMPORAL_CORE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(cores[state.getValue(TemporalCoreBlock.ACTIVE) ? 1 : 0]).build());
        simpleBlockItem(ModBlocks.TEMPORAL_CORE.get(), cores[1]);

        // Core sinew: a knotted strand along its axis, thicker at the knots.
        BlockModelBuilder sinew = models().withExistingParent("core_sinew", mcLoc("block/block"))
                .texture("flesh", modLoc("block/core_sinew"))
                .texture("particle", modLoc("block/core_sinew"))
                .renderType("cutout");
        box(sinew, 6, 0, 6, 10, 16, 10, "#flesh", false);
        box(sinew, 5, 3, 5.5f, 10.5f, 6, 11, "#flesh", false);
        box(sinew, 5.5f, 10, 5, 11, 13, 10.5f, "#flesh", false);
        getVariantBuilder(ModBlocks.CORE_SINEW.get()).forAllStates(state -> {
            var axis = state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS);
            return ConfiguredModel.builder().modelFile(sinew)
                    .rotationX(axis == net.minecraft.core.Direction.Axis.Y ? 0 : 90)
                    .rotationY(axis == net.minecraft.core.Direction.Axis.X ? 90 : 0).build();
        });
        simpleBlockItem(ModBlocks.CORE_SINEW.get(), sinew);

        ModelFile broken = models().cubeAll("seal_broken", mcLoc("block/cracked_polished_blackstone_bricks"));
        for (var seal : java.util.List.of(ModBlocks.HEAVY_SEAL, ModBlocks.NARROW_SEAL, ModBlocks.SWIFT_SEAL, ModBlocks.VEILED_SEAL,
                ModBlocks.STILLED_SEAL)) {
            String name = seal.getId().getPath();
            ModelFile whole = seal == ModBlocks.VEILED_SEAL
                    ? models().cubeAll(name, mcLoc("block/polished_blackstone_bricks"))
                    : models().cubeAll(name, modLoc("block/" + name));
            getVariantBuilder(seal.get()).forAllStates(state -> ConfiguredModel.builder()
                    .modelFile(state.getValue(SealBlock.BROKEN) ? broken : whole).build());
            simpleBlockItem(seal.get(), whole);
        }
    }

    /** Obelisk stone, the glowing inscription band, and the stepped capstone (the nail's head). */
    private void obelisk() {
        simpleBlock(ModBlocks.OBELISK_STONE.get(), models().cubeAll("obelisk_stone", modLoc("block/obelisk_stone")));
        simpleBlock(ModBlocks.OBELISK_INSCRIPTION.get(),
                models().cubeAll("obelisk_inscription", modLoc("block/obelisk_inscription")));
        BlockModelBuilder cap = models().withExistingParent("obelisk_capstone", mcLoc("block/block"))
                .texture("stone", modLoc("block/obelisk_stone"))
                .texture("glyph", modLoc("block/obelisk_inscription"))
                .texture("particle", modLoc("block/obelisk_stone"));
        box(cap, 1, 0, 1, 15, 6, 15, "#stone", false);
        box(cap, 4, 6, 4, 12, 12, 12, "#glyph", false);
        box(cap, 6.5f, 12, 6.5f, 9.5f, 16, 9.5f, "#stone", false);
        simpleBlock(ModBlocks.OBELISK_CAPSTONE.get(), cap);
        // The Unmade Nail: the capstone's shape, dull and veined until it is driven, then glowing like an inscription.
        BlockModelBuilder[] nails = new BlockModelBuilder[2];
        for (int driven = 0; driven < 2; driven++) {
            BlockModelBuilder nail = models().withExistingParent(driven == 1 ? "unmade_nail_driven" : "unmade_nail", mcLoc("block/block"))
                    .texture("stone", modLoc("block/obelisk_stone"))
                    .texture("glyph", modLoc(driven == 1 ? "block/obelisk_inscription" : "block/unmade_nail"))
                    .texture("particle", modLoc("block/unmade_nail"));
            box(nail, 1, 0, 1, 15, 6, 15, "#glyph", false);
            box(nail, 4, 6, 4, 12, 12, 12, "#stone", false);
            box(nail, 6.5f, 12, 6.5f, 9.5f, 16, 9.5f, "#glyph", false);
            nails[driven] = nail;
        }
        getVariantBuilder(ModBlocks.UNMADE_NAIL.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(nails[state.getValue(com.chronogenesis.timetravel.UnmadeNailBlock.DRIVEN) ? 1 : 0]).build());
        simpleBlockItem(ModBlocks.UNMADE_NAIL.get(), nails[0]);
    }

    /** A thin sheet of yellowed paper on a table, turned to face the reader. */
    private void fadedBlueprint() {
        BlockModelBuilder sheet = models().withExistingParent("faded_blueprint", mcLoc("block/block"))
                .texture("paper", modLoc("block/faded_blueprint"))
                .texture("particle", modLoc("block/faded_blueprint"))
                .renderType("cutout");
        sheet.element().from(1, 0, 2).to(15, 0.25f, 14)
                .allFaces((dir, face) -> face.texture("#paper")).end();
        getVariantBuilder(ModBlocks.FADED_BLUEPRINT.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(sheet)
                .rotationY(((int) state.getValue(HorizontalDirectionalBlock.FACING).toYRot() + 180) % 360)
                .build());
    }

    private BlockModelBuilder tableModel(String name) {
        BlockModelBuilder model = models().withExistingParent(name, mcLoc("block/block"))
                .texture("top", modLoc("block/dissection_table_top"))
                .texture("side", modLoc("block/dissection_table_side"))
                .texture("steel", modLoc("block/steel"))
                .texture("brass", modLoc("block/brass_fitting"))
                .texture("brain", modLoc("block/brain_tissue"))
                .texture("fluid", modLoc("block/cortex_fluid"))
                .texture("glass", modLoc("block/cortex_glass"))
                .texture("particle", modLoc("block/dissection_table_top"))
                .renderType("translucent");
        model.element().from(0, 7, 0).to(16, 9, 16)
                .allFaces((dir, face) -> face.texture(dir.getAxis().isVertical() ? "#top" : "#side")).end();
        for (int[] leg : new int[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}}) {
            box(model, leg[0], 0, leg[1], leg[0] + 2, 7, leg[1] + 2, "#side", false);
        }
        return model;
    }

    /** A specimen jar on the table: brain, preserving fluid, glass, brass lid. */
    private static void jar(BlockModelBuilder model, float from, float to, float top) {
        float inset = (to - from) * 0.15f;
        box(model, from + inset, 9, from + inset, to - inset, top - 3, to - inset, "#brain", false);
        box(model, from + 0.25f, 9, from + 0.25f, to - 0.25f, top - 1.5f, to - 0.25f, "#fluid", false);
        box(model, from, 9, from, to, top - 1, to, "#glass", false);
        box(model, from - 0.5f, top - 1, from - 0.5f, to + 0.5f, top, to + 0.5f, "#brass", false);
    }

    // ---------------------------------------------------------------- conduit and veins

    /**
     * Brass Conduit, as a multipart: a core plus one arm per connected side. Grown into a vein, the
     * same shapes are wrapped in living tissue; a clogged vein shows the rotting clot.
     */
    private void conduit() {
        ModelFile[] cores = new ModelFile[3];
        ModelFile[] arms = new ModelFile[3];
        String[][] variants = {{"brass_conduit", "brass_fitting"}, {"brass_conduit_vein", "vein"},
                {"brass_conduit_clogged", "vein_clogged"}};
        for (int i = 0; i < 3; i++) {
            String name = variants[i][0];
            String texture = variants[i][1];
            BlockModelBuilder core = models().withExistingParent(name + "_core", mcLoc("block/block"))
                    .texture("pipe", modLoc("block/" + texture))
                    .texture("brass", modLoc("block/brass_fitting"))
                    .texture("particle", modLoc("block/" + texture))
                    .renderType("cutout");
            box(core, 5, 5, 5, 11, 11, 11, "#pipe", false);
            cores[i] = core;
            BlockModelBuilder arm = models().withExistingParent(name + "_arm", mcLoc("block/block"))
                    .texture("pipe", modLoc("block/" + texture))
                    .texture("brass", modLoc("block/brass_fitting"))
                    .texture("particle", modLoc("block/" + texture))
                    .renderType("cutout");
            box(arm, 6, 6, 0, 10, 10, 5, "#pipe", false);
            box(arm, 5.5f, 5.5f, 0, 10.5f, 10.5f, 1, "#brass", false); // brass collar at the joint
            arms[i] = arm;
        }

        var builder = getMultipartBuilder(ModBlocks.BRASS_CONDUIT.get());
        for (int i = 0; i < 3; i++) {
            boolean vein = i > 0;
            boolean clogged = i == 2;
            builder.part().modelFile(cores[i]).addModel()
                    .condition(com.chronogenesis.organism.BrassConduitBlock.VEIN, vein)
                    .condition(com.chronogenesis.organism.BrassConduitBlock.CLOGGED, clogged).end();
            for (var entry : com.chronogenesis.organism.BrassConduitBlock.CONNECTIONS.entrySet()) {
                Direction dir = entry.getKey();
                int x = dir == Direction.UP ? 270 : dir == Direction.DOWN ? 90 : 0;
                int y = dir.getAxis().isHorizontal() ? ((int) dir.toYRot() + 180) % 360 : 0;
                builder.part().modelFile(arms[i]).rotationX(x).rotationY(y).addModel()
                        .condition(entry.getValue(), true)
                        .condition(com.chronogenesis.organism.BrassConduitBlock.VEIN, vein)
                        .condition(com.chronogenesis.organism.BrassConduitBlock.CLOGGED, clogged).end();
            }
        }
        // The item shows a straight length of pipe.
        itemModels().getBuilder("brass_conduit").parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("pipe", modLoc("block/brass_fitting"))
                .texture("particle", modLoc("block/brass_fitting"))
                .element().from(6, 6, 0).to(10, 10, 16).allFaces((dir, face) -> face.texture("#pipe")).end()
                .element().from(5.5f, 5.5f, 5).to(10.5f, 10.5f, 6).allFaces((dir, face) -> face.texture("#pipe")).end()
                .element().from(5.5f, 5.5f, 10).to(10.5f, 10.5f, 11).allFaces((dir, face) -> face.texture("#pipe")).end();
    }

    private BlockModelBuilder graftModel(String name, String fleshTexture) {
        return graftModel(name, fleshTexture, name);
    }

    /** For model variants (e.g. a graft at work) that share their particle texture with the plain graft. */
    private BlockModelBuilder graftModel(String name, String fleshTexture, String particleTexture) {
        return models().withExistingParent(name, mcLoc("block/block"))
                .texture("flesh", modLoc("block/" + fleshTexture))
                .texture("brass", modLoc("block/brass_fitting"))
                .texture("steel", modLoc("block/steel"))
                .texture("particle", modLoc("block/" + particleTexture))
                .renderType("cutout");
    }

    private void graftBlock(net.minecraft.world.level.block.Block block, ModelFile model) {
        graftBlock(block, model, model);
    }

    /** A graft whose model changes while it is {@link GraftBlock#ACTIVE}; rotated like {@code directionalBlock}. */
    private void graftBlock(net.minecraft.world.level.block.Block block, ModelFile model, ModelFile activeModel) {
        getVariantBuilder(block).forAllStates(state -> {
            Direction dir = state.getValue(GraftBlock.FACING);
            return ConfiguredModel.builder()
                    .modelFile(state.getValue(GraftBlock.ACTIVE) ? activeModel : model)
                    .rotationX(dir == Direction.DOWN ? 180 : dir.getAxis().isHorizontal() ? 90 : 0)
                    .rotationY(dir.getAxis().isVertical() ? 0 : ((int) dir.toYRot() + 180) % 360)
                    .build();
        });
        String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }

    // ---------------------------------------------------------------- misc blocks

    private void jug() {
        ModelFile[] levels = new ModelFile[5];
        for (int fill = 0; fill <= 4; fill++) {
            BlockModelBuilder jug = models().getBuilder(fill == 0 ? "essence_jug" : "essence_jug_" + fill)
                    .texture("side", modLoc("block/essence_jug"))
                    .texture("top", modLoc("block/essence_jug_top"))
                    .texture("fill", modLoc("block/essence_jug_fill"))
                    .texture("particle", modLoc("block/essence_jug"))
                    .renderType("cutout")
                    .element().from(4, 0, 4).to(12, 10, 12)
                    .allFaces((dir, face) -> face.texture(dir.getAxis().isVertical() ? "#top" : "#side")).end()
                    .element().from(6, 10, 6).to(10, 13, 10)
                    .allFaces((dir, face) -> face.texture(dir.getAxis().isVertical() ? "#top" : "#side")).end();
            if (fill > 0) {
                // Liquid in the glass band between the brass hoops (y 2-8), drawn just outside the glass so the
                // bars in the fill texture stay clear. Tint 0 = the aspect inside (ClientEvents).
                float top = 2 + fill * 1.5f;
                jug.element().from(3.99f, 2, 3.99f).to(12.01f, top, 12.01f)
                        .face(Direction.NORTH).texture("#fill").tintindex(0).end()
                        .face(Direction.SOUTH).texture("#fill").tintindex(0).end()
                        .face(Direction.EAST).texture("#fill").tintindex(0).end()
                        .face(Direction.WEST).texture("#fill").tintindex(0).end()
                        .end();
            }
            levels[fill] = jug;
        }
        getVariantBuilder(ModBlocks.ESSENCE_JUG.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(levels[state.getValue(EssenceJugBlock.FILL)]).build());
        simpleBlockItem(ModBlocks.ESSENCE_JUG.get(), levels[0]);
    }

    private void timeTear() {
        ModelFile tear = models().getBuilder("time_tear")
                .texture("all", modLoc("block/time_tear"))
                .texture("particle", modLoc("block/time_tear"))
                .renderType("translucent")
                .element().from(3, 3, 3).to(13, 13, 13).allFaces((dir, face) -> face.texture("#all")).end();
        simpleBlock(ModBlocks.TIME_TEAR.get(), tear);
    }

    // ---------------------------------------------------------------- shared parts

    /** A model with the organism textures: flesh (tinted), the brass frame, and a particle texture. */
    private BlockModelBuilder organismModel(String name, String fleshTexture) {
        return models().withExistingParent(name, mcLoc("block/block"))
                .texture("flesh", modLoc("block/" + fleshTexture))
                .texture("frame", BRASS)
                .texture("particle", modLoc("block/" + fleshTexture))
                .renderType("cutout");
    }

    /** The twelve 2x2 brass beams along the edges of the block: the armature the organism grows on. */
    private static void frame(BlockModelBuilder model) {
        int[] ends = {0, 14};
        for (int a : ends) {
            for (int b : ends) {
                box(model, 0, a, b, 16, a + 2, b + 2, FRAME, false); // along X
                box(model, a, 0, b, a + 2, 16, b + 2, FRAME, false); // along Y
                box(model, a, b, 0, a + 2, b + 2, 16, FRAME, false); // along Z
            }
        }
    }

    private static void box(BlockModelBuilder model, float x1, float y1, float z1, float x2, float y2, float z2,
            String texture, boolean tinted) {
        model.element().from(x1, y1, z1).to(x2, y2, z2).allFaces((dir, face) -> {
            face.texture(texture);
            if (tinted) {
                face.tintindex(0);
            }
        }).end();
    }
}
