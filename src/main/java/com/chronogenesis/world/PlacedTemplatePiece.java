package com.chronogenesis.world;

import com.chronogenesis.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** A {@link PlacedTemplateStructure}'s template: turned about its own centre, so it stays centred where it was put. */
public class PlacedTemplatePiece extends TemplateStructurePiece {
    public PlacedTemplatePiece(StructureTemplateManager manager, ResourceLocation id, BlockPos origin, Rotation rotation) {
        super(ModStructures.PLACED_TEMPLATE_PIECE.get(), 0, manager, id, id.toString(), settings(manager, id, rotation), origin);
    }

    public PlacedTemplatePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.PLACED_TEMPLATE_PIECE.get(), tag, manager,
                id -> settings(manager, id, Rotation.valueOf(tag.getString("Rot"))));
    }

    private static StructurePlaceSettings settings(StructureTemplateManager manager, ResourceLocation id, Rotation rotation) {
        Vec3i size = manager.getOrCreate(id).getSize();
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }
}
