package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.organism.CorePhase;
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
 * Organisms are built from shared parts: a copper frame (the twelve edges of the armature),
 * flesh masses tinted by the organism's aspect (tint index 0), and untinted details (brass,
 * eyes, pustules). Flesh has four growth stages and cores have four phases; see {@link FleshBlock}
 * and {@link CorePhase}. Grafts are modelled pointing up; {@code directionalBlock} rotates them.
 */
public class ModBlockStateProvider extends BlockStateProvider {
    private static final String FLESH = "#flesh";
    private static final String FRAME = "#frame";

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, ChronoGenesis.MODID, fileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Frame: see-through copper lattice.
        ModelFile armature = models().cubeAll("copper_armature", modLoc("block/copper_armature")).renderType("cutout");
        simpleBlockWithItem(ModBlocks.COPPER_ARMATURE.get(), armature);

        fleshStages();
        corePhases();
        grafts();
        jug();
        timeTear();
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
                .texture("sac", modLoc("block/enhancing_gland"));
        box(gland, 3, 0, 3, 13, 2, 13, FLESH, false);
        box(gland, 4, 2, 4, 12, 4, 12, "#sac", false);
        box(gland, 3, 4, 3, 13, 8, 13, "#sac", false);
        box(gland, 4, 8, 4, 12, 10, 12, "#sac", false);
        box(gland, 6.5f, 10, 6.5f, 9.5f, 11.5f, 9.5f, "#brass", false);
        box(gland, 7.5f, 11.5f, 7.5f, 8.5f, 14, 8.5f, "#steel", false);
        graftBlock(ModBlocks.ENHANCING_GLAND.get(), gland);
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
        ModelFile jug = models().getBuilder("essence_jug")
                .texture("side", modLoc("block/essence_jug"))
                .texture("top", modLoc("block/essence_jug_top"))
                .texture("particle", modLoc("block/essence_jug"))
                .renderType("cutout")
                .element().from(4, 0, 4).to(12, 10, 12)
                .allFaces((dir, face) -> face.texture(dir.getAxis().isVertical() ? "#top" : "#side")).end()
                .element().from(6, 10, 6).to(10, 13, 10)
                .allFaces((dir, face) -> face.texture(dir.getAxis().isVertical() ? "#top" : "#side")).end();
        simpleBlockWithItem(ModBlocks.ESSENCE_JUG.get(), jug);
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

    /** A model with the organism textures: flesh (tinted), the copper frame, and a particle texture. */
    private BlockModelBuilder organismModel(String name, String fleshTexture) {
        return models().withExistingParent(name, mcLoc("block/block"))
                .texture("flesh", modLoc("block/" + fleshTexture))
                .texture("frame", modLoc("block/copper_beam"))
                .texture("particle", modLoc("block/" + fleshTexture))
                .renderType("cutout");
    }

    /** The twelve 2x2 copper beams along the edges of the block: the armature the organism grows on. */
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
