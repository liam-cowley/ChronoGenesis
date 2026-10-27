package com.chronogenesis.biomech;

import com.chronogenesis.Config;
import com.chronogenesis.essence.Aspect;
import com.chronogenesis.essence.EssenceDrain;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Leech Tendril graft: drains its organism's aspect from creatures nearby, one dram at a time. This is how
 * farms work. Players are never touched.
 */
public class LeechTendrilBlock extends GraftBlock {
    public static final MapCodec<LeechTendrilBlock> CODEC = simpleCodec(LeechTendrilBlock::new);
    private static final int INTERVAL = 40;

    public LeechTendrilBlock(Properties properties) {
        super(properties, 6, 0, 6, 10, 14, 10);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, INTERVAL);
        OrganismCoreBlockEntity core = core(level, state, pos);
        if (core == null) {
            return;
        }
        Aspect aspect = core.aspect();
        if (core.insert(aspect, 1, true) <= 0) {
            return;
        }
        AABB range = new AABB(pos).inflate(Config.LEECH_RANGE.get());
        LivingEntity victim = level.getEntitiesOfClass(LivingEntity.class, range,
                        entity -> !(entity instanceof Player) && entity.isAlive() && EssenceDrain.remaining(entity, aspect) > 0)
                .stream().findFirst().orElse(null);
        if (victim == null) {
            return;
        }
        int taken = EssenceDrain.drain(victim, aspect, 1);
        if (taken > 0) {
            core.insert(aspect, taken, false);
            trail(level, pos, victim, aspect);
            level.playSound(null, pos, SoundEvents.HONEY_DRINK, SoundSource.BLOCKS, 0.5f, 0.6f);
        }
    }

    /** A line of essence-coloured motes from the victim to the tendril. */
    private static void trail(ServerLevel level, BlockPos pos, LivingEntity victim, Aspect aspect) {
        Vec3 from = victim.position().add(0, victim.getBbHeight() / 2, 0);
        Vec3 to = Vec3.atCenterOf(pos);
        int steps = (int) Math.ceil(from.distanceTo(to) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, (double) i / steps);
            level.sendParticles(EssenceDrain.dust(aspect), point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }
}
