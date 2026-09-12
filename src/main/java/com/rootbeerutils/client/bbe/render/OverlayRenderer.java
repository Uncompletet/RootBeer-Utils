/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 */
package com.rootbeerutils.client.bbe.render;

import com.mojang.blaze3d.vertex.PoseStack;

import com.rootbeerutils.client.bbe.config.ConfigCache;
import com.rootbeerutils.client.bbe.ext.BlockEntityExt;
import com.rootbeerutils.client.bbe.ext.RenderingMode;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class OverlayRenderer {

    private OverlayRenderer() {
    }

        public static <S> boolean manageCrumblingOverlay(BlockEntity blockEntity, SubmitNodeCollector submitNodeCollector, PoseStack poseStack, Model<? super S> model,
                                                    S state, int light, int overlayCoords, int tint,
                                                    ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        if (crumblingOverlay == null) {
            return false;
        }

        BlockEntityExt blockEntityExt = (BlockEntityExt) blockEntity;

        boolean substitutionActive =
                ConfigCache.masterOptimize
                        && ConfigCache.ENABLED[blockEntityExt.rootbeer_utils$optKind() & 0xFF];

        if (substitutionActive
                && blockEntityExt.rootbeer_utils$renderingMode() == RenderingMode.TERRAIN
                && blockEntityExt.rootbeer_utils$terrainMeshReady()) {
            submitCrumblingOverlay(submitNodeCollector, poseStack, model, state, light, overlayCoords, tint, crumblingOverlay);
            return true;
        }

        return false;
    }

    public static <S> void submitCrumblingOverlay(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, Model<? super S> model,
                                                  S state, int light, int overlayCoords, int tint,
                                                  ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        submitNodeCollector.submitModel(
                model,
                state,
                poseStack,
                ModelBakery.DESTROY_TYPES.get(crumblingOverlay.progress()),
                light,
                overlayCoords,
                tint,
                crumblingOverlay
        );
    }

    /**
     * True if any destruction-progress entry exists for the given block-pos packed long.
     */
    public static boolean isBreaking(long posLong, Long2ObjectMap<?> progression) {
        return !progression.isEmpty() && progression.get(posLong) != null;
    }

}
