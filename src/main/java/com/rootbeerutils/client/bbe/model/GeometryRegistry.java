/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 */
package com.rootbeerutils.client.bbe.model;

import com.mojang.blaze3d.vertex.PoseStack;

import com.rootbeerutils.client.bbe.util.QuadTransform;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GeometryRegistry {

    private GeometryRegistry() {
    }

    private static final ConcurrentHashMap<ModelLayerLocation, BlockStateModel> CACHE = new ConcurrentHashMap<>();

    public static void cacheGeometry(ModelLayerLocation key, ModelPart root, Identifier texture, PoseStack stack) {
        MultiPartBlockModel model = new MultiPartBlockModel(root, QuadTransform.getSprite(texture), stack);
        CACHE.put(key, model);
    }

    public static BlockStateModel getModel(ModelLayerLocation layer) {
        return CACHE.get(layer);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static Map<ModelLayerLocation, BlockStateModel> getCache() {
        return CACHE;
    }

    /**
     * Vanilla model-layer locations BBE bakes geometry from.
     */
    public static final class SupportedVanillaModelLayers {
        public static final ModelLayerLocation CHEST = ModelLayers.CHEST;
        public static final ModelLayerLocation LEFT_CHEST = ModelLayers.DOUBLE_CHEST_LEFT;
        public static final ModelLayerLocation RIGHT_CHEST = ModelLayers.DOUBLE_CHEST_RIGHT;
        public static final ModelLayerLocation BELL_BODY = ModelLayers.BELL;
        public static final ModelLayerLocation DECORATED_POT_BASE = ModelLayers.DECORATED_POT_BASE;
        public static final ModelLayerLocation DECORATED_POT_SIDES = ModelLayers.DECORATED_POT_SIDES;
        public static final ModelLayerLocation SHULKER = ModelLayers.SHULKER_BOX;
        public static final ModelLayerLocation STANDING_BANNER = ModelLayers.STANDING_BANNER;
        public static final ModelLayerLocation WALL_BANNER = ModelLayers.WALL_BANNER;
        public static final ModelLayerLocation STANDING_BANNER_FLAG = ModelLayers.STANDING_BANNER_FLAG;
        public static final ModelLayerLocation WALL_BANNER_FLAG = ModelLayers.WALL_BANNER_FLAG;
        public static final ModelLayerLocation COPPER_GOLEM = ModelLayers.COPPER_GOLEM;
        public static final ModelLayerLocation COPPER_GOLEM_RUNNING = ModelLayers.COPPER_GOLEM_RUNNING;
        public static final ModelLayerLocation COPPER_GOLEM_SITTING = ModelLayers.COPPER_GOLEM_SITTING;
        public static final ModelLayerLocation COPPER_GOLEM_STAR = ModelLayers.COPPER_GOLEM_STAR;

        public static final ModelLayerLocation[] ALL = new ModelLayerLocation[] {
                CHEST, LEFT_CHEST, RIGHT_CHEST, BELL_BODY, DECORATED_POT_BASE, DECORATED_POT_SIDES,
                SHULKER, STANDING_BANNER, WALL_BANNER, STANDING_BANNER_FLAG, WALL_BANNER_FLAG,
                COPPER_GOLEM, COPPER_GOLEM_RUNNING, COPPER_GOLEM_SITTING, COPPER_GOLEM_STAR
        };
    }

    /**
     * Placeholder sprite IDs used when baking each model layer to UV-correct quads.
     */
    public static final class PlaceHolderSpriteIdentifiers {
        public static final Identifier CHEST = Identifier.withDefaultNamespace("entity/chest/normal");
        public static final Identifier BELL_BODY = Identifier.withDefaultNamespace("entity/bell/bell_body");
        public static final Identifier DECORATED_POT_BASE = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_base");
        public static final Identifier DECORATED_POT_SIDES = Identifier.withDefaultNamespace("entity/decorated_pot/decorated_pot_side");
        public static final Identifier SHULKER = Identifier.withDefaultNamespace("entity/shulker/shulker");
        public static final Identifier BANNER = Identifier.withDefaultNamespace("entity/banner_base");
        public static final Identifier COPPER_GOLEM_STATUE = Identifier.withDefaultNamespace("entity/copper_golem/copper_golem");
    }
}
