package com.chronogenesis.organism;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.Config;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.essence.MobEssence;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Behaviour of all graft types. Finds its organism through the flesh it grows on. */
public class GraftBlockEntity extends BlockEntity {
    private static final int LEECH_INTERVAL = 40;
    private static final int LEECH_RANGE = 4;
    private static final int TAP_INTERVAL = 20;
    private static final int TAP_RATE = 4;

    @Nullable
    private BlockPos cachedCore;
    private int tick;

    public GraftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAFT.get(), pos, state);
    }

    public GraftType type() {
        return ((GraftBlock) getBlockState().getBlock()).type();
    }

    /** The organism this graft belongs to, or null if it is cut off or dead. */
    @Nullable
    public OrganismCoreBlockEntity core() {
        if (level == null) {
            return null;
        }
        if (cachedCore != null && level.getBlockEntity(cachedCore) instanceof OrganismCoreBlockEntity core
                && OrganismScan.isTissue(level.getBlockState(GraftBlock.supportPos(getBlockState(), worldPosition)), core.aspect())) {
            return core.isNecrotic() ? null : core;
        }
        OrganismCoreBlockEntity found = OrganismScan.findCore(level, GraftBlock.supportPos(getBlockState(), worldPosition),
                Config.MAX_ORGANISM_SIZE.getAsInt());
        cachedCore = found == null ? null : found.getBlockPos();
        return found == null || found.isNecrotic() ? null : found;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GraftBlockEntity graft) {
        graft.tick++;
        switch (graft.type()) {
            case LEECH_TENDRIL -> {
                if (graft.tick % LEECH_INTERVAL == 0) {
                    graft.leech((ServerLevel) level);
                }
            }
            case TAP -> {
                if (graft.tick % TAP_INTERVAL == 0) {
                    graft.tapIntoJug(level);
                }
            }
            default -> { }
        }
    }

    /** Leech Tendril: drain 1 dram of the organism's aspect from every mob in range. */
    private void leech(ServerLevel level) {
        OrganismCoreBlockEntity core = core();
        if (core == null || core.room() <= 0 || core.aspect() == Aspect.TIME) {
            return; // Time cannot be leeched without Stasis Bladders.
        }
        Aspect aspect = core.aspect();
        AABB area = new AABB(worldPosition).inflate(LEECH_RANGE);
        Vec3 mouth = Vec3.atCenterOf(worldPosition);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, MobEssence::canBeDrained)) {
            if (core.room() <= 0) {
                break;
            }
            int taken = MobEssence.drain(entity, aspect, 1);
            if (taken > 0) {
                core.feed(taken);
                Vec3 from = entity.position().add(0, entity.getBbHeight() / 2, 0);
                Vec3 step = mouth.subtract(from).scale(0.2);
                for (int i = 0; i < 5; i++) {
                    Vec3 p = from.add(step.scale(i));
                    level.sendParticles(ParticleTypes.CRIMSON_SPORE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    /** Tap: pour into an Essence Jug placed in front of it. */
    private void tapIntoJug(Level level) {
        BlockPos front = worldPosition.relative(getBlockState().getValue(GraftBlock.FACING));
        if (!(level.getBlockEntity(front) instanceof EssenceJugBlockEntity jug)) {
            return;
        }
        OrganismCoreBlockEntity core = core();
        if (core == null || core.stored() <= 0) {
            return;
        }
        int space = jug.space(core.aspect());
        int moved = core.extract(Math.min(TAP_RATE, space));
        if (moved > 0) {
            jug.insert(core.aspect(), moved);
        }
    }

    public ItemInteractionResult useItemOn(ItemStack stack, Player player, InteractionHand hand) {
        if (level == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (type() == GraftType.TAP && EssenceItems.isContainer(stack)) {
            if (level.isClientSide()) {
                return ItemInteractionResult.SUCCESS;
            }
            OrganismCoreBlockEntity core = core();
            if (core == null) {
                OrganismInteractions.tell(player, Component.translatable("message.chronogenesis.organism.dead"));
                return ItemInteractionResult.FAIL;
            }
            if (!core.aspect().fitsInGlass() && core.aspect() != Aspect.TIME) {
                OrganismInteractions.tell(player, Component.translatable("message.chronogenesis.tap.volatile",
                        core.aspect().displayName()));
                return ItemInteractionResult.FAIL;
            }
            int space = EssenceItems.space(stack, core.aspect());
            int moved = core.extract(space);
            if (moved > 0) {
                EssenceItems.fillHeld(player, hand, core.aspect(), moved);
                level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 0.8f);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        // Anything else: treat the graft as part of the organism (feeding).
        return OrganismInteractions.useItemOnTissue(stack, level, GraftBlock.supportPos(getBlockState(), worldPosition),
                player, hand);
    }

    public InteractionResult useWithoutItem(Player player) {
        if (level == null) {
            return InteractionResult.PASS;
        }
        if (type() == GraftType.ENHANCING_GLAND) {
            if (!level.isClientSide()) {
                OrganismCoreBlockEntity core = core();
                if (core == null) {
                    OrganismInteractions.tell(player, Component.translatable("message.chronogenesis.organism.dead"));
                } else {
                    Powers.infuseFromGland(player, core, worldPosition);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return OrganismInteractions.inspect(level, GraftBlock.supportPos(getBlockState(), worldPosition), player);
    }
}
