package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.organism.GraftBlock;
import com.chronogenesis.organism.OrganismCoreBlock;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, ChronoGenesis.MODID, fileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Frame: see-through copper lattice.
        ModelFile armature = models().cubeAll("copper_armature", modLoc("block/copper_armature")).renderType("cutout");
        simpleBlockWithItem(ModBlocks.COPPER_ARMATURE.get(), armature);

        // Flesh: a tinted layer (aspect colour) plus an untinted overlay (stitches, staples, the core's eye).
        ModelFile flesh = tintedWithOverlay("chronoflesh");
        simpleBlockWithItem(ModBlocks.CHRONOFLESH.get(), flesh);

        ModelFile core = tintedWithOverlay("organism_core");
        ModelFile necrotic = models().cubeAll("organism_core_necrotic", modLoc("block/organism_core_necrotic"));
        getVariantBuilder(ModBlocks.ORGANISM_CORE.get()).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(OrganismCoreBlock.NECROTIC) ? necrotic : core).build());

        graft(ModBlocks.TAP);
        graft(ModBlocks.LEECH_TENDRIL);
        graft(ModBlocks.ENHANCING_GLAND);

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

        ModelFile tear = models().getBuilder("time_tear")
                .texture("all", modLoc("block/time_tear"))
                .texture("particle", modLoc("block/time_tear"))
                .renderType("translucent")
                .element().from(3, 3, 3).to(13, 13, 13).allFaces((dir, face) -> face.texture("#all")).end();
        simpleBlock(ModBlocks.TIME_TEAR.get(), tear);
    }

    /**
     * A full cube drawn twice: once with {@code block/<name>} tinted by the aspect colour (tint index 0),
     * once with the untinted {@code block/<name>_overlay} on top (the same trick as vanilla grass).
     */
    private ModelFile tintedWithOverlay(String name) {
        return models().withExistingParent(name, mcLoc("block/block"))
                .texture("all", modLoc("block/" + name))
                .texture("overlay", modLoc("block/" + name + "_overlay"))
                .texture("particle", modLoc("block/" + name))
                .renderType("cutout")
                .element().allFaces((dir, face) -> face.texture("#all").cullface(dir).tintindex(0)).end()
                .element().allFaces((dir, face) -> face.texture("#overlay").cullface(dir)).end();
    }

    /**
     * A graft is a fleshy lump on the face it grows from. The model points UP (base on the floor),
     * which is what {@code directionalBlock} rotates from (like vanilla end rods).
     */
    private void graft(DeferredBlock<GraftBlock> block) {
        String name = block.getId().getPath();
        ModelFile model = models().getBuilder(name)
                .texture("all", modLoc("block/" + name))
                .texture("particle", modLoc("block/" + name))
                .element().from(3, 0, 3).to(13, 8, 13).allFaces((dir, face) -> face.texture("#all")).end()
                .element().from(5, 8, 5).to(11, 11, 11)
                .allFaces((dir, face) -> face.texture("#all")).end();
        directionalBlock(block.get(), model);
        itemModels().withExistingParent(name, modLoc("block/" + name));
    }
}
