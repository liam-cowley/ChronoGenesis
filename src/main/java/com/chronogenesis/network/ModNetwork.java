package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.biocomputer.ChronocortexBlockEntity;
import com.chronogenesis.biocomputer.Research;
import com.chronogenesis.client.ClientPayloads;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModAttachments;
import com.chronogenesis.time.TimeZones;
import com.chronogenesis.timetravel.TemporalShift;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ModNetwork {
    private static final int RESEARCH_REACH = 8;

    private ModNetwork() {}

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(SyncPlayerEssencePayload.TYPE, SyncPlayerEssencePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        context.player().setData(ModAttachments.PLAYER_ESSENCE, payload.essence())));

        registrar.playToServer(PowerActionPayload.TYPE, PowerActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        switch (payload.action()) {
                            case USE -> Powers.use(player);
                            case SELECT -> Powers.select(player, payload.power());
                            case TOGGLE -> Powers.toggle(player, payload.power());
                            case STRENGTH -> Powers.setStrength(player, payload.power(), payload.value());
                            case NUDGE -> Powers.nudgeStrength(player, Integer.signum(payload.value()));
                        }
                    }
                }));

        registrar.playToClient(SyncResearchPayload.TYPE, SyncResearchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        context.player().setData(ModAttachments.PLAYER_RESEARCH, payload.research())));

        registrar.playToClient(OpenChronocortexPayload.TYPE, OpenChronocortexPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.openChronocortex(payload)));

        registrar.playToServer(ResearchPayload.TYPE, ResearchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // Only at the Chronocortex itself, within reach.
                    if (context.player() instanceof ServerPlayer player
                            && player.blockPosition().distSqr(payload.pos()) <= RESEARCH_REACH * RESEARCH_REACH
                            && player.level().getBlockEntity(payload.pos()) instanceof ChronocortexBlockEntity cortex) {
                        Research.perform(player, cortex, payload.topic());
                        PacketDistributor.sendToPlayer(player, OpenChronocortexPayload.of(cortex));
                    }
                }));

        registrar.playToClient(PastSectionsPayload.TYPE, PastSectionsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.pastSections(payload)));

        registrar.playToServer(ShiftPayload.TYPE, ShiftPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.blockPosition().distSqr(payload.pos()) <= TemporalShift.REACH * TemporalShift.REACH
                            && player.level().getBlockEntity(payload.pos()) instanceof ChronocortexBlockEntity cortex) {
                        TemporalShift.begin(player, cortex, payload.era());
                    }
                }));

        registrar.playToClient(ShiftStatusPayload.TYPE, ShiftStatusPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPayloads.shiftStatus(payload)));

        registrar.playToClient(TimeZonesPayload.TYPE, TimeZonesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> TimeZones.clientReceive(payload)));
    }
}
