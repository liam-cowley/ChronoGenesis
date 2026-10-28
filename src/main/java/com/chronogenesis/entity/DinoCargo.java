package com.chronogenesis.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What a harnessed Titanosaur carries: real blocks, built onto its back. Every block placed touching the harness or
 * touching something already attached becomes part of the cargo; a block broken off leaves it.
 * <p>
 * The cargo goes where the beast goes. Whenever the harness's spot (above its back) moves to another block, or the
 * beast turns past the next quarter, the whole cargo is lifted and set down again there, turned with it: states
 * rotated, block entities moved with all their data (chests keep their contents, organisms keep growing), and
 * whatever stands on it carried along. If something solid is in the way, the beast cannot go there.
 * <p>
 * If the harness breaks (or the beast dies), nothing holds the cargo on: every block of it breaks at once, dropping.
 */
public final class DinoCargo {
    /** How many blocks one beast can carry. */
    public static final int MAX_BLOCKS = 384;
    /** Block flags for moving: tell clients, no shape updates, no drops, as a piston moves blocks. */
    private static final int MOVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_MOVE_BY_PISTON;

    private final Titanosaur beast;
    @Nullable
    private BlockPos harness;
    /** Cargo blocks relative to the harness, as they stand now (already turned). Includes the harness (0, 0, 0). */
    private final List<BlockPos> offsets = new ArrayList<>();
    private int quarter;
    private boolean moving;

    DinoCargo(Titanosaur beast) {
        this.beast = beast;
    }

    public boolean harnessed() {
        return harness != null;
    }

    @Nullable
    public BlockPos harness() {
        return harness;
    }

    public boolean moving() {
        return moving;
    }

    public int size() {
        return offsets.size();
    }

    public boolean contains(BlockPos pos) {
        return harness != null && offsets.contains(pos.subtract(harness));
    }

    /** Where the harness should be now: above the middle of its back. */
    public BlockPos harnessSpot() {
        return BlockPos.containing(beast.getX(), beast.getY() + Titanosaur.BACK, beast.getZ());
    }

    /** Which quarter it faces: its yaw, rounded to the nearest 90 degrees. */
    int quarterNow() {
        return Mth.positiveModulo(Math.round(beast.getYRot() / 90.0f), 4);
    }

    /** A harness is buckled on: its block appears on the beast's back. */
    boolean buckle(Player player, ItemStack stack) {
        ServerLevel level = (ServerLevel) beast.level();
        BlockPos spot = harnessSpot();
        if (!level.getBlockState(spot).canBeReplaced()) {
            player.displayClientMessage(Component.translatable("message.chronogenesis.harness.blocked").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        level.setBlockAndUpdate(spot, ModBlocks.BUCKLED_HARNESS.get().defaultBlockState());
        if (level.getBlockEntity(spot) instanceof DinoHarnessBlockEntity entity) {
            entity.setOwner(beast.getUUID());
        }
        harness = spot;
        offsets.clear();
        offsets.add(BlockPos.ZERO);
        quarter = quarterNow();
        stack.consume(1, player);
        level.playSound(null, spot, SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0f, 0.6f);
        player.displayClientMessage(Component.translatable("message.chronogenesis.harness.buckled").withStyle(ChatFormatting.DARK_AQUA), true);
        return true;
    }

    /** A block was placed touching the cargo: it is part of it now. */
    public boolean attach(BlockPos pos) {
        if (harness == null || offsets.size() >= MAX_BLOCKS) {
            return false;
        }
        BlockPos offset = pos.subtract(harness);
        if (!offsets.contains(offset)) {
            offsets.add(offset.immutable());
        }
        return true;
    }

    public void detach(BlockPos pos) {
        if (harness != null && !pos.equals(harness)) {
            offsets.remove(pos.subtract(harness));
        }
    }

    /**
     * Keeps the cargo on the beast's back. Returns false if it could not follow (something in the way): the beast
     * must then step back.
     */
    public boolean follow() {
        if (harness == null) {
            return true;
        }
        ServerLevel level = (ServerLevel) beast.level();
        if (!level.isLoaded(harness)) {
            return true;
        }
        if (!level.getBlockState(harness).is(ModBlocks.BUCKLED_HARNESS.get())) {
            collapse();
            return true;
        }
        BlockPos spot = harnessSpot();
        int facing = quarterNow();
        if (spot.equals(harness) && facing == quarter) {
            return true;
        }
        return move(spot, facing);
    }

    private boolean move(BlockPos spot, int facing) {
        ServerLevel level = (ServerLevel) beast.level();
        int steps = Mth.positiveModulo(facing - quarter, 4);
        Rotation rotation = Rotation.values()[steps];
        // What the cargo is now (blocks broken off by other means are dropped from it).
        List<BlockPos> from = new ArrayList<>();
        List<BlockPos> keep = new ArrayList<>();
        for (BlockPos offset : offsets) {
            BlockPos pos = harness.offset(offset);
            if (offset.equals(BlockPos.ZERO) || !level.getBlockState(pos).isAir()) {
                from.add(pos);
                keep.add(offset);
            }
        }
        Set<BlockPos> occupied = new HashSet<>(from);
        List<BlockPos> to = new ArrayList<>();
        for (BlockPos offset : keep) {
            BlockPos target = spot.offset(offset.rotate(rotation));
            if (!occupied.contains(target) && (!level.isLoaded(target) || !level.getBlockState(target).canBeReplaced())) {
                return false;
            }
            to.add(target);
        }

        moving = true;
        List<BlockState> states = new ArrayList<>();
        List<CompoundTag> data = new ArrayList<>();
        for (BlockPos pos : from) {
            states.add(level.getBlockState(pos));
            BlockEntity entity = level.getBlockEntity(pos);
            data.add(entity == null ? null : entity.saveWithFullMetadata(level.registryAccess()));
        }
        AABB carried = bounds(from).expandTowards(0, 2, 0);
        for (BlockPos pos : from) {
            level.removeBlockEntity(pos);
        }
        for (BlockPos pos : from) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), MOVE_FLAGS);
        }
        for (int i = 0; i < to.size(); i++) {
            BlockState state = states.get(i).rotate(rotation);
            level.setBlock(to.get(i), state, MOVE_FLAGS);
            if (data.get(i) != null) {
                BlockEntity entity = BlockEntity.loadStatic(to.get(i), state, data.get(i), level.registryAccess());
                if (entity != null) {
                    level.setBlockEntity(entity);
                }
            }
        }
        for (BlockPos pos : from) {
            level.updateNeighborsAt(pos, Blocks.AIR);
        }
        for (int i = 0; i < to.size(); i++) {
            level.updateNeighborsAt(to.get(i), states.get(i).getBlock());
        }
        carry(level, carried, Vec3.atCenterOf(harness), Vec3.atCenterOf(spot), steps);

        offsets.clear();
        for (BlockPos offset : keep) {
            offsets.add(offset.rotate(rotation));
        }
        harness = spot;
        quarter = facing;
        moving = false;
        return true;
    }

    /** Whatever stands on the cargo is moved and turned with it. */
    private void carry(ServerLevel level, AABB area, Vec3 oldCentre, Vec3 newCentre, int steps) {
        for (Entity entity : level.getEntities(beast, area, entity -> !entity.isPassenger() && !(entity instanceof Titanosaur))) {
            Vec3 relative = entity.position().subtract(oldCentre);
            for (int i = 0; i < steps; i++) {
                relative = new Vec3(-relative.z, relative.y, relative.x);
            }
            Vec3 at = newCentre.add(relative);
            float yaw = entity.getYRot() + 90.0f * steps;
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(level, at.x, at.y, at.z, yaw, player.getXRot());
            } else {
                entity.moveTo(at.x, at.y, at.z, yaw, entity.getXRot());
            }
        }
    }

    private static AABB bounds(List<BlockPos> positions) {
        AABB box = new AABB(positions.get(0));
        for (BlockPos pos : positions) {
            box = box.minmax(new AABB(pos));
        }
        return box;
    }

    /** Nothing holds the cargo on any more: every block of it breaks, dropping. */
    public void collapse() {
        if (harness == null) {
            return;
        }
        ServerLevel level = (ServerLevel) beast.level();
        BlockPos at = harness;
        List<BlockPos> blocks = new ArrayList<>(offsets);
        harness = null;
        offsets.clear();
        for (BlockPos offset : blocks) {
            BlockPos pos = at.offset(offset);
            if (!level.getBlockState(pos).isAir()) {
                level.destroyBlock(pos, true);
            }
        }
        level.playSound(null, at, SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 1.5f, 0.5f);
    }

    void save(CompoundTag tag) {
        if (harness == null) {
            return;
        }
        tag.put("harness", NbtUtils.writeBlockPos(harness));
        tag.putInt("quarter", quarter);
        ListTag list = new ListTag();
        for (BlockPos offset : offsets) {
            list.add(NbtUtils.writeBlockPos(offset));
        }
        tag.put("cargo", list);
    }

    void load(CompoundTag tag) {
        harness = NbtUtils.readBlockPos(tag, "harness").orElse(null);
        quarter = tag.getInt("quarter");
        offsets.clear();
        ListTag list = tag.getList("cargo", Tag.TAG_INT_ARRAY);
        for (Tag entry : list) {
            if (entry instanceof net.minecraft.nbt.IntArrayTag array && array.size() == 3) {
                offsets.add(new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()));
            }
        }
        if (harness != null && offsets.isEmpty()) {
            offsets.add(BlockPos.ZERO);
        }
    }
}
