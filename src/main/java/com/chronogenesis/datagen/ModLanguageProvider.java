package com.chronogenesis.datagen;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModItems;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** en_us translations. Every new item, block, tab and config entry gets a line here. */
public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, ChronoGenesis.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.chronogenesis", "ChronoGenesis");

        addItem(ModItems.ESSENCE_VIAL, "Essence Vial");
        addItem(ModItems.TEMPORAL_RESIDUE, "Temporal Residue");
        addBlock(ModBlocks.CHRONOFLESH, "Chronoflesh");

        // Config screen
        add("chronogenesis.configuration.title", "ChronoGenesis Configs");
        add("chronogenesis.configuration.section.chronogenesis.common.toml", "ChronoGenesis Configs");
        add("chronogenesis.configuration.section.chronogenesis.common.toml.title", "ChronoGenesis Configs");
        add("chronogenesis.configuration.temporal_essence", "Temporal Essence");
        add("chronogenesis.configuration.maxEntityEssence", "Max Essence per Entity");
        add("chronogenesis.configuration.essenceRegenTicks", "Essence Regen Interval (ticks)");
    }
}
