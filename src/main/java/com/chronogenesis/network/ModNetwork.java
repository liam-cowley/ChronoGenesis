package com.chronogenesis.network;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.player.Powers;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ChronoGenesis.MODID)
public final class ModNetwork {
    private ModNetwork() {}

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(SyncPlayerEssencePayload.TYPE, SyncPlayerEssencePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        context.player().setData(com.chronogenesis.registry.ModAttachments.PLAYER_ESSENCE, payload.essence())));

        registrar.playToServer(PowerKeyPayload.TYPE, PowerKeyPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        switch (payload.action()) {
                            case CYCLE_SELECTED -> Powers.cycleSelected(player);
                            case USE_SELECTED -> Powers.use(player);
                        }
                    }
                }));

        registrar.playToServer(SelectPowerPayload.TYPE, SelectPowerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        Powers.select(player, payload.aspect());
                    }
                }));
    }
}
