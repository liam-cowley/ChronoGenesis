package com.chronogenesis.client;

import org.jetbrains.annotations.Nullable;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.entity.DinoCargo;
import com.chronogenesis.entity.Titanosaur;
import com.chronogenesis.network.CargoActionPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Reaching into a Titanosaur's cargo ({@link DinoCargo}): the block under the cursor is found in the cargo's own frame
 * (turned with the beast, off the world's grid) and, if it is nearer than anything in the world, it is the target:
 * outlined, used with right-click, broken by holding left-click (cracking as it goes). The server does the rest.
 */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class CargoControls {
    /** A block of a cargo under the cursor: whose, where in the cargo, which face, and the point (cargo frame). */
    public record Target(Titanosaur beast, BlockPos offset, BlockState state, Direction face, Vec3 hit) {}

    @Nullable
    private static Target target;
    @Nullable
    private static Target breaking;
    private static float progress;
    private static int delay;
    private static int swingTicks;

    private CargoControls() {}

    /** The cargo block under the cursor, nearer than the world's, or null. */
    @Nullable
    public static Target pick(Minecraft mc, float partialTick) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator()) {
            return null;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        double reach = player.blockInteractionRange();
        Vec3 end = eye.add(player.getViewVector(partialTick).scale(reach));
        Target best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Titanosaur beast : mc.level.getEntitiesOfClass(Titanosaur.class, player.getBoundingBox().inflate(reach + DinoCargo.REACH + 12),
                beast -> beast.cargo().carrying())) {
            DinoCargo cargo = beast.cargo();
            Vec3 at = beast.getPosition(partialTick);
            float yaw = Mth.rotLerp(partialTick, beast.yRotO, beast.getYRot());
            Vec3 from = cargo.toLocal(eye, at, yaw);
            Vec3 to = cargo.toLocal(end, at, yaw);
            for (DinoCargo.Carried block : cargo.carried()) {
                VoxelShape shape = block.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
                if (shape.isEmpty()) {
                    continue;
                }
                BlockHitResult hit = shape.clip(from, to, block.offset());
                if (hit != null) {
                    double distance = hit.getLocation().distanceToSqr(from);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new Target(beast, block.offset(), block.state(), hit.getDirection(), hit.getLocation());
                    }
                }
            }
        }
        if (best == null) {
            return null;
        }
        HitResult world = mc.hitResult;
        // The beasts themselves never stand in front of their own cargo.
        boolean beastHit = world instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Titanosaur;
        if (world != null && !beastHit && world.getType() != HitResult.Type.MISS && world.getLocation().distanceToSqr(eye) < bestDistance - 1.0E-4) {
            return null;
        }
        return best;
    }

    /** Before the keys are handled: a cargo block in front of the world's takes the cursor (the world's is missed). */
    @SubscribeEvent
    static void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        target = mc.screen == null ? pick(mc, 1.0f) : null;
        if (target != null && mc.hitResult != null) {
            mc.hitResult = BlockHitResult.miss(mc.hitResult.getLocation(), Direction.UP, BlockPos.containing(mc.hitResult.getLocation()));
        }
        if (delay > 0) {
            delay--;
        }
        if (breaking == null) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || target == null || !mc.options.keyAttack.isDown() || target.beast() != breaking.beast()
                || !target.offset().equals(breaking.offset()) || target.state() != breaking.state()) {
            breaking = null;
            progress = 0;
            return;
        }
        progress += breaking.state().getDestroyProgress(player, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        player.swing(InteractionHand.MAIN_HAND);
        if (++swingTicks % 4 == 0) {
            SoundType sound = breaking.state().getSoundType();
            Vec3 at = breaking.beast().cargo().toWorld(Vec3.atCenterOf(breaking.offset()), breaking.beast().position(), breaking.beast().getYRot());
            mc.level.playLocalSound(at.x, at.y, at.z, sound.getHitSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 8.0f,
                    sound.getPitch() * 0.5f, false);
        }
        if (progress >= 1.0f) {
            send(breaking, true);
            breaking = null;
            progress = 0;
            delay = 5;
        }
    }

    @SubscribeEvent
    static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (target == null || mc.player == null) {
            return;
        }
        event.setCanceled(true);
        if (event.isUseItem()) {
            event.setSwingHand(false);
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                send(target, false);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else if (event.isAttack()) {
            event.setSwingHand(true);
            if (mc.player.getAbilities().instabuild) {
                if (delay == 0) {
                    send(target, true);
                    delay = 5;
                }
            } else if (breaking == null && delay == 0) {
                breaking = target;
                progress = 0;
            }
        }
    }

    private static void send(Target at, boolean breaking) {
        PacketDistributor.sendToServer(new CargoActionPayload(at.beast().getId(), breaking, at.offset(), at.face(), at.hit()));
    }

    /** How far the block being broken has cracked (0-9), or -1 if this block of this beast is not being broken. */
    public static int cracks(Titanosaur beast, BlockPos offset) {
        return breaking != null && breaking.beast() == beast && breaking.offset().equals(offset)
                ? Mth.clamp((int) (progress * 10.0f), 0, 9) : -1;
    }

    // ---------------------------------------------------------------- the outline

    @SubscribeEvent
    static void onHighlight(RenderHighlightEvent.Block event) {
        if (target != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Target now = mc.screen == null && !mc.options.hideGui ? pick(mc, partial) : null;
        if (now == null) {
            return;
        }
        Titanosaur beast = now.beast();
        Vec3 at = beast.getPosition(partial);
        float yaw = Mth.rotLerp(partial, beast.yRotO, beast.getYRot());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
        pose.mulPose(Axis.YP.rotationDegrees(-beast.cargo().turn(yaw)));
        Vec3 corner = beast.cargo().anchor().add(Vec3.atLowerCornerOf(now.offset()));
        pose.translate(corner.x, corner.y, corner.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack.Pose last = pose.last();
        now.state().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()).forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= length;
            dy /= length;
            dz /= length;
            lines.addVertex(last, (float) x1, (float) y1, (float) z1).setColor(0, 0, 0, 0.4f).setNormal(last, dx, dy, dz);
            lines.addVertex(last, (float) x2, (float) y2, (float) z2).setColor(0, 0, 0, 0.4f).setNormal(last, dx, dy, dz);
        });
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }
}
