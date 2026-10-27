package com.chronogenesis.datagen;

import java.util.function.Consumer;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModItems;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** The early-game advancement line. Picking up a clock starts it all (early-game.md, "Hour 0"). */
final class ModAdvancements implements AdvancementProvider.AdvancementGenerator {
    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper fileHelper) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(Items.CLOCK, title("root"), description("root"),
                        ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                        AdvancementType.TASK, true, false, false)
                .addCriterion("has_clock", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CLOCK))
                .save(saver, ChronoGenesis.id("root"), fileHelper);

        AdvancementHolder syringe = obtain(saver, fileHelper, root, "syringe", ModItems.CHRONO_SYRINGE.get(), AdvancementType.TASK);
        AdvancementHolder culture = obtain(saver, fileHelper, syringe, "culture", ModItems.CULTURE_JAR.get(), AdvancementType.TASK);
        obtain(saver, fileHelper, culture, "living_flesh", ModItems.LIVING_FLESH.get(), AdvancementType.GOAL);
        obtain(saver, fileHelper, syringe, "temporal_residue", ModItems.TEMPORAL_RESIDUE.get(), AdvancementType.TASK);
    }

    private static AdvancementHolder obtain(Consumer<AdvancementHolder> saver, ExistingFileHelper fileHelper,
                                            AdvancementHolder parent, String name, ItemLike item, AdvancementType type) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(item, title(name), description(name), null, type, true, true, false)
                .addCriterion("has_" + name, InventoryChangeTrigger.TriggerInstance.hasItems(item))
                .save(saver, ChronoGenesis.id(name), fileHelper);
    }

    private static Component title(String name) {
        return Component.translatable("advancements.chronogenesis." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancements.chronogenesis." + name + ".description");
    }
}
