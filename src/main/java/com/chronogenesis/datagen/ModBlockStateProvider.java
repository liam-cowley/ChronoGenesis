package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biomech.OrganismCoreBlock;
import com.chronogenesis.biomech.OrganismFleshBlock;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceJugBlock;
import com.chronogenesis.registry.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
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
        cubeAllWithItem(ModBlocks.CHRONOFLESH);
        simpleBlockWithItem(ModBlocks.COPPER_ARMATURE.get(),
                models().cubeAll("copper_armature", modLoc("block/copper_armature")).renderType("cutout"));

        organismCore();
        organismFlesh();
        tap();
        maw();
        leechTendril();
        essenceJug();
    }

    /** Full cube using textures/block/&lt;name&gt;.png on all sides, plus its item model. */
    private void cubeAllWithItem(DeferredBlock<? extends Block> block) {
        simpleBlockWithItem(block.get(), cubeAll(block.get()));
    }

    private void organismCore() {
        ModelFile necrotic = models().cubeAll("organism_core_necrotic", modLoc("block/organism_core_necrotic"));
        getVariantBuilder(ModBlocks.ORGANISM_CORE.get()).forAllStates(state -> {
            if (state.getValue(OrganismCoreBlock.NECROTIC)) {
                return ConfiguredModel.builder().modelFile(necrotic).build();
            }
            String name = "organism_core_" + state.getValue(OrganismCoreBlock.ASPECT).getSerializedName();
            return ConfiguredModel.builder().modelFile(models().cubeAll(name, modLoc("block/" + name))).build();
        });
    }

    private void organismFlesh() {
        getVariantBuilder(ModBlocks.ORGANISM_FLESH.get()).forAllStates(state -> {
            Aspect aspect = state.getValue(OrganismFleshBlock.ASPECT);
            String name = "organism_flesh_" + aspect.getSerializedName();
            // A few rotated variants so large organisms don't look tiled.
            ModelFile model = models().cubeAll(name, modLoc("block/" + name));
            return ConfiguredModel.builder()
                    .modelFile(model).nextModel()
                    .modelFile(model).rotationY(90).nextModel()
                    .modelFile(model).rotationX(180).nextModel()
                    .modelFile(model).rotationY(270).rotationX(180)
                    .build();
        });
    }

    /** Graft models point up from the floor; {@code directionalBlock} rotates them onto the right face. */
    private BlockModelBuilder graft(String name) {
        return models().withExistingParent(name, mcLoc("block/block"))
                .ao(false)
                .texture("base", modLoc("block/graft_base"));
    }

    private void tap() {
        BlockModelBuilder model = graft("tap")
                .texture("tap", modLoc("block/tap"))
                .texture("particle", modLoc("block/tap"))
                .element().from(4, 0, 4).to(12, 2, 12).allFaces((dir, face) -> face.texture("#base")).end()
                .element().from(6, 2, 6).to(10, 7, 10).allFaces((dir, face) -> face.texture("#tap")).end()
                .element().from(5, 7, 5).to(11, 9, 11).allFaces((dir, face) -> face.texture("#tap")).end();
        directionalBlock(ModBlocks.TAP.get(), model);
        simpleBlockItem(ModBlocks.TAP.get(), model);
    }

    private void maw() {
        BlockModelBuilder model = graft("maw")
                .texture("side", modLoc("block/maw_side"))
                .texture("top", modLoc("block/maw_top"))
                .texture("particle", modLoc("block/maw_side"))
                .element().from(3, 0, 3).to(13, 4, 13)
                .allFaces((dir, face) -> face.texture(dir == Direction.UP ? "#top" : "#side")).end();
        directionalBlock(ModBlocks.MAW.get(), model);
        simpleBlockItem(ModBlocks.MAW.get(), model);
    }

    private void leechTendril() {
        BlockModelBuilder model = graft("leech_tendril")
                .texture("stalk", modLoc("block/leech_tendril"))
                .texture("tip", modLoc("block/leech_tendril_tip"))
                .texture("particle", modLoc("block/leech_tendril"))
                .renderType("cutout")
                .element().from(5, 0, 5).to(11, 2, 11).allFaces((dir, face) -> face.texture("#base")).end()
                .element().from(7, 2, 7).to(9, 11, 9).allFaces((dir, face) -> face.texture("#stalk")).end()
                .element().from(6, 11, 6).to(10, 14, 10).allFaces((dir, face) -> face.texture("#tip")).end();
        directionalBlock(ModBlocks.LEECH_TENDRIL.get(), model);
        simpleBlockItem(ModBlocks.LEECH_TENDRIL.get(), model);
    }

    private void essenceJug() {
        ModelFile[] levels = new ModelFile[5];
        for (int fill = 0; fill <= 4; fill++) {
            BlockModelBuilder model = models().withExistingParent("essence_jug_" + fill, mcLoc("block/block"))
                    .ao(false)
                    .renderType("translucent")
                    .texture("glass", modLoc("block/essence_jug"))
                    .texture("cork", modLoc("block/essence_jug_cork"))
                    .texture("liquid", modLoc("block/essence_jug_liquid"))
                    .texture("particle", modLoc("block/essence_jug"));
            if (fill > 0) {
                float top = 0.5f + fill * 2.25f;
                model.element().from(4.5f, 0.5f, 4.5f).to(11.5f, top, 11.5f)
                        .allFaces((dir, face) -> face.texture("#liquid").tintindex(0)).end();
            }
            model.element().from(4, 0, 4).to(12, 10, 12).allFaces((dir, face) -> face.texture("#glass")).end()
                    .element().from(6, 10, 6).to(10, 13, 10).allFaces((dir, face) -> face.texture("#glass")).end()
                    .element().from(5.5f, 13, 5.5f).to(10.5f, 15, 10.5f).allFaces((dir, face) -> face.texture("#cork")).end();
            levels[fill] = model;
        }
        getVariantBuilder(ModBlocks.ESSENCE_JUG.get()).forAllStates(state ->
                ConfiguredModel.builder().modelFile(levels[state.getValue(EssenceJugBlock.FILL)]).build());
        // The item shows a half-full jug; the liquid takes the colour of its contents (or clear glass).
        simpleBlockItem(ModBlocks.ESSENCE_JUG.get(), levels[2]);
    }
}
