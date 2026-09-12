package com.rootbeerutils.client.quickpack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public class QuickPackClient implements ClientModInitializer {

    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = new KeyMapping("key.rootbeerutils.quickpack_toggle",
                                   GLFW.GLFW_KEY_UNKNOWN, // unbound by default (-1)
                                   KeyMapping.Category.MISC);
        KeyMappingHelper.registerKeyMapping(toggleKey);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                boolean newState = !QuickPackConfig.isEnabled();
                QuickPackConfig.setEnabled(newState);
                MutableComponent message = Component.translatable(newState
                        ? "rootbeerutils.quickpack.enabled"
                        : "rootbeerutils.quickpack.disabled")
                    .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);
                announce(client, message);
            }
        });
    }

    private static void announce(Minecraft client, Component message) {
        client.gui.hud.setOverlayMessage(message, false);
    }
}
