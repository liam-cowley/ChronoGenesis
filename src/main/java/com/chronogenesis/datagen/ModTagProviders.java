package com.chronogenesis.datagen;

import java.util.concurrent.CompletableFuture;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Block, item and entity type tags. */
public final class ModTagProviders {
    public static class Blocks extends BlockTagsProvider {
        public Blocks(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper fileHelper) {
            super(output, registries, ChronoGenesis.MODID, fileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.COPPER_ARMATURE.get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.COPPER_ARMATURE.get());
            // Flesh is cut, not mined.
            tag(BlockTags.MINEABLE_WITH_HOE).add(
                    ModBlocks.CHRONOFLESH.get(),
                    ModBlocks.ORGANISM_CORE.get(),
                    ModBlocks.ORGANISM_FLESH.get(),
                    ModBlocks.TAP.get(),
                    ModBlocks.MAW.get(),
                    ModBlocks.LEECH_TENDRIL.get());
        }
    }

    public static class Items extends ItemTagsProvider {
        public Items(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                     CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper fileHelper) {
            super(output, registries, blockTags, ChronoGenesis.MODID, fileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            tag(ModTags.NEEDLES).add(
                    ModItems.IRON_NEEDLE.get(),
                    ModItems.GOLD_NEEDLE.get(),
                    ModItems.BONE_NEEDLE.get(),
                    ModItems.AMETHYST_NEEDLE.get());
        }
    }

    public static class EntityTypes extends EntityTypeTagsProvider {
        public EntityTypes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper fileHelper) {
            super(output, registries, ChronoGenesis.MODID, fileHelper);
        }

        @Override
        protected void addTags(HolderLookup.Provider registries) {
            // Phantoms are born from sleeplessness, endermen slip through space, the undead live on borrowed time.
            tag(ModTags.TIME_BEARERS)
                    .add(EntityType.PHANTOM, EntityType.ENDERMAN)
                    .addTag(EntityTypeTags.UNDEAD);
        }
    }

    private ModTagProviders() {}
}
