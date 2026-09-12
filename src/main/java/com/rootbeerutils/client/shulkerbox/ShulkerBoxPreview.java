package com.rootbeerutils.client.shulkerbox;

import com.mojang.blaze3d.platform.InputConstants;
import com.rootbeerutils.client.mixin.KeyMappingAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ShulkerBoxPreview implements ClientModInitializer {

    private static KeyMapping lockKey;
    private static KeyMapping expandKey;
    public static boolean echestWasOpened = false;
    public static List<ItemStack> enderChestItems = new ArrayList<>();
    public static Minecraft mc = Minecraft.getInstance();

    @Override
    public void onInitializeClient() {
        lockKey = new KeyMapping("key.rootbeerutils.shulker_lock_tooltip",
                GLFW.GLFW_KEY_LEFT_CONTROL,
                KeyMapping.Category.MISC);
        KeyMappingHelper.registerKeyMapping(lockKey);

        expandKey = new KeyMapping("key.rootbeerutils.ender_expand_tooltip",
                GLFW.GLFW_KEY_LEFT_ALT,
                KeyMapping.Category.MISC);
        KeyMappingHelper.registerKeyMapping(expandKey);

        ClientTooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof ShulkerBoxPreviewTooltipComponent component) {
                return new ShulkerBoxPreviewClientTooltipComponent(component);
            }

            return null;
        });
    }

    /**
     * Reads the GLFW raw key state instead of {@link KeyMapping#isDown()} so the lock works
     * inside screens (where vanilla suppresses isDown for non-text keys).
     */
    public static boolean isLockKeyPressed() {
        if (lockKey == null) {
            return false;
        }

        InputConstants.Key key = ((KeyMappingAccessor) lockKey).rbutils$getBoundKey();
        if (key.getType() != InputConstants.Type.KEYSYM
                || key.getValue() == InputConstants.UNKNOWN.getValue()) {
            return false;
        }

        long handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(handle, key.getValue()) == GLFW.GLFW_PRESS;
    }

    public static boolean isExpandKeyPressed() {
        if (expandKey == null) {
            return false;
        }

        InputConstants.Key key = ((KeyMappingAccessor) expandKey).rbutils$getBoundKey();
        if (key.getType() != InputConstants.Type.KEYSYM
                || key.getValue() == InputConstants.UNKNOWN.getValue()) {
            return false;
        }

        long handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(handle, key.getValue()) == GLFW.GLFW_PRESS;
    }
}


