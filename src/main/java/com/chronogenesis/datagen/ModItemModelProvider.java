package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModItems;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
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
        basicItem(ModItems.ESSENCE_MANIPULATOR.get());
        basicItem(ModItems.CONFLUENCE_NEEDLE.get());
        basicItem(ModItems.CHRONO_TETHER.get());
        basicItem(ModItems.CORTEX_OFFSHOOT.get());
        basicItem(ModItems.NERVE_SPOOL.get());
        withExistingParent("precursor_spawn_egg", mcLoc("item/template_spawn_egg"));
        basicItem(ModItems.TEMPORAL_RESIDUE.get());
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
