package com.rootbeerutils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class CapturedFrame {

    public static final Identifier CAPTURED_FRAME_ID =
        Identifier.fromNamespaceAndPath("rootbeerutils", "captured_frame");

    public static boolean initialJoin = true;

    private CapturedFrame() {
    }

    public static void clearCapturedTexture() {
        Minecraft.getInstance().getTextureManager().release(CAPTURED_FRAME_ID);
    }

    public static void captureLastFrame() {
        final Minecraft client = Minecraft.getInstance();
        Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), nativeImage ->
            client.getTextureManager().register(
                CAPTURED_FRAME_ID,
                new DynamicTexture(CAPTURED_FRAME_ID::toString, nativeImage)
            )
        );
    }
}
