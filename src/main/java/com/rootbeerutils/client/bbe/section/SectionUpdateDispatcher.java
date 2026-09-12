/*
 * Derived from BetterBlockEntities (LGPL-3.0). See BBE.java for details.
 */
package com.rootbeerutils.client.bbe.section;

import com.rootbeerutils.client.bbe.task.TaskScheduler;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* sodium */
import com.rootbeerutils.client.bbe.sodium.render.SodiumWorldRenderer;

public final class SectionUpdateDispatcher {

    private SectionUpdateDispatcher() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("BBE-SectionUpdateDispatcher");

    public static void queueRebuildAtBlockPos(BlockPos pos) {
        try {
            TaskScheduler.schedule(() -> {
                if (Minecraft.getInstance().level == null) return;

                SodiumWorldRenderer sodiumWorldRenderer = SodiumWorldRenderer.instanceNullable();
                if (sodiumWorldRenderer != null) {
                    sodiumWorldRenderer.scheduleRebuildForBlockArea(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX(), pos.getY(), pos.getZ(),
                            false
                    );
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to rebuild terrain section!", e);
            SectionRebuildCallbacks.remove(pos);
        }
    }

    /**
     * Rebuild a section with a fence callback (runs after section rebuild is complete).
     */
    public static void queueRebuildAtBlockPos(BlockPos pos, Runnable onUploadedFence) {
        SectionRebuildCallbacks.await(pos, onUploadedFence);
        queueRebuildAtBlockPos(pos);
    }

    public static void queueUpdateAllSections() {
        try {
            SodiumWorldRenderer sodiumWorldRenderer = SodiumWorldRenderer.instanceNullable();
            if (sodiumWorldRenderer != null) {
                sodiumWorldRenderer.scheduleTerrainUpdate();
            }
        } catch (Exception e) {
            LOGGER.error("Reloading terrain sections failed!", e);
        }
    }
}
