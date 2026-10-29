package com.chronogenesis.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import com.chronogenesis.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * What a harnessed Titanosaur carries: real blocks, fixed to its body and turning with it at any angle, the way a
 * Valkyrien Skies ship or a Create contraption carries them. The blocks themselves stand in the beast's
 * {@link CargoHold} (a plot far away, where they tick like any others); the beast mirrors them on its back:
 * <ul>
 *   <li>The harness is the cargo's origin. It sits above the middle of the back, and the cargo's frame turns with the
 *       beast: the cargo is never squared to the world's grid.</li>
 *   <li>The client finds the block under the cursor in that frame ({@code client/CargoControls}); placing against it,
 *       breaking it, opening it or using it is done to the real block in the hold. Blocks may reach {@value #REACH}
 *       blocks out from the harness, {@value #DOWN} down and {@value #UP} up, {@value #MAX_BLOCKS} at most.</li>
 *   <li>Its shape is solid ({@link CargoPart}s: you can stand on it, and are carried along); it cannot pass through
 *       terrain (the beast stops). A Rider's Seat in it steers.</li>
 * </ul>
 * If the beast dies, or the harness is broken, nothing holds the cargo on: every block of it drops.
 */
public final class DinoCargo {
    public static final int MAX_BLOCKS = 384;
    public static final int REACH = 12;
    public static final int DOWN = 6;
    public static final int UP = 16;
    /** Ticks between looks at the hold for changes (machines growing, doors opening). */
    private static final int RESCAN = 10;
    /** Where the harness's corner sits relative to the beast's feet (it is centred over the back). */
    private static final Vec3 ANCHOR = new Vec3(-0.5, Titanosaur.BACK, -0.5);

    /** One block of the cargo as the clients see it: where it sits relative to the harness, and what it is. */
    public record Carried(BlockPos offset, BlockState state) {}

    private final Titanosaur beast;
    /** The hold (server), or -1. */
    private int plot = -1;
    private boolean forced;
    private boolean collapsing;
    private final List<Carried> carried = new ArrayList<>();
    private List<AABB> shape = List.of();
    private int scan;
    /** A cargo saved by an older version (lifted blocks, or blocks standing in the world), moved into a hold on loading. */
    @Nullable
    private CompoundTag legacy;

    DinoCargo(Titanosaur beast) {
        this.beast = beast;
    }

    public boolean harnessed() {
        return plot >= 0 || !carried.isEmpty() || legacy != null;
    }

    public boolean carrying() {
        return !carried.isEmpty();
    }

    public int plot() {
        return plot;
    }

    /** The harness in the hold (server). */
    @Nullable
    public BlockPos harness() {
        return plot < 0 ? null : CargoHold.harnessOf(beast.level(), plot);
    }

    public int size() {
        return carried.size();
    }

    public List<Carried> carried() {
        return carried;
    }

    public Vec3 anchor() {
        return ANCHOR;
    }

    /** The cargo's solid shape, a few boxes in its own frame (see {@link CargoPart#merge}). */
    public List<AABB> shape() {
        return shape;
    }

    private void reshape() {
        shape = carried.isEmpty() ? List.of() : CargoPart.merge(carried.stream().map(Carried::offset).toList());
    }

    /** How far the cargo has turned from the world's axes: as far as the beast. */
    public float turn(float yaw) {
        return yaw;
    }

    /** A point of the cargo's frame (relative to the harness corner) in the world, with the beast at {@code beastPos}. */
    public Vec3 toWorld(Vec3 local, Vec3 beastPos, float yaw) {
        return beastPos.add(rotate(ANCHOR.add(local), turn(yaw)));
    }

    /** A point of the world in the cargo's frame. */
    public Vec3 toLocal(Vec3 world, Vec3 beastPos, float yaw) {
        return rotate(world.subtract(beastPos), -turn(yaw)).subtract(ANCHOR);
    }

    /** Where a point of the hold is in the world now. */
    public Vec3 worldOf(Vec3 inHold) {
        BlockPos harness = harness();
        return harness == null ? beast.position() : toWorld(inHold.subtract(Vec3.atLowerCornerOf(harness)), beast.position(), beast.getYRot());
    }

    /** Turns a vector about the vertical by {@code degrees} of yaw (Minecraft's sense: +90 takes south to west). */
    public static Vec3 rotate(Vec3 v, float degrees) {
        float r = degrees * Mth.DEG_TO_RAD;
        float cos = Mth.cos(r);
        float sin = Mth.sin(r);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** Whether an offset from the harness is inside what a cargo may fill. */
    public static boolean within(BlockPos offset) {
        return Math.abs(offset.getX()) <= REACH && Math.abs(offset.getZ()) <= REACH && offset.getY() >= -DOWN && offset.getY() <= UP;
    }

    /** A harness is buckled on: its hold is claimed, and the harness appears on the beast's back. */
    boolean buckle(Player player, ItemStack stack) {
        ServerLevel level = (ServerLevel) beast.level();
        claim(level);
        BlockPos harness = harness();
        level.setBlockAndUpdate(harness, ModBlocks.BUCKLED_HARNESS.get().defaultBlockState());
        if (level.getBlockEntity(harness) instanceof DinoHarnessBlockEntity entity) {
            entity.setOwner(beast.getUUID());
        }
        rescan(level);
        stack.consume(1, player);
        level.playSound(null, beast.blockPosition().above(Titanosaur.BACK), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0f, 0.6f);
        player.displayClientMessage(Component.translatable("message.chronogenesis.harness.buckled").withStyle(ChatFormatting.DARK_AQUA), true);
        return true;
    }

    private void claim(ServerLevel level) {
        plot = CargoHold.holds(level).claim(beast.getUUID());
        BlockPos harness = harness();
        // A hold is handed out empty; clear anything a lost beast left in it.
        CargoHold.force(level, plot, true);
        forced = true;
        for (BlockPos pos : BlockPos.betweenClosed(harness.offset(-REACH, -DOWN, -REACH), harness.offset(REACH, UP, REACH))) {
            if (!level.getBlockState(pos).isAir()) {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }

    /** Server, every tick: keeps the hold loaded, and the mirror up to date. */
    public void tick() {
        ServerLevel level = (ServerLevel) beast.level();
        if (legacy != null) {
            migrate(level, legacy);
            legacy = null;
        }
        if (plot < 0) {
            return;
        }
        if (!forced) {
            if (!CargoHold.holds(level).owns(plot, beast.getUUID())) {
                // Someone else's hold now (a copy of this beast from an older save): this one has lost its cargo.
                plot = -1;
                carried.clear();
                reshape();
                beast.cargoChanged();
                return;
            }
            CargoHold.force(level, plot, true);
            forced = true;
        }
        BlockPos harness = harness();
        if (!level.isLoaded(harness)) {
            return;
        }
        if (!level.getBlockState(harness).is(ModBlocks.BUCKLED_HARNESS.get())) {
            collapse();
            return;
        }
        if (++scan >= RESCAN) {
            rescan(level);
        }
    }

    /** The beast left the level (unloaded or gone): its hold need not stay loaded. */
    public void unload() {
        if (forced && plot >= 0 && beast.level() instanceof ServerLevel level) {
            CargoHold.force(level, plot, false);
        }
        forced = false;
    }

    /** Looks at the hold again, and tells the clients if the cargo changed. */
    public void rescan(ServerLevel level) {
        scan = 0;
        BlockPos harness = harness();
        if (harness == null) {
            return;
        }
        List<Carried> now = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(harness.offset(-REACH, -DOWN, -REACH), harness.offset(REACH, UP, REACH))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                now.add(new Carried(pos.subtract(harness), state));
            }
        }
        if (!now.equals(carried)) {
            carried.clear();
            carried.addAll(now);
            reshape();
            beast.cargoChanged();
        }
    }

    /** Whether one more block may be placed in the hold, at {@code pos}. */
    public boolean mayPlace(BlockPos pos) {
        BlockPos harness = harness();
        return harness != null && within(pos.subtract(harness)) && carried.size() < MAX_BLOCKS;
    }

    /** Whether the cargo, with the beast at {@code pos} facing {@code yaw}, would be inside terrain. */
    public boolean blocked(Level level, Vec3 pos, float yaw) {
        for (AABB box : CargoPart.boxes(this, pos, yaw)) {
            if (!level.noBlockCollision(null, box.deflate(0.08))) {
                return true;
            }
        }
        return false;
    }

    /** Nothing holds the cargo on any more: every block of it breaks, dropping from the beast, and the hold is let go. */
    public void collapse() {
        ServerLevel level = (ServerLevel) beast.level();
        if (plot < 0 || collapsing) {
            return;
        }
        collapsing = true;
        BlockPos harness = harness();
        // Broken while the beast still owns the hold, so what falls lands where the beast is.
        for (BlockPos pos : BlockPos.betweenClosed(harness.offset(-REACH, -DOWN, -REACH), harness.offset(REACH, UP, REACH))) {
            if (!level.getBlockState(pos).isAir()) {
                level.destroyBlock(pos.immutable(), true);
            }
        }
        CargoHold.force(level, plot, false);
        CargoHold.holds(level).free(plot);
        plot = -1;
        forced = false;
        collapsing = false;
        carried.clear();
        reshape();
        level.playSound(null, beast.blockPosition().above(Titanosaur.BACK), SoundEvents.CHAIN_BREAK, SoundSource.NEUTRAL, 1.5f, 0.5f);
        beast.cargoChanged();
    }

    // ---------------------------------------------------------------- saving and syncing

    void save(CompoundTag tag) {
        if (plot >= 0) {
            tag.putInt("hold", plot);
        }
        if (legacy != null) {
            tag.merge(legacy);
        }
    }

    void load(CompoundTag tag) {
        plot = tag.contains("hold") ? tag.getInt("hold") : -1;
        forced = false;
        if (plot < 0 && (tag.contains("harness") || tag.contains("carried"))) {
            legacy = tag.copy();
        }
    }

    /** An older cargo (anchored in the world, or lifted with its blocks' data) is moved into a hold. */
    private void migrate(ServerLevel level, CompoundTag tag) {
        claim(level);
        BlockPos harness = harness();
        // The old cargo kept to the world's quarters; the new one keeps to the beast. Turn it into the beast's frame.
        int quarter = Mth.positiveModulo(Math.round(beast.getYRot() / 90.0f), 4);
        Rotation back = Rotation.values()[(4 - quarter) % 4];
        BlockPos oldHarness = NbtUtils.readBlockPos(tag, "harness").orElse(null);
        if (oldHarness != null) {
            for (Tag entry : tag.getList("cargo", Tag.TAG_INT_ARRAY)) {
                if (entry instanceof IntArrayTag array && array.size() == 3) {
                    BlockPos offset = new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
                    BlockPos from = oldHarness.offset(offset);
                    BlockState state = level.getBlockState(from);
                    if (state.isAir()) {
                        continue;
                    }
                    BlockEntity entity = level.getBlockEntity(from);
                    CompoundTag data = entity == null ? null : entity.saveWithFullMetadata(level.registryAccess());
                    level.removeBlockEntity(from);
                    level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                    put(level, harness.offset(offset.rotate(back)), state.rotate(back), data);
                }
            }
        }
        CompoundTag lifted = tag.getCompound("carried");
        if (lifted.contains("blocks")) {
            Rotation turn = Rotation.values()[Mth.positiveModulo(-lifted.getInt("quarter"), 4)];
            HolderGetter<Block> blocks = level.holderLookup(Registries.BLOCK);
            List<BlockState> palette = new ArrayList<>();
            for (Tag entry : lifted.getList("palette", Tag.TAG_COMPOUND)) {
                palette.add(NbtUtils.readBlockState(blocks, (CompoundTag) entry));
            }
            for (Tag entry : lifted.getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag block = (CompoundTag) entry;
                int i = block.getInt("state");
                if (i < palette.size()) {
                    BlockPos offset = NbtUtils.readBlockPos(block, "pos").orElse(BlockPos.ZERO);
                    put(level, harness.offset(offset.rotate(turn)), palette.get(i).rotate(turn),
                            block.contains("data") ? block.getCompound("data") : null);
                }
            }
        }
        if (!level.getBlockState(harness).is(ModBlocks.BUCKLED_HARNESS.get())) {
            level.setBlockAndUpdate(harness, ModBlocks.BUCKLED_HARNESS.get().defaultBlockState());
        }
        if (level.getBlockEntity(harness) instanceof DinoHarnessBlockEntity entity) {
            entity.setOwner(beast.getUUID());
        }
        rescan(level);
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state, @Nullable CompoundTag data) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        if (data != null) {
            BlockEntity entity = BlockEntity.loadStatic(pos, state, data, level.registryAccess());
            if (entity != null) {
                level.setBlockEntity(entity);
            }
        }
    }

    /** The cargo as a tag: what the clients draw. */
    CompoundTag writeCarried() {
        CompoundTag tag = new CompoundTag();
        if (carried.isEmpty()) {
            return tag;
        }
        ListTag palette = new ListTag();
        Map<BlockState, Integer> index = new HashMap<>();
        int[] packed = new int[carried.size() * 2];
        for (int i = 0; i < carried.size(); i++) {
            Carried block = carried.get(i);
            packed[i * 2] = index.computeIfAbsent(block.state(), state -> {
                palette.add(NbtUtils.writeBlockState(state));
                return palette.size() - 1;
            });
            BlockPos o = block.offset();
            packed[i * 2 + 1] = ((o.getX() + 128) << 16) | ((o.getY() + 128) << 8) | (o.getZ() + 128);
        }
        tag.put("palette", palette);
        tag.putIntArray("blocks", packed);
        return tag;
    }

    /** Reads what {@link #writeCarried} wrote (the client does this when the beast's synced cargo changes). */
    public void readCarried(CompoundTag tag, HolderGetter<Block> blocks) {
        carried.clear();
        List<BlockState> palette = new ArrayList<>();
        for (Tag entry : tag.getList("palette", Tag.TAG_COMPOUND)) {
            palette.add(NbtUtils.readBlockState(blocks, (CompoundTag) entry));
        }
        int[] packed = tag.getIntArray("blocks");
        for (int i = 0; i + 1 < packed.length; i += 2) {
            int p = packed[i + 1];
            BlockPos offset = new BlockPos(((p >> 16) & 0xFF) - 128, ((p >> 8) & 0xFF) - 128, (p & 0xFF) - 128);
            carried.add(new Carried(offset, packed[i] < palette.size() ? palette.get(packed[i]) : Blocks.AIR.defaultBlockState()));
        }
        reshape();
    }
}
