package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModItems;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Flat item models. Block items get their models from {@link ModBlockStateProvider}. */
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, ChronoGenesis.MODID, fileHelper);
    }

    @Override
    protected void registerModels() {
        // Containers: layer 1 is the liquid, tinted by aspect.
        withFill(ModItems.CHRONO_SYRINGE.get());
        withFill(ModItems.ESSENCE_VIAL.get());
        withFill(ModItems.STASIS_AMPOULE.get());

        basicItem(ModItems.CHRONO_GOGGLES.get());
        basicItem(ModItems.CHRONO_LAB_COAT.get());
        basicItem(ModItems.CHRONO_TROUSERS.get());
        basicItem(ModItems.CHRONO_GALOSHES.get());
        basicItem(ModItems.BRASS_BLEND.get());
        basicItem(ModItems.BRASS_INGOT.get());
        basicItem(ModItems.BRASS_NUGGET.get());
        basicItem(ModItems.IRON_NEEDLE.get());
        basicItem(ModItems.GOLD_NEEDLE.get());
        basicItem(ModItems.BONE_NEEDLE.get());
        basicItem(ModItems.AMETHYST_NEEDLE.get());
        // Culture: the growth (tinted) in an untinted brass dish.
        withExistingParent("culture", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/culture"))
                .texture("layer1", modLoc("item/culture_dish"));
        basicItem(ModItems.PRESERVED_BRAIN.get());
        basicItem(ModItems.PRESERVED_HEART.get());
        basicItem(ModItems.UMBILICAL_LOCKET.get());
        basicItem(ModItems.TEMPORAL_LENS.get());
        basicItem(ModItems.INTACT_BLUEPRINT.get());
        basicItem(ModItems.ETERNAL_HOURGLASS.get());
        gauntlet();
        basicItem(ModItems.CONFLUENCE_NEEDLE.get());
        basicItem(ModItems.CHRONO_TETHER.get());
        basicItem(ModItems.CORTEX_OFFSHOOT.get());
        basicItem(ModItems.NERVE_SPOOL.get());
        withExistingParent("precursor_spawn_egg", mcLoc("item/template_spawn_egg"));
        withExistingParent("raptor_spawn_egg", mcLoc("item/template_spawn_egg"));
        withExistingParent("titanosaur_spawn_egg", mcLoc("item/template_spawn_egg"));
        basicItem(ModItems.DINO_HARNESS.get());
        basicItem(ModItems.STILLED_CORE.get());
        basicItem(ModItems.TEMPORAL_RESIDUE.get());
    }

    /**
     * The Essence Manipulator: drawn by its renderer as a 3D gauntlet (client/GauntletRenderer), so its model is
     * {@code builtin/entity} with display transforms: worn over the hand in first and third person (fingers along
     * the arm), upright and turned in the inventory, smaller on the ground.
     */
    private void gauntlet() {
        // builtin/entity is not a file: it cannot be checked.
        var model = getBuilder("essence_manipulator")
                .parent(new ModelFile.UncheckedModelFile("builtin/entity"))
                .texture("particle", modLoc("item/essence_manipulator"));
        model.transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(-80, 0, 0).translation(0, 3, 1).scale(0.55f).end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(-80, 0, 0).translation(0, 3, 1).scale(0.55f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(-25, -15, 0).translation(1, 2, 0).scale(0.6f).end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(-25, 15, 0).translation(-1, 2, 0).scale(0.6f).end()
                .transform(ItemDisplayContext.GUI).rotation(20, -30, 0).translation(0, 0, 0).scale(0.85f).end()
                .transform(ItemDisplayContext.GROUND).translation(0, 3, 0).scale(0.4f).end()
                .transform(ItemDisplayContext.FIXED).rotation(0, 180, 0).scale(0.75f).end()
                .transform(ItemDisplayContext.HEAD).translation(0, 13, 7).scale(1.0f).end()
                .end();
    }

    /** Vessel (layer 0), liquid tinted by aspect (layer 1), untinted brass and glints on top (layer 2). */
    private void withFill(Item item) {
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name))
                .texture("layer1", modLoc("item/" + name + "_fill"))
                .texture("layer2", modLoc("item/" + name + "_front"));
    }
}
