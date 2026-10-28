package com.chronogenesis.client;

import com.chronogenesis.network.OpenChronocortexPayload;
import com.chronogenesis.network.PastSectionsPayload;
import com.chronogenesis.network.ShiftStatusPayload;

import net.minecraft.client.Minecraft;

/** Client-side handling of server payloads that open or refresh screens. Only called on the client. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void pastSections(PastSectionsPayload payload) {
        PastSections.receive(payload);
    }

    public static void shiftStatus(ShiftStatusPayload payload) {
        ShiftHud.receive(payload);
    }

    public static void openChronocortex(OpenChronocortexPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ChronocortexScreen open) {
            open.update(payload);
        } else {
            minecraft.setScreen(new ChronocortexScreen(payload));
        }
    }
}
