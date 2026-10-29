package com.chronogenesis.client;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.Precursor;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Precursors share the villagers' bones (the villagers are what they became, much diminished), but not their
 * look: taller and slighter, grey-skinned, amethyst-eyed. Each kingdom dresses its own way
 * ({@code textures/entity/precursor_<kingdom>.png}, the villager texture layout): the Sun Court in white linen, a gold
 * collar and a striped headcloth; the Tide Kingdom in sea-green scales with a fin crest; the Deep Kingdom hooded in
 * charcoal, a soul-blue band over the eyes; the Hearth in russet wool, a leather apron, a red cap; the Verdant
 * Kingdom in woven green, copper armbands, a crown of flowers. A Precursor of no kingdom wears deep blue and gold
 * ({@code precursor.png}).
 */
public class PrecursorRenderer extends MobRenderer<Precursor, VillagerModel<Precursor>> {
    private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/precursor.png");
    private static final java.util.Map<String, ResourceLocation> DRESS = new java.util.HashMap<>();

    public PrecursorRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(Precursor entity) {
        String kingdom = entity.kingdom();
        if (!Precursor.KINGDOMS.contains(kingdom)) {
            return TEXTURE;
        }
        return DRESS.computeIfAbsent(kingdom, k -> ChronoGenesis.id("textures/entity/precursor_" + k + ".png"));
    }

    @Override
    protected void scale(Precursor entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.95f, 1.12f, 0.95f);
    }
}
