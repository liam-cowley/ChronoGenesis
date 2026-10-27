package com.chronogenesis.client;

import org.lwjgl.glfw.GLFW;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.CultureItem;
import com.chronogenesis.network.PowerKeyPayload;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only wiring: key bindings, the essence HUD, and aspect colour tints. */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final String CATEGORY = "key.categories.chronogenesis";

    public static final KeyMapping CYCLE_POWER = new KeyMapping("key.chronogenesis.cycle_power",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping USE_POWER = new KeyMapping("key.chronogenesis.use_power",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    private ClientEvents() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CYCLE_POWER);
        event.register(USE_POWER);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ChronoGenesis.id("essence_hud"), EssenceHud::render);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        while (CYCLE_POWER.consumeClick()) {
            PacketDistributor.sendToServer(new PowerKeyPayload(PowerKeyPayload.Action.CYCLE_SELECTED));
        }
        while (USE_POWER.consumeClick()) {
            PacketDistributor.sendToServer(new PowerKeyPayload(PowerKeyPayload.Action.USE_SELECTED));
        }
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> fleshColor(state),
                ModBlocks.CHRONOFLESH.get(), ModBlocks.ORGANISM_CORE.get());
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Layer 1 of containers is the liquid inside, tinted by aspect.
        event.register((stack, tintIndex) -> tintIndex == 1 ? contentsColor(stack) : -1,
                ModItems.ESSENCE_VIAL.get(), ModItems.CHRONO_SYRINGE.get(), ModItems.STASIS_AMPOULE.get(),
                ModItems.ESSENCE_JUG.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? opaque(CultureItem.aspect(stack).color()) : -1,
                ModItems.CULTURE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? opaque(0xA0546E) : -1, ModItems.CHRONOFLESH.get());
    }

    private static int fleshColor(BlockState state) {
        if (!state.hasProperty(FleshBlock.ASPECT)) {
            return -1;
        }
        // Mix the aspect colour into a bruised flesh base so every organism still reads as flesh.
        int aspect = state.getValue(FleshBlock.ASPECT).color();
        int base = 0xA0546E;
        int r = (FastColor.ARGB32.red(base) + FastColor.ARGB32.red(aspect)) / 2;
        int g = (FastColor.ARGB32.green(base) + FastColor.ARGB32.green(aspect)) / 2;
        int b = (FastColor.ARGB32.blue(base) + FastColor.ARGB32.blue(aspect)) / 2;
        return FastColor.ARGB32.color(255, r, g, b);
    }

    private static int contentsColor(ItemStack stack) {
        EssenceContents contents = EssenceItems.get(stack);
        if (contents == null && stack.getItem() instanceof ChronoSyringeItem) {
            return 0x00FFFFFF; // fully transparent tint for an empty barrel
        }
        return contents == null ? 0x00FFFFFF : opaque(contents.aspect().color());
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }
}
