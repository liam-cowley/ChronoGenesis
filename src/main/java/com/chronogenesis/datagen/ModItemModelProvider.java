package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.Needle;
import com.chronogenesis.registry.ModItems;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

/** Flat item models. Block items get their models from {@link ModBlockStateProvider}. */
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, ChronoGenesis.MODID, fileHelper);
    }

    @Override
    protected void registerModels() {
        syringe();
        basicItem(ModItems.IRON_NEEDLE.get());
        basicItem(ModItems.GOLD_NEEDLE.get());
        basicItem(ModItems.BONE_NEEDLE.get());
        basicItem(ModItems.AMETHYST_NEEDLE.get());

        liquidContainer(ModItems.ESSENCE_VIAL);
        liquidContainer(ModItems.STASIS_AMPOULE);
        liquidContainer(ModItems.CULTURE_JAR);

        basicItem(ModItems.LIVING_FLESH.get());
        basicItem(ModItems.TEMPORAL_RESIDUE.get());
    }

    /**
     * A glass container drawn over a liquid layer. The liquid (layer0, tint 0) is coloured by the
     * contents in {@code ModClientEvents}.
     */
    private void liquidContainer(DeferredItem<? extends Item> item) {
        String name = item.getId().getPath();
        withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name + "_liquid"))
                .texture("layer1", modLoc("item/" + name));
    }

    /** One model per fitted needle, switched by the {@code chronogenesis:needle} item property. */
    private void syringe() {
        ItemModelBuilder base = withExistingParent("chrono_syringe", mcLoc("item/generated"))
                .texture("layer0", modLoc("item/chrono_syringe_liquid"))
                .texture("layer1", modLoc("item/chrono_syringe"));
        for (Needle needle : Needle.values()) {
            ItemModelBuilder variant = withExistingParent("chrono_syringe_" + needle.getSerializedName(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/chrono_syringe_liquid"))
                    .texture("layer1", modLoc("item/chrono_syringe"))
                    .texture("layer2", modLoc("item/chrono_syringe_needle_" + needle.getSerializedName()));
            base.override()
                    .predicate(ChronoGenesis.id("needle"), (needle.ordinal() + 1) / (float) Needle.values().length)
                    .model(variant)
                    .end();
        }
    }
}
