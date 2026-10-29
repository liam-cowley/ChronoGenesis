package com.chronogenesis.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.network.CargoActionPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Where a harnessed Titanosaur's cargo really is ({@link DinoCargo}): the way Valkyrien Skies keeps a ship's blocks,
 * each beast is given a <b>hold</b>, a plot of its own level far out near the world's edge and high in the sky, where
 * its cargo stands as ordinary blocks. They tick there (machines run, chests keep their contents), and everything the
 * player does to them is done there: the client finds the block under the cursor in the beast's frame and the server
 * applies the click to the block in the hold. What the hold holds is mirrored onto the beast, drawn and made solid on
 * its back at whatever angle it stands.
 * <ul>
 *   <li>Holds are {@value #SPACING} blocks apart along x, from {@link #ORIGIN}; a hold's harness stands in the middle,
 *       and the cargo may reach {@link DinoCargo#REACH} blocks round it.</li>
 *   <li>Its chunks are force-loaded while its beast is loaded, so the cargo ticks with the beast.</li>
 *   <li>Anything dropped in a hold (broken blocks, spilled chests, experience) falls from the beast instead.</li>
 *   <li>Reach is measured to where the block is on the beast, not in the hold ({@code mixin/PlayerMixin}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class CargoHold {
    /** The first hold's corner (x and z): far from anything, inside the world border. */
    public static final int ORIGIN = 28_000_000;
    public static final int SPACING = 64;
    /** The harness stands this far into its hold's square. */
    private static final int MIDDLE = 32;
    /** The harness stands this far below the top of the level. */
    private static final int BELOW_TOP = 20;

    private CargoHold() {}

    /** Whether a position is inside the holds' band (anywhere a hold could be). */
    public static boolean isHold(BlockPos pos) {
        return pos.getX() >= ORIGIN && pos.getZ() >= ORIGIN && pos.getZ() < ORIGIN + SPACING;
    }

    public static int indexOf(BlockPos pos) {
        return Math.floorDiv(pos.getX() - ORIGIN, SPACING);
    }

    /** Where the harness of hold {@code index} stands. */
    public static BlockPos harnessOf(Level level, int index) {
        return new BlockPos(ORIGIN + index * SPACING + MIDDLE, level.getMaxBuildHeight() - BELOW_TOP, ORIGIN + MIDDLE);
    }

    /** The chunks a hold covers. */
    static List<ChunkPos> chunks(Level level, int index) {
        BlockPos harness = harnessOf(level, index);
        List<ChunkPos> out = new ArrayList<>();
        int r = DinoCargo.REACH;
        for (int cx = (harness.getX() - r) >> 4; cx <= (harness.getX() + r) >> 4; cx++) {
            for (int cz = (harness.getZ() - r) >> 4; cz <= (harness.getZ() + r) >> 4; cz++) {
                out.add(new ChunkPos(cx, cz));
            }
        }
        return out;
    }

    static void force(ServerLevel level, int index, boolean on) {
        for (ChunkPos chunk : chunks(level, index)) {
            level.setChunkForced(chunk.x, chunk.z, on);
        }
    }

    /** The beast whose hold a position is in (loaded beasts only). */
    @Nullable
    public static Titanosaur beastAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !isHold(pos)) {
            return null;
        }
        UUID owner = holds(server).owners.get(indexOf(pos));
        return owner != null && server.getEntity(owner) instanceof Titanosaur beast && beast.cargo().plot() == indexOf(pos) ? beast : null;
    }

    /** Where a block of a hold is in the world now (its middle), or null if no beast carries it. */
    @Nullable
    public static Vec3 worldOf(Level level, BlockPos pos) {
        Titanosaur beast = beastAt(level, pos);
        return beast == null ? null : beast.cargo().worldOf(Vec3.atCenterOf(pos));
    }

    /** Reach to a block in a hold, measured to where it is on its beast. Null if the position is not in one. */
    @Nullable
    public static Boolean canReach(Player player, BlockPos pos, double extra) {
        if (!isHold(pos) || player.level().isClientSide()) {
            return null;
        }
        Vec3 at = worldOf(player.level(), pos);
        if (at == null) {
            return false;
        }
        double reach = player.blockInteractionRange() + extra + 1.0;
        return new AABB(at, at).inflate(0.5).distanceToSqr(player.getEyePosition()) < reach * reach;
    }

    /**
     * A player used or broke a block of a beast's cargo: done to the block in the hold, as if they stood there, but
     * turned to face it the way they face it on the beast.
     */
    public static void act(ServerPlayer player, CargoActionPayload action) {
        if (!(player.level().getEntity(action.beast()) instanceof Titanosaur beast) || !DinoCargo.within(action.offset())) {
            return;
        }
        DinoCargo cargo = beast.cargo();
        BlockPos harness = cargo.harness();
        if (harness == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = harness.offset(action.offset());
        Vec3 world = cargo.worldOf(Vec3.atCenterOf(pos));
        double reach = player.blockInteractionRange() + 3.0;
        if (player.getEyePosition().distanceToSqr(world) > reach * reach) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (action.breaking()) {
            if (!state.isAir() && player.gameMode.destroyBlock(pos)) {
                level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, BlockPos.containing(world), Block.getId(state));
            }
        } else {
            BlockHitResult hit = new BlockHitResult(Vec3.atLowerCornerOf(harness).add(action.hit()), action.face(), pos, false);
            float yaw = player.getYRot();
            float head = player.getYHeadRot();
            float turn = cargo.turn(beast.getYRot());
            player.setYRot(yaw - turn);
            player.setYHeadRot(head - turn);
            try {
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = player.getItemInHand(hand);
                    BlockPos target = state.canBeReplaced() ? pos : pos.relative(action.face());
                    if (stack.getItem() instanceof BlockItem && !cargo.mayPlace(target)) {
                        player.displayClientMessage(Component.translatable("message.chronogenesis.harness.too_far")
                                .withStyle(ChatFormatting.GRAY), true);
                        break;
                    }
                    BlockState before = level.getBlockState(target);
                    InteractionResult result = player.gameMode.useItemOn(player, level, stack, hand, hit);
                    if (result.consumesAction()) {
                        BlockState after = level.getBlockState(target);
                        if (after != before && !after.isAir()) {
                            SoundType sound = after.getSoundType(level, target, player);
                            level.playSound(null, world.x, world.y, world.z, sound.getPlaceSound(), SoundSource.BLOCKS,
                                    (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
                        }
                        if (result.shouldSwing()) {
                            player.swing(hand, true);
                        }
                        break;
                    }
                }
            } finally {
                player.setYRot(yaw);
                player.setYHeadRot(head);
            }
        }
        cargo.rescan(level);
    }

    /** Things dropped in a hold fall from the beast instead. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || !(entity instanceof ItemEntity || entity instanceof ExperienceOrb)
                || !isHold(entity.blockPosition())) {
            return;
        }
        Vec3 at = worldOf(event.getLevel(), entity.blockPosition());
        if (at != null) {
            entity.setPos(at.x, at.y, at.z);
            entity.setDeltaMovement(entity.getDeltaMovement().scale(0.3));
        }
    }

    /** Holds are forced only while their beasts are loaded: anything left forced from before is let go. */
    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (long chunk : level.getForcedChunks().toLongArray()) {
                ChunkPos pos = new ChunkPos(chunk);
                if (pos.getMinBlockX() >= ORIGIN && pos.getMinBlockZ() >= ORIGIN) {
                    level.setChunkForced(pos.x, pos.z, false);
                }
            }
        }
    }

    // ---------------------------------------------------------------- which beast has which hold

    public static Holds holds(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Holds::new, Holds::load), "chronogenesis_cargo_holds");
    }

    public static class Holds extends SavedData {
        private final Map<Integer, UUID> owners = new HashMap<>();

        static Holds load(CompoundTag tag, HolderLookup.Provider registries) {
            Holds holds = new Holds();
            for (Tag entry : tag.getList("holds", Tag.TAG_COMPOUND)) {
                CompoundTag hold = (CompoundTag) entry;
                holds.owners.put(hold.getInt("index"), hold.getUUID("owner"));
            }
            return holds;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            owners.forEach((index, owner) -> {
                CompoundTag hold = new CompoundTag();
                hold.putInt("index", index);
                hold.putUUID("owner", owner);
                list.add(hold);
            });
            tag.put("holds", list);
            return tag;
        }

        /** The first free hold, now the beast's. */
        int claim(UUID owner) {
            int index = 0;
            while (owners.containsKey(index)) {
                index++;
            }
            owners.put(index, owner);
            setDirty();
            return index;
        }

        /** Whether the hold is the beast's (a beast loaded from an old copy of the world may find it taken). */
        boolean owns(int index, UUID owner) {
            return owner.equals(owners.get(index));
        }

        void free(int index) {
            if (owners.remove(index) != null) {
                setDirty();
            }
        }
    }
}
