package com.chronogenesis.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import com.chronogenesis.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What a harnessed Titanosaur carries: real blocks, built onto its back, carried the way a contraption is (Create's
 * moving structures): while it stands, its cargo stands in the world and can be built on; when it walks, the cargo
 * comes away from the world and goes with it.
 * <ul>
 *   <li><b>Anchored</b> (standing): the cargo is blocks in the world on its back. Every block placed touching the
 *       harness, or touching anything already attached, joins it; a block broken off leaves it. Machines run.</li>
 *   <li><b>Carried</b> (walking): the moment it is steered (or anything moves it), the whole cargo is lifted out of
 *       the world into the beast, block entities with all their data. It is drawn on the beast, turning smoothly
 *       with it ({@code client/DinoRenderers}); its shape is solid ({@link CargoPart}s: you can stand on it, and are
 *       carried along); it cannot pass through terrain (the beast stops). A Rider's Seat in it still steers.</li>
 *   <li>When it has stood still a moment, the cargo is set down again where it now stands, squared to the nearest
 *       quarter turn (states rotated), block entities restored: anchored again. If something is in the way it waits.</li>
 * </ul>
 * If the beast dies, nothing holds the cargo on: every block of it drops (containers spill).
 */
public final class DinoCargo {
    /** How many blocks one beast can carry. */
    public static final int MAX_BLOCKS = 384;
    /** Ticks without being steered before a carried cargo is set down. */
    public static final int SETTLE = 30;
    /** Block flags for moving: tell clients, no shape updates, no drops, as a piston moves blocks. */
    private static final int MOVE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_MOVE_BY_PISTON;

    /** One carried block: where it sits (relative to the harness at lifting), what it is, its block entity's data. */
    public record Carried(BlockPos offset, BlockState state, @Nullable CompoundTag data) {}

    private final Titanosaur beast;
    // Anchored: the harness in the world, and the cargo blocks relative to it.
    @Nullable
    private BlockPos harness;
    private final List<BlockPos> offsets = new ArrayList<>();
    @Nullable
    private Vec3 anchoredAt;
    private float anchoredYaw;
    // Carried: the blocks themselves, where the harness corner was relative to the beast, and the quarter it faced.
    private final List<Carried> carried = new ArrayList<>();
    private Vec3 anchor = Vec3.ZERO;
    private List<AABB> shape = List.of();
    private int quarter;
    private boolean moving;
    private int still;

    DinoCargo(Titanosaur beast) {
        this.beast = beast;
    }

    public boolean harnessed() {
        return harness != null || !carried.isEmpty();
    }

    public boolean carrying() {
        return !carried.isEmpty();
    }

    @Nullable
    public BlockPos harness() {
        return harness;
    }

    public boolean moving() {
        return moving;
    }

    public int size() {
        return carrying() ? carried.size() : offsets.size();
    }

    public List<Carried> carried() {
        return carried;
    }

    public Vec3 anchor() {
        return anchor;
    }

    /** The carried cargo's solid shape, a few boxes in its own frame (see {@link CargoPart#merge}). */
    public List<AABB> shape() {
        return shape;
    }

    private void reshape() {
        shape = carried.isEmpty() ? List.of() : CargoPart.merge(carried.stream().map(Carried::offset).toList());
    }

    /** Whether a block in the world is part of the (anchored) cargo. */
    public boolean contains(BlockPos pos) {
        return harness != null && offsets.contains(pos.subtract(harness));
    }

    /** Where the harness should be: above the middle of its back. */
    public BlockPos harnessSpot() {
        return BlockPos.containing(beast.getX(), beast.getY() + Titanosaur.BACK, beast.getZ());
    }

    /** Which quarter it faces: its yaw, rounded to the nearest 90 degrees. */
    int quarterNow() {
        return Mth.positiveModulo(Math.round(beast.getYRot() / 90.0f), 4);
    }

    /** How far a carried cargo has turned since it was lifted (degrees, yaw sense). */
    public float turn(float yaw) {
        return yaw - quarter * 90.0f;
    }

    /** A point of the carried cargo (relative to the harness corner at lifting) in the world now. */
    public Vec3 toWorld(Vec3 local, Vec3 beastPos, float yaw) {
        return beastPos.add(rotate(anchor.add(local), turn(yaw)));
    }

    /** Turns a vector about the vertical by {@code degrees} of yaw (Minecraft's sense: +90 takes south to west). */
    public static Vec3 rotate(Vec3 v, float degrees) {
        float r = degrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(r);
        float sin = Mth.sin(r);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
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
        markAnchored();
        stack.consume(1, player);
        level.playSound(null, spot, SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0f, 0.6f);
        player.displayClientMessage(Component.translatable("message.chronogenesis.harness.buckled").withStyle(ChatFormatting.DARK_AQUA), true);
        return true;
    }

    private void markAnchored() {
        anchoredAt = beast.position();
        anchoredYaw = beast.getYRot();
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
     * Server, every tick. Anchored: if the beast is steered or has been moved off its spot, the cargo is lifted.
     * Carried: once it has stood still a while, the cargo is set down.
     */
    public void tick(boolean steered) {
        ServerLevel level = (ServerLevel) beast.level();
        if (harness != null) {
            if (!level.isLoaded(harness)) {
                return;
            }
            if (!level.getBlockState(harness).is(ModBlocks.BUCKLED_HARNESS.get())) {
                collapse();
                return;
            }
            if (anchoredAt == null) {
                markAnchored();
            }
            boolean moved = beast.position().distanceToSqr(anchoredAt) > 0.36
                    || Math.abs(Mth.wrapDegrees(beast.getYRot() - anchoredYaw)) > 20.0f;
            if (steered || moved) {
                lift(level);
            }
            return;
        }
        if (carrying()) {
            boolean resting = !steered && beast.onGround() && beast.getDeltaMovement().horizontalDistanceSqr() < 1.0E-3;
            still = resting ? still + 1 : 0;
            if (still >= SETTLE && still % 10 == 0) {
                setDown(level);
            }
        }
    }

    /** Lifts the anchored cargo out of the world into the beast. */
    private void lift(ServerLevel level) {
        int facing = quarterNow();
        // Squared to the quarter it faces, so the blocks keep to its body.
        float yaw = facing * 90.0f;
        beast.setYRot(yaw);
        beast.yBodyRot = yaw;
        beast.yHeadRot = yaw;
        List<BlockPos> from = new ArrayList<>();
        for (BlockPos offset : offsets) {
            BlockPos pos = harness.offset(offset);
            if (offset.equals(BlockPos.ZERO) || !level.getBlockState(pos).isAir()) {
                from.add(pos);
            }
        }
        moving = true;
        carried.clear();
        Map<BlockPos, RiderSeat> seats = new HashMap<>();
        for (RiderSeat seat : level.getEntitiesOfClass(RiderSeat.class, bounds(from).inflate(1.0))) {
            seats.put(seat.blockPosition(), seat);
        }
        for (BlockPos pos : from) {
            BlockEntity entity = level.getBlockEntity(pos);
            carried.add(new Carried(pos.subtract(harness), level.getBlockState(pos),
                    entity == null ? null : entity.saveWithFullMetadata(level.registryAccess())));
        }
        for (BlockPos pos : from) {
            level.removeBlockEntity(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), MOVE_FLAGS);
        }
        for (BlockPos pos : from) {
            level.updateNeighborsAt(pos, Blocks.AIR);
        }
        anchor = Vec3.atLowerCornerOf(harness).subtract(beast.position());
        quarter = facing;
        BlockPos was = harness;
        seats.forEach((pos, seat) -> seat.carriedBy(beast, pos.subtract(was)));
        harness = null;
        offsets.clear();
        moving = false;
        still = 0;
        reshape();
        beast.cargoChanged();
        level.playSound(null, beast.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0f, 0.6f);
    }

    /** Sets the carried cargo down where it now stands, squared to the nearest quarter. Waits if anything is in the way. */
    public boolean setDown(ServerLevel level) {
        int facing = quarterNow();
        float yaw = facing * 90.0f;
        int steps = Mth.positiveModulo(facing - quarter, 4);
        Rotation rotation = Rotation.values()[steps];
        Vec3 at = beast.position();
        List<BlockPos> to = new ArrayList<>();
        Set<BlockPos> taken = new HashSet<>();
        for (Carried block : carried) {
            BlockPos target = BlockPos.containing(toWorld(Vec3.atCenterOf(block.offset()), at, yaw));
            if (!taken.add(target) || !level.isLoaded(target) || !level.getBlockState(target).canBeReplaced()) {
                return false;
            }
            to.add(target);
        }
        beast.setYRot(yaw);
        beast.yBodyRot = yaw;
        beast.yHeadRot = yaw;
        moving = true;
        BlockPos newHarness = to.get(0);
        for (int i = 0; i < carried.size(); i++) {
            Carried block = carried.get(i);
            BlockState state = block.state().rotate(rotation);
            BlockPos pos = to.get(i);
            level.setBlock(pos, state, MOVE_FLAGS);
            if (block.data() != null) {
                BlockEntity entity = BlockEntity.loadStatic(pos, state, block.data(), level.registryAccess());
                if (entity != null) {
                    level.setBlockEntity(entity);
                }
            }
            if (block.offset().equals(BlockPos.ZERO)) {
                newHarness = pos;
            }
        }
        offsets.clear();
        for (int i = 0; i < carried.size(); i++) {
            level.updateNeighborsAt(to.get(i), carried.get(i).state().getBlock());
            offsets.add(to.get(i).subtract(newHarness));
        }
        harness = newHarness;
        BlockPos placed = newHarness;
        for (RiderSeat seat : level.getEntitiesOfClass(RiderSeat.class, beast.getBoundingBox().inflate(32.0), seat -> seat.carrier() == beast)) {
            seat.setDown(placed.offset(seat.seatOffset().rotate(rotation)));
        }
        carried.clear();
        reshape();
        markAnchored();
        moving = false;
        still = 0;
        beast.cargoChanged();
        level.playSound(null, newHarness, SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0f, 0.8f);
        return true;
    }

    /** Whether the carried cargo, with the beast at {@code pos} facing {@code yaw}, would be inside terrain. */
    public boolean blocked(Level level, Vec3 pos, float yaw) {
        for (AABB box : CargoPart.boxes(this, pos, yaw)) {
            if (!level.noBlockCollision(null, box.deflate(0.08))) {
                return true;
            }
        }
        return false;
    }

    /** Nothing holds the cargo on any more: every block of it breaks, dropping. */
    public void collapse() {
        ServerLevel level = (ServerLevel) beast.level();
        if (harness != null) {
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
        if (carrying()) {
            for (Carried block : carried) {
                BlockPos pos = BlockPos.containing(toWorld(Vec3.atCenterOf(block.offset()), beast.position(), beast.getYRot()));
                BlockEntity entity = block.data() == null ? null : BlockEntity.loadStatic(pos, block.state(), block.data(), level.registryAccess());
                if (entity != null) {
                    entity.setLevel(level);
                }
                Block.dropResources(block.state(), level, pos, entity);
                if (entity instanceof Container container) {
                    Containers.dropContents(level, pos, container);
                }
            }
            carried.clear();
            reshape();
            level.playSound(null, beast.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 1.5f, 0.5f);
            beast.cargoChanged();
        }
    }

    private static AABB bounds(List<BlockPos> positions) {
        AABB box = new AABB(positions.get(0));
        for (BlockPos pos : positions) {
            box = box.minmax(new AABB(pos));
        }
        return box;
    }

    // ---------------------------------------------------------------- saving and syncing

    void save(CompoundTag tag) {
        if (harness != null) {
            tag.put("harness", NbtUtils.writeBlockPos(harness));
            ListTag list = new ListTag();
            for (BlockPos offset : offsets) {
                list.add(NbtUtils.writeBlockPos(offset));
            }
            tag.put("cargo", list);
        }
        if (carrying()) {
            tag.put("carried", writeCarried(true));
        }
    }

    void load(CompoundTag tag) {
        harness = NbtUtils.readBlockPos(tag, "harness").orElse(null);
        offsets.clear();
        for (Tag entry : tag.getList("cargo", Tag.TAG_INT_ARRAY)) {
            if (entry instanceof net.minecraft.nbt.IntArrayTag array && array.size() == 3) {
                offsets.add(new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt()));
            }
        }
        if (harness != null && offsets.isEmpty()) {
            offsets.add(BlockPos.ZERO);
        }
        readCarried(tag.getCompound("carried"), beast.level().holderLookup(Registries.BLOCK));
        anchoredAt = null;
    }

    /** The carried cargo as a tag: what the client draws (and, with block entities' data, what is saved). */
    CompoundTag writeCarried(boolean withData) {
        CompoundTag tag = new CompoundTag();
        if (carried.isEmpty()) {
            return tag;
        }
        ListTag palette = new ListTag();
        Map<BlockState, Integer> index = new HashMap<>();
        ListTag blocks = new ListTag();
        for (Carried block : carried) {
            int i = index.computeIfAbsent(block.state(), state -> {
                palette.add(NbtUtils.writeBlockState(state));
                return palette.size() - 1;
            });
            CompoundTag entry = new CompoundTag();
            entry.put("pos", NbtUtils.writeBlockPos(block.offset()));
            entry.putInt("state", i);
            if (withData && block.data() != null) {
                entry.put("data", block.data());
            }
            blocks.add(entry);
        }
        tag.put("palette", palette);
        tag.put("blocks", blocks);
        tag.putDouble("ax", anchor.x);
        tag.putDouble("ay", anchor.y);
        tag.putDouble("az", anchor.z);
        tag.putInt("quarter", quarter);
        return tag;
    }

    /** Reads what {@link #writeCarried} wrote (the client does this when the beast's synced cargo changes). */
    public void readCarried(CompoundTag tag, HolderGetter<Block> blocks) {
        carried.clear();
        if (!tag.contains("blocks")) {
            reshape();
            return;
        }
        List<BlockState> palette = new ArrayList<>();
        for (Tag entry : tag.getList("palette", Tag.TAG_COMPOUND)) {
            palette.add(NbtUtils.readBlockState(blocks, (CompoundTag) entry));
        }
        for (Tag entry : tag.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag block = (CompoundTag) entry;
            BlockPos offset = NbtUtils.readBlockPos(block, "pos").orElse(BlockPos.ZERO);
            int i = block.getInt("state");
            carried.add(new Carried(offset, i < palette.size() ? palette.get(i) : Blocks.AIR.defaultBlockState(),
                    block.contains("data") ? block.getCompound("data") : null));
        }
        anchor = new Vec3(tag.getDouble("ax"), tag.getDouble("ay"), tag.getDouble("az"));
        quarter = tag.getInt("quarter");
        reshape();
    }
}
