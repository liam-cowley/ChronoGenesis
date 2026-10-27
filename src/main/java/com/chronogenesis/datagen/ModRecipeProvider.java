package com.chronogenesis.datagen;

import java.util.concurrent.CompletableFuture;

import com.chronogenesis.registry.ModItems;
import com.chronogenesis.registry.ModTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Crafting recipes. Everything is improvised from vanilla materials: copper, glass, nuggets, bone, amethyst.
 * The journal recipe is hand-written in src/main/resources because its result is a Patchouli item.
 */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // --- Extraction ---
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CHRONO_SYRINGE.get())
                .pattern("  C")
                .pattern(" G ")
                .pattern("C  ")
                .define('C', Items.COPPER_INGOT)
                .define('G', Items.GLASS)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output);

        needle(output, ModItems.IRON_NEEDLE.get(), Items.IRON_NUGGET);
        needle(output, ModItems.GOLD_NEEDLE.get(), Items.GOLD_NUGGET);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, ModItems.BONE_NEEDLE.get())
                .requires(Items.BONE)
                .requires(Items.FLINT)
                .unlockedBy("has_syringe", has(ModItems.CHRONO_SYRINGE.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.AMETHYST_NEEDLE.get())
                .pattern(" A")
                .pattern("N ")
                .define('A', Items.AMETHYST_SHARD)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_syringe", has(ModItems.CHRONO_SYRINGE.get()))
                .save(output);

        // --- Containers ---
        ShapedRecipeBuilder.shaped(RecipeCategory.BREWING, ModItems.ESSENCE_VIAL.get(), 2)
                .pattern("P")
                .pattern("P")
                .define('P', Items.GLASS_PANE)
                .unlockedBy("has_syringe", has(ModItems.CHRONO_SYRINGE.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BREWING, ModItems.STASIS_AMPOULE.get())
                .requires(ModItems.ESSENCE_VIAL.get())
                .requires(Items.PHANTOM_MEMBRANE)
                .unlockedBy("has_membrane", has(Items.PHANTOM_MEMBRANE))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BREWING, ModItems.ESSENCE_JUG.get())
                .pattern(" W ")
                .pattern("G G")
                .pattern("GGG")
                .define('W', ItemTags.WOODEN_SLABS)
                .define('G', Items.GLASS)
                .unlockedBy("has_vial", has(ModItems.ESSENCE_VIAL.get()))
                .save(output);

        // --- Grown machines ---
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CULTURE_JAR.get())
                .requires(Items.GLASS_BOTTLE)
                .requires(Items.ROTTEN_FLESH)
                .requires(Items.BONE_MEAL)
                .unlockedBy("has_vial", has(ModItems.ESSENCE_VIAL.get()))
                .save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.COPPER_ARMATURE.get(), 4)
                .pattern("C C")
                .pattern(" C ")
                .pattern("C C")
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_culture", has(ModItems.CULTURE_JAR.get()))
                .save(output);

        // --- Grafts ---
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TAP.get())
                .pattern("F")
                .pattern("G")
                .pattern("C")
                .define('F', ModItems.LIVING_FLESH.get())
                .define('G', Items.GLASS)
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_flesh", has(ModItems.LIVING_FLESH.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MAW.get())
                .requires(ModItems.LIVING_FLESH.get(), 2)
                .requires(Items.BONE)
                .unlockedBy("has_flesh", has(ModItems.LIVING_FLESH.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.LEECH_TENDRIL.get())
                .requires(ModItems.LIVING_FLESH.get())
                .requires(Items.SPIDER_EYE)
                .requires(ModTags.NEEDLES)
                .unlockedBy("has_flesh", has(ModItems.LIVING_FLESH.get()))
                .save(output);

        // --- Storage ---
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModItems.CHRONOFLESH.get())
                .pattern("FFF")
                .pattern("FFF")
                .pattern("FFF")
                .define('F', ModItems.LIVING_FLESH.get())
                .unlockedBy("has_flesh", has(ModItems.LIVING_FLESH.get()))
                .save(output);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.LIVING_FLESH.get(), 9)
                .requires(ModItems.CHRONOFLESH.get())
                .unlockedBy("has_chronoflesh", has(ModItems.CHRONOFLESH.get()))
                .save(output, "living_flesh_from_chronoflesh");
    }

    /** Two nuggets on a diagonal. */
    private static void needle(RecipeOutput output, Item needle, Item nugget) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, needle)
                .pattern(" N")
                .pattern("N ")
                .define('N', nugget)
                .unlockedBy("has_syringe", has(ModItems.CHRONO_SYRINGE.get()))
                .save(output);
    }
}
