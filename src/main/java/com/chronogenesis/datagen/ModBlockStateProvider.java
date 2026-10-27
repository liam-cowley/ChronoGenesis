package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.DissectionTableBlock;
import com.chronogenesis.organism.CorePhase;
import com.chronogenesis.organism.EssenceJugBlock;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
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
    }

    // ---------------------------------------------------------------- flesh

    private void fleshStages() {
        ModelFile[] stages = new ModelFile[4];

        // 0: tendrils: the bare frame with the first strands wrapped around its beams.
        BlockModelBuilder tendrils = organismModel("chronoflesh_growth_0", "chronoflesh");
        frame(tendrils);
        box(tendrils, 3, 0, 0, 10, 3, 3, FLESH, true);       // sleeve on a bottom beam
        box(tendrils, 0, 4, 0, 3, 12, 3, FLESH, true);       // sleeve on an upright
        box(tendrils, 6, 13, 13, 14, 16, 16, FLESH, true);   // sleeve on a top beam
        box(tendrils, 12, 0, 12, 16, 4, 16, FLESH, true);    // a knot in one corner
        tendrils.element().from(7, 1, 0.5f).to(9, 15, 1.5f)  // a strand across the face
                .rotation().angle(45).axis(Direction.Axis.Z).origin(8, 8, 1).end()
                .allFaces((dir, face) -> face.texture(FLESH).tintindex(0)).end();
        stages[0] = tendrils;

        // 1: creeping: a soft inner mass, every upright sleeved, a membrane on the floor.
        BlockModelBuilder creeping = organismModel("chronoflesh_growth_1", "chronoflesh");
        frame(creeping);
        box(creeping, 4, 3, 4, 12, 11, 12, FLESH, true);
        box(creeping, 2, 0, 2, 14, 1, 14, FLESH, true);
        box(creeping, 0, 0, 0, 3, 9, 3, FLESH, true);
        box(creeping, 13, 0, 0, 16, 6, 3, FLESH, true);
        box(creeping, 0, 0, 13, 3, 11, 16, FLESH, true);
        box(creeping, 13, 0, 13, 16, 7, 16, FLESH, true);
        stages[1] = creeping;

        // 2: swelling: the mass fills the frame and bulges between the beams.
        BlockModelBuilder swelling = organismModel("chronoflesh_growth_2", "chronoflesh");
        frame(swelling);
        box(swelling, 2, 1, 2, 14, 15, 14, FLESH, true);
        box(swelling, 5, 6, 14, 10, 11, 15, FLESH, true);
        box(swelling, 14, 4, 6, 15, 9, 11, FLESH, true);
        box(swelling, 6, 15, 4, 11, 16, 9, FLESH, true);
        stages[2] = swelling;

        // 3: mature: the frame is swallowed. Scarred, stapled, and weeping pustules.
        BlockModelBuilder mature = organismModel("chronoflesh_growth_3", "chronoflesh")
                .texture("overlay", modLoc("block/chronoflesh_overlay"))
                .texture("pustule", modLoc("block/flesh_pustule"));
        mature.element().allFaces((dir, face) -> face.texture(FLESH).cullface(dir).tintindex(0)).end();
        mature.element().allFaces((dir, face) -> face.texture("#overlay").cullface(dir)).end();
        box(mature, 10, 16, 3, 13, 17, 6, "#pustule", false);
        box(mature, 4, 9, -1, 7, 12, 0, "#pustule", false);
        box(mature, 11, 3, 16, 13, 5, 17, "#pustule", false);
        stages[3] = mature;

        getVariantBuilder(ModBlocks.CHRONOFLESH.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(stages[state.getValue(FleshBlock.GROWTH)]).build());
        itemModels().withExistingParent("chronoflesh", modLoc("block/chronoflesh_growth_3"));
    }

    // ---------------------------------------------------------------- core

    private void corePhases() {
        // Seed: a culture on a brass cradle, reaching for the frame.
        BlockModelBuilder seed = organismModel("organism_core_seed", "organism_core")
                .texture("brass", modLoc("block/brass_fitting"));
        frame(seed);
        box(seed, 3, 0, 3, 13, 2, 13, "#brass", false);
        box(seed, 5, 2, 5, 11, 7, 11, FLESH, true);
        box(seed, 6, 7, 6, 10, 9, 10, FLESH, true);
        box(seed, 2, 2, 7, 5, 4, 9, FLESH, true);
        box(seed, 11, 3, 7, 14, 5, 9, FLESH, true);
        box(seed, 7, 9, 7, 9, 13, 9, FLESH, true);

        // Awake: a bloated mass inside the frame with an eye on every side.
        BlockModelBuilder awake = organismModel("organism_core_awake", "organism_core")
                .texture("eye", modLoc("block/organism_core_overlay"));
        frame(awake);
        box(awake, 1, 1, 1, 15, 15, 15, FLESH, true);
        box(awake, 1, 1, 1, 15, 15, 15, "#eye", false);

        // Hungry: shrunken, the eyes half shut and bloodshot.
        BlockModelBuilder hungry = organismModel("organism_core_hungry", "organism_core")
                .texture("eye", modLoc("block/organism_core_hungry_overlay"));
        frame(hungry);
        box(hungry, 2, 2, 2, 14, 14, 14, FLESH, true);
        box(hungry, 2, 2, 2, 14, 14, 14, "#eye", false);

        // Necrotic: slumped, grey, stitched shut.
        BlockModelBuilder necrotic = organismModel("organism_core_necrotic", "organism_core_necrotic");
        frame(necrotic);
        box(necrotic, 2, 1, 2, 14, 11, 14, FLESH, false);
        box(necrotic, 4, 11, 4, 12, 12, 12, FLESH, false);

        getVariantBuilder(ModBlocks.ORGANISM_CORE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(switch (state.getValue(OrganismCoreBlock.PHASE)) {
                    case SEED -> seed;
                    case AWAKE -> awake;
                    case HUNGRY -> hungry;
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
        graftBlock(ModBlocks.LEECH_TENDRIL.get(), leech);

        // Enhancing Gland: a swollen sac on a mound, a brass port and a needle on top.
        BlockModelBuilder gland = graftModel("enhancing_gland", "graft_flesh")
                .texture("sac", modLoc("block/gland_sac"));
        box(gland, 3, 0, 3, 13, 2, 13, FLESH, false);
        box(gland, 4, 2, 4, 12, 4, 12, "#sac", false);
        box(gland, 3, 4, 3, 13, 8, 13, "#sac", false);
        box(gland, 4, 8, 4, 12, 10, 12, "#sac", false);
        box(gland, 6.5f, 10, 6.5f, 9.5f, 11.5f, 9.5f, "#brass", false);
        box(gland, 7.5f, 11.5f, 7.5f, 8.5f, 14, 8.5f, "#steel", false);
        graftBlock(ModBlocks.ENHANCING_GLAND.get(), gland);

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

        // Awake: a great jar, the brain swollen inside, electrodes through the brass lid.
        BlockModelBuilder cortex = tableModel("chronocortex");
        jar(cortex, 3, 13, 16);
        box(cortex, 5, 16, 7.5f, 6, 19, 8.5f, "#steel", false);
        box(cortex, 10, 16, 7.5f, 11, 19, 8.5f, "#steel", false);
        simpleBlock(ModBlocks.CHRONOCORTEX.get(), cortex);
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
        return models().withExistingParent(name, mcLoc("block/block"))
                .texture("flesh", modLoc("block/" + fleshTexture))
                .texture("brass", modLoc("block/brass_fitting"))
                .texture("steel", modLoc("block/steel"))
                .texture("particle", modLoc("block/" + name))
                .renderType("cutout");
    }

    private void graftBlock(net.minecraft.world.level.block.Block block, ModelFile model) {
        directionalBlock(block, model);
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
