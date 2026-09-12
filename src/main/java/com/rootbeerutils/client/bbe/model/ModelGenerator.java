/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 */
package com.rootbeerutils.client.bbe.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.rootbeerutils.client.bbe.BBE;
import com.rootbeerutils.client.bbe.config.ConfigCache;
import com.rootbeerutils.client.bbe.task.ResourceTasks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;

public final class ModelGenerator {

    private ModelGenerator() {
    }

    public static int generateAppend() {
        PoseStack stack = new PoseStack();

        EntityModelSet entityModelSet = tryGetEntityModelSet();
        if (entityModelSet == null) {
            return ResourceTasks.FAILED;
        }

        for (ModelLayerLocation layer : GeometryRegistry.SupportedVanillaModelLayers.ALL) {
            try {
                bakeLayerSetupAndAppend(entityModelSet, layer, stack);
            } catch (Exception e) {
                BBE.getLogger().error("Geometry setup for ModelLayer {} failed", layer.layer(), e);
            }
        }

        return ResourceTasks.COMPLETE;
    }

    public static void bakeLayerSetupAndAppend(EntityModelSet entityModelSet, ModelLayerLocation layer, PoseStack stack) {
        ModelPart root = entityModelSet.bakeLayer(layer);
        if (root.getAllParts().isEmpty()) {
            BBE.getLogger().error("Root ModelPart for ModelLayer {} is empty after bake; skipping", layer.layer());
            return;
        }

        stack.setIdentity();

        if (layer == ModelLayers.SHULKER_BOX) {
            setupShulker(layer, root, stack);
        } else if (layer == ModelLayers.DOUBLE_CHEST_RIGHT || layer == ModelLayers.DOUBLE_CHEST_LEFT || layer == ModelLayers.CHEST) {
            setupChest(layer, root, stack);
        } else if (layer == ModelLayers.BELL) {
            setupBell(layer, root, stack);
        } else if (layer == ModelLayers.DECORATED_POT_BASE || layer == ModelLayers.DECORATED_POT_SIDES) {
            setupDecoratedPot(layer, root, stack);
        } else if (layer == ModelLayers.STANDING_BANNER ||
                layer == ModelLayers.WALL_BANNER ||
                layer == ModelLayers.STANDING_BANNER_FLAG ||
                layer == ModelLayers.WALL_BANNER_FLAG) {
            setupBanners(layer, root, stack);
        } else if (layer == ModelLayers.COPPER_GOLEM      ||
                layer == ModelLayers.COPPER_GOLEM_RUNNING ||
                layer == ModelLayers.COPPER_GOLEM_SITTING ||
                layer == ModelLayers.COPPER_GOLEM_STAR) {
            setupCopperGolemStatue(layer, root, stack);
        }
    }

    private static void setupShulker(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        stack.pushPose();
        stack.translate(0.5F, -0.5F, 0.5F);
        stack.mulPose(Axis.YP.rotationDegrees(180.0F));
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.SHULKER, stack);
        stack.popPose();
    }

    private static void setupChest(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        stack.pushPose();
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.CHEST, stack);
        stack.popPose();
    }

    private static void setupBell(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        stack.pushPose();
        stack.translate(0.5F, 0.0F, 0.5F);
        stack.mulPose(Axis.YP.rotationDegrees(90.0F));
        stack.translate(-0.5F, 0.0F, -0.5F);
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.BELL_BODY, stack);
        stack.popPose();
    }

    private static void setupDecoratedPot(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        stack.pushPose();
        stack.translate(0.5F, 0.0F, 0.5F);
        stack.mulPose(Axis.YP.rotationDegrees(180.0F));
        stack.translate(-0.5F, 0.0F, -0.5F);
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_BASE, stack);
        stack.popPose();
    }

    private static void setupBanners(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        stack.pushPose();
        stack.translate(0.5F, 0.0F, 0.5F);
        stack.scale(0.6666667F, -0.6666667F, -0.6666667F);

        if (layer == ModelLayers.WALL_BANNER_FLAG || layer == ModelLayers.STANDING_BANNER_FLAG) {
            ModelPart flag = root.getChild("flag");
            float step = -0.45f;
            float rot = step * ConfigCache.bannerPose;
            float rotClamped = Math.clamp(rot, -4.05f, -0.45f);
            flag.xRot = (float) Math.toRadians(rotClamped);
        }

        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.BANNER, stack);
        stack.popPose();
    }

    private static void setupCopperGolemStatue(ModelLayerLocation layer, ModelPart root, PoseStack stack) {
        stack.pushPose();
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.XP.rotationDegrees(180));
        stack.translate(-0.5f, -0.5f, -0.5f);
        stack.translate(0.5F, 1.0F, 0.5F);
        GeometryRegistry.cacheGeometry(layer, root, GeometryRegistry.PlaceHolderSpriteIdentifiers.COPPER_GOLEM_STATUE, stack);
        stack.popPose();
    }

    private static EntityModelSet tryGetEntityModelSet() {
        try {
            return Minecraft.getInstance().getEntityModels();
        } catch (Exception e) {
            BBE.getLogger().error("Failed to get EntityModelSet", e);
            return null;
        }
    }
}
