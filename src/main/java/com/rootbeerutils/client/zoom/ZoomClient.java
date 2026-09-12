package com.rootbeerutils.client.zoom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ZoomClient implements ClientModInitializer {

    public static final float ZOOM_HOLD_FOV = 25.5f;

    public static KeyMapping zoomKey;

    /**
     * True if Zoom was active on the previous frame; used for press- / release-edge detection.
     */
    private static boolean zoomedLastFrame = false;

    /**
     * Snapshot of {@link net.minecraft.client.Options#smoothCamera} taken at the press edge,
     * restored on the release edge so we don't permanently overwrite the user's setting.
     */
    private static boolean savedSmoothCamera = false;

    @Override
    public void onInitializeClient() {
        zoomKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.rootbeerutils.zoom", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));
    }

    public static boolean isZoomActive() {
        Minecraft client = Minecraft.getInstance();
        if (zoomKey == null) {
            return false;
        }

        return zoomKey.isDown() && client.level != null && client.player != null;
    }

    /**
     * Per-frame zoom-state sync. Called from {@code ZoomFrameSyncMixin} at HEAD of
     * {@code Minecraft.runTick}, so this runs before mouse-handler input is processed and the
     * smooth-camera flag is in the correct state by the time vanilla reads it. Three cases:
     *
     * <ul>
     *   <li><b>Press edge</b> (active and not previously active): snapshot the user's
     *       smooth-camera setting, then force it on.</li>
     *   <li><b>Release edge</b> (no longer active and was previously active): restore the
     *       snapshotted value.</li>
     *   <li><b>Steady hold</b> (active and was previously active): keep the flag pinned on,
     *       in case some other code path flipped it back off mid-zoom.</li>
     * </ul>
     */
    public static void onFrameStart() {
        if (zoomKey == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        boolean active = isZoomActive();

        if (active && !zoomedLastFrame) {
            savedSmoothCamera = client.options.smoothCamera;
            client.options.smoothCamera = true;
        } else if (!active && zoomedLastFrame) {
            client.options.smoothCamera = savedSmoothCamera;
        } else if (active) {
            client.options.smoothCamera = true;
        }

        zoomedLastFrame = active;
    }
}
