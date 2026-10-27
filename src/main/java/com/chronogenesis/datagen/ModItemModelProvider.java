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

        basicItem(ModItems.IRON_NEEDLE.get());
        basicItem(ModItems.GOLD_NEEDLE.get());
        basicItem(ModItems.BONE_NEEDLE.get());
        basicItem(ModItems.AMETHYST_NEEDLE.get());
        basicItem(ModItems.CULTURE.get());
        basicItem(ModItems.PRESERVED_BRAIN.get());
        basicItem(ModItems.TEMPORAL_RESIDUE.get());
    }

    private void withFill(Item item) {
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name))
                .texture("layer1", modLoc("item/" + name + "_fill"));
    }
}
