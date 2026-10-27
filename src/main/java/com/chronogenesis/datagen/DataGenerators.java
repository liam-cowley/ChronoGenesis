package com.chronogenesis.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModRegistries;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for {@code gradlew runData}. Output goes to src/generated/resources.
 * Hand-written assets (textures, Patchouli pages, multiblock structures) stay in src/main/resources.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class DataGenerators {
    @SubscribeEvent
    static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client assets
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, fileHelper));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, fileHelper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output));

        // Server data
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)),
                lookupProvider));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookupProvider));
        var blockTags = generator.addProvider(event.includeServer(), new ModTagProviders.Blocks(output, lookupProvider, fileHelper));
        generator.addProvider(event.includeServer(), new ModTagProviders.Items(output, lookupProvider, blockTags.contentsGetter(), fileHelper));
        generator.addProvider(event.includeServer(), new ModTagProviders.EntityTypes(output, lookupProvider, fileHelper));
        generator.addProvider(event.includeServer(), new AdvancementProvider(output, lookupProvider, fileHelper,
                List.of(new ModAdvancements())));
        // Essence profiles are keyed by vanilla entity ids, so they land in the minecraft namespace.
        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(output, lookupProvider,
                new RegistrySetBuilder().add(ModRegistries.ESSENCE_PROFILE, ModEssenceProfiles::bootstrap),
                Set.of(ChronoGenesis.MODID, "minecraft")));
    }

    private DataGenerators() {}
}
