package com.chronogenesis.client;

import org.lwjgl.glfw.GLFW;

import com.chronogenesis.ChronoGenesis;
import com.chronogenesis.essence.EssenceContents;
import com.chronogenesis.essence.EssenceItems;
import com.chronogenesis.item.ChronoSyringeItem;
import com.chronogenesis.item.CultureItem;
import com.chronogenesis.network.PowerActionPayload;
import com.chronogenesis.organism.EssenceJugBlockEntity;
import com.chronogenesis.organism.FleshBlock;
import com.chronogenesis.player.Powers;
import com.chronogenesis.registry.ModBlocks;
import com.chronogenesis.registry.ModEntities;
import com.chronogenesis.registry.ModItems;
import com.chronogenesis.registry.ModParticles;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only wiring: key bindings, the essence HUD, and aspect colour tints. */
@EventBusSubscriber(modid = ChronoGenesis.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final String CATEGORY = "key.categories.chronogenesis";

    /** Tap: use the selected active power. With Shift held: the Essence Manipulator's wheel. */
    public static final KeyMapping POWERS = new KeyMapping("key.chronogenesis.powers",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private ClientEvents() {}

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(POWERS);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRECURSOR.get(), PrecursorRenderer::new);
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ESSENCE_MOTE.get(), EssenceMoteParticle.Provider::new);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ChronoGenesis.id("essence_hud"), EssenceHud::render);
        event.registerAboveAll(ChronoGenesis.id("shift_hud"), ShiftHud::render);
        event.registerBelowAll(ChronoGenesis.id("temporal_lens"), LensView::composite);
        // Below the lens: chunks with their own time are part of the world the lens looks out from.
        event.registerBelowAll(ChronoGenesis.id("time_zones"), TimeZoneView::composite);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ShiftHud.tick();
        while (POWERS.consumeClick()) {
            if (minecraft.screen != null) {
                continue;
            }
            if (minecraft.options.keyShift.isDown()) {
                // Shift + R: the wheel, held open while Shift is down. It needs the Manipulator.
                if (Powers.hasManipulator(minecraft.player)) {
                    minecraft.setScreen(new PowerWheelScreen());
                } else {
                    minecraft.player.displayClientMessage(Component.translatable("message.chronogenesis.power.need_manipulator")
                            .withStyle(ChatFormatting.GRAY), true);
                }
            } else {
                PacketDistributor.sendToServer(PowerActionPayload.use());
            }
        }
    }

    /** Shift + scroll with the Essence Manipulator in hand: the selected power's strength, not the hotbar. */
    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || event.getScrollDeltaY() == 0
                || !minecraft.options.keyShift.isDown()) {
            return;
        }
        if (minecraft.player.getMainHandItem().is(ModItems.ESSENCE_MANIPULATOR.get())
                || minecraft.player.getOffhandItem().is(ModItems.ESSENCE_MANIPULATOR.get())) {
            PacketDistributor.sendToServer(PowerActionPayload.nudge(event.getScrollDeltaY() > 0 ? 1 : -1));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> fleshColor(state),
                ModBlocks.CHRONOFLESH.get(), ModBlocks.ORGANISM_CORE.get());
        // The liquid behind the jug's glass takes the colour of what is inside.
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex == 0 && level != null && pos != null
                    && level.getBlockEntity(pos) instanceof EssenceJugBlockEntity jug && jug.contents() != null) {
                return opaque(jug.contents().aspect().color());
            }
            return -1;
        }, ModBlocks.ESSENCE_JUG.get());
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
