package com.chronogenesis.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * The solid shape of a Titanosaur's cargo ({@link DinoCargo}): the cargo's blocks merged into boxes of a few blocks
 * across, each a part of the beast that other things collide with (you can stand on it; it carries you along). Boxes
 * are kept square to the world, so at a slant each covers a little more than its blocks. Unused parts are empty and
 * pass through everything.
 */
public class CargoPart extends PartEntity<Titanosaur> {
    /** How many boxes a cargo's shape is merged into, at most. */
    public static final int COUNT = 32;
    /** A box is at most this many blocks across, so that turned at a slant it covers little more than its blocks. */
    private static final int SIDE = 5;
    private AABB box = new AABB(0, 0, 0, 0, 0, 0);
    private boolean active;

    public CargoPart(Titanosaur parent) {
        super(parent);
        noPhysics = true;
    }

    void place(AABB box) {
        this.box = box;
        this.active = true;
        setPosRaw((box.minX + box.maxX) / 2, box.minY, (box.minZ + box.maxZ) / 2);
        setBoundingBox(box);
    }

    void clear() {
        active = false;
        Vec3 at = getParent().position();
        box = new AABB(at, at);
        setPosRaw(at.x, at.y, at.z);
        setBoundingBox(box);
    }

    public boolean active() {
        return active;
    }

    @Override
    protected AABB makeBoundingBox() {
        // Null while the entity is being constructed (before this class's fields are set).
        return box != null ? box : super.makeBoundingBox();
    }

    @Override
    public boolean canBeCollidedWith() {
        return active;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    // ---------------------------------------------------------------- the cargo's shape

    /**
     * The cargo's blocks (offsets from the harness) merged into at most {@link #COUNT} boxes in its own frame: each
     * layer greedily into rectangles, rectangles stacked where the layer above repeats them; past the limit, whole
     * layers become one box each, then neighbouring boxes are joined.
     */
    public static List<AABB> merge(List<BlockPos> blocks) {
        Set<BlockPos> left = new HashSet<>(blocks);
        List<int[]> rects = new ArrayList<>(); // minX, minY, minZ, maxX, maxY, maxZ (inclusive)
        List<BlockPos> order = new ArrayList<>(blocks);
        order.sort((a, b) -> a.getY() != b.getY() ? a.getY() - b.getY() : a.getZ() != b.getZ() ? a.getZ() - b.getZ() : a.getX() - b.getX());
        for (BlockPos start : order) {
            if (!left.contains(start)) {
                continue;
            }
            int y = start.getY();
            int x1 = start.getX();
            while (x1 - start.getX() + 1 < SIDE && left.contains(new BlockPos(x1 + 1, y, start.getZ()))) {
                x1++;
            }
            int z1 = start.getZ();
            grow:
            while (z1 - start.getZ() + 1 < SIDE) {
                for (int x = start.getX(); x <= x1; x++) {
                    if (!left.contains(new BlockPos(x, y, z1 + 1))) {
                        break grow;
                    }
                }
                z1++;
            }
            for (int x = start.getX(); x <= x1; x++) {
                for (int z = start.getZ(); z <= z1; z++) {
                    left.remove(new BlockPos(x, y, z));
                }
            }
            int[] rect = {start.getX(), y, start.getZ(), x1, y, z1};
            boolean stacked = false;
            for (int[] r : rects) {
                if (r[4] == y - 1 && r[0] == rect[0] && r[2] == rect[2] && r[3] == rect[3] && r[5] == rect[5]) {
                    r[4] = y;
                    stacked = true;
                    break;
                }
            }
            if (!stacked) {
                rects.add(rect);
            }
        }
        while (rects.size() > COUNT) {
            // Join the two boxes whose union adds the least.
            int bi = 0;
            int bj = 1;
            long best = Long.MAX_VALUE;
            for (int i = 0; i < rects.size(); i++) {
                for (int j = i + 1; j < rects.size(); j++) {
                    long cost = volume(union(rects.get(i), rects.get(j))) - volume(rects.get(i)) - volume(rects.get(j));
                    if (cost < best) {
                        best = cost;
                        bi = i;
                        bj = j;
                    }
                }
            }
            int[] joined = union(rects.get(bi), rects.get(bj));
            rects.remove(bj);
            rects.set(bi, joined);
        }
        List<AABB> boxes = new ArrayList<>();
        for (int[] r : rects) {
            boxes.add(new AABB(r[0], r[1], r[2], r[3] + 1, r[4] + 1, r[5] + 1));
        }
        return boxes;
    }

    private static int[] union(int[] a, int[] b) {
        return new int[] {Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                Math.max(a[3], b[3]), Math.max(a[4], b[4]), Math.max(a[5], b[5])};
    }

    private static long volume(int[] r) {
        return (long) (r[3] - r[0] + 1) * (r[4] - r[1] + 1) * (r[5] - r[2] + 1);
    }

    /** The cargo's boxes in the world, with the beast at {@code pos} facing {@code yaw}. */
    public static List<AABB> boxes(DinoCargo cargo, Vec3 pos, float yaw) {
        List<AABB> out = new ArrayList<>();
        for (AABB local : cargo.shape()) {
            Vec3 a = cargo.toWorld(new Vec3(local.minX, local.minY, local.minZ), pos, yaw);
            Vec3 b = cargo.toWorld(new Vec3(local.maxX, local.minY, local.minZ), pos, yaw);
            Vec3 c = cargo.toWorld(new Vec3(local.minX, local.minY, local.maxZ), pos, yaw);
            Vec3 d = cargo.toWorld(new Vec3(local.maxX, local.minY, local.maxZ), pos, yaw);
            double minX = Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x));
            double maxX = Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x));
            double minZ = Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z));
            double maxZ = Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z));
            out.add(new AABB(minX, a.y, minZ, maxX, a.y + local.getYsize(), maxZ));
        }
        return out;
    }
}
