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
 * look: taller and slighter, grey-skinned, amethyst-eyed, robed in deep blue and gold, a gold circlet and nose
 * guard ({@code textures/entity/precursor.png}, the villager texture layout).
 */
public class PrecursorRenderer extends MobRenderer<Precursor, VillagerModel<Precursor>> {
    private static final ResourceLocation TEXTURE = ChronoGenesis.id("textures/entity/precursor.png");

    public PrecursorRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(Precursor entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(Precursor entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.95f, 1.12f, 0.95f);
    }
}
