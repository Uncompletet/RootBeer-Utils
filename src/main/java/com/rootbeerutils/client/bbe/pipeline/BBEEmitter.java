/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 *
 * FRAPI rewrite of BBE's Sodium chunk-mesh substitution. Called from a mixin into VulkanMod's
 * {@code AbstractBlockRenderContext.emitVanillaBlockQuads} HEAD: we recognize the block, pull
 * matching baked geometry from {@link GeometryRegistry}, and emit it through the FRAPI
 * {@link QuadEmitter} provided by the chunk pipeline. The original method is allowed to continue
 * after we return so any remaining vanilla terrain geometry (the "empty cube" model on chests
 * etc.) still gets emitted normally.
 */
package com.rootbeerutils.client.bbe.pipeline;

import com.rootbeerutils.client.bbe.BBE;
import com.rootbeerutils.client.bbe.api.AltRenderers;
import com.rootbeerutils.client.bbe.config.ConfigCache;
import com.rootbeerutils.client.bbe.config.EnumTypes;
import com.rootbeerutils.client.bbe.ext.BlockEntityExt;
import com.rootbeerutils.client.bbe.ext.RenderingMode;
import com.rootbeerutils.client.bbe.model.GeometryRegistry;
import com.rootbeerutils.client.bbe.model.MaterialSelector;
import com.rootbeerutils.client.bbe.model.MultiPartBlockModel;
import com.rootbeerutils.client.bbe.section.SectionUpdateDispatcher;
import com.rootbeerutils.client.bbe.task.ResourceTasks;
import com.rootbeerutils.client.bbe.task.TaskScheduler;
import com.rootbeerutils.client.bbe.util.QuadTransform;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class BBEEmitter {

    private BBEEmitter() {
    }

    private static final ThreadLocal<ArrayList<BlockStateModelPart>> PARTS_BUF =
            ThreadLocal.withInitial(() -> new ArrayList<>(64));

    private static final float[] ROT_UP    = {180f, 180f};
    private static final float[] ROT_DOWN  = {  0f, 180f};
    private static final float[] ROT_NORTH = { 90f,   0f};
    private static final float[] ROT_SOUTH = { 90f, 180f};
    private static final float[] ROT_WEST  = { 90f, 270f};
    private static final float[] ROT_EAST  = { 90f,  90f};

    /**
     * Entry point called from the chunk-pipeline mixin BEFORE VulkanMod's vanilla emit runs.
     * Non-managed blocks are a no-op (the original method continues normally). Managed blocks have
     * their replacement geometry emitted to the FRAPI emitter, then the original method emits its
     * (typically empty) vanilla model on top.
     */
    public static void emit(QuadEmitter emitter,
                            BlockAndTintGetter level,
                            BlockPos pos,
                            BlockState state,
                            RandomSource random,
                            Predicate<@Nullable Direction> cullTest) {
        if (!ConfigCache.masterOptimize) {
            return;
        }

        Block block = state.getBlock();
        BlockEntity blockEntity;
        BlockRenderHelper helper = new BlockRenderHelper(emitter);

        if (block instanceof ChestBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }

            if (ConfigCache.optimizeChests) {
                emitChest(cullTest, random, state, helper, false, blockEntity);
            }
        } else if (block instanceof EnderChestBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }

            if (ConfigCache.optimizeChests) {
                emitChest(cullTest, random, state, helper, true, blockEntity);
            }
        } else if (block instanceof ShulkerBoxBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }

            if (ConfigCache.optimizeShulker) {
                emitShulker(cullTest, random, state, helper, blockEntity);
            }
        } else if (block instanceof BellBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }

            if (ConfigCache.optimizeBells) {
                emitBell(cullTest, random, state, helper, blockEntity);
            }
        } else if (block instanceof DecoratedPotBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }

            if (ConfigCache.optimizeDecoratedPots) {
                emitDecoratedPot(cullTest, random, state, helper, blockEntity);
            }
        } else if (block instanceof BedBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }
        } else if (block instanceof BannerBlock || block instanceof WallBannerBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }

            if (ConfigCache.optimizeBanners) {
                emitBanner(cullTest, random, state, helper, blockEntity);
            }
        } else if (block instanceof CopperGolemStatueBlock) {
            blockEntity = tryGetBlockEntity(pos, level);
            if (blockEntity == null) {
                return;
            }

            if (ConfigCache.optimizeCopperGolemStatue) {
                emitCopperGolemStatue(cullTest, random, state, helper);
            }
        }
    }

    private static void emitChest(Predicate<@Nullable Direction> cullTest, RandomSource random,
                                  BlockState state, BlockRenderHelper helper, boolean ender, BlockEntity blockEntity) {
        ModelLayerLocation layer;
        if (ender) {
            layer = ModelLayers.CHEST;
        } else if (state.hasProperty(ChestBlock.TYPE)) {
            ChestType t = state.getValue(ChestBlock.TYPE);
            layer = (t == ChestType.LEFT) ? ModelLayers.DOUBLE_CHEST_LEFT
                    : (t == ChestType.RIGHT) ? ModelLayers.DOUBLE_CHEST_RIGHT
                    : ModelLayers.CHEST;
        } else {
            layer = ModelLayers.CHEST;
        }

        Map<String, BlockStateModel> pairs = getPairs(layer);
        if (pairs.isEmpty()) {
            return;
        }

        boolean drawLid = shouldRenderLid(blockEntity);
        boolean addBase = (ConfigCache.updateType == EnumTypes.UpdateSchedulerType.FAST.ordinal())
                || (drawLid && ConfigCache.updateType == EnumTypes.UpdateSchedulerType.SMART.ordinal());

        ArrayList<BlockStateModelPart> merged = partsBuf();
        if (addBase) addParts(merged, pairs.get("bottom"), random);
        if (drawLid) {
            addParts(merged, pairs.get("lid"), random);
            addParts(merged, pairs.get("lock"), random);
        }
        if (merged.isEmpty()) {
            return;
        }

        var chestMat = MaterialSelector.getChestMaterial(blockEntity, ConfigCache.christmasChests);
        ChestType type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        SpriteId material = Sheets.chooseSprite(chestMat, type);

        helper.setSourceSprite(QuadTransform.getSprite(GeometryRegistry.PlaceHolderSpriteIdentifiers.CHEST));
        helper.setMaterial(material);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        helper.emitParts(merged, state, cullTest);
    }

    private static void emitShulker(Predicate<@Nullable Direction> cullTest, RandomSource random,
                                    BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        Map<String, BlockStateModel> pairs = getPairs(ModelLayers.SHULKER_BOX);
        if (pairs.isEmpty()) {
            return;
        }

        boolean drawLid = shouldRenderLid(blockEntity);
        boolean addBase = (ConfigCache.updateType == EnumTypes.UpdateSchedulerType.FAST.ordinal())
                || (drawLid && ConfigCache.updateType == EnumTypes.UpdateSchedulerType.SMART.ordinal());

        ArrayList<BlockStateModelPart> merged = partsBuf();
        if (addBase) addParts(merged, pairs.get("base"), random);
        if (drawLid) addParts(merged, pairs.get("lid"), random);
        if (merged.isEmpty()) {
            return;
        }

        Direction facing = state.hasProperty(ShulkerBoxBlock.FACING) ? state.getValue(ShulkerBoxBlock.FACING) : Direction.UP;
        float[] rotation = switch (facing) {
            case UP    -> ROT_UP;
            case DOWN  -> ROT_DOWN;
            case NORTH -> ROT_NORTH;
            case SOUTH -> ROT_SOUTH;
            case WEST  -> ROT_WEST;
            case EAST  -> ROT_EAST;
        };

        DyeColor color = ((ShulkerBoxBlock) state.getBlock()).getColor();
        SpriteId shulkerMaterial = (color == null) ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxSprite(color);

        helper.setSourceSprite(QuadTransform.getSprite(GeometryRegistry.PlaceHolderSpriteIdentifiers.SHULKER));
        helper.setMaterial(shulkerMaterial);
        helper.setRendertype(ChunkSectionLayer.CUTOUT);
        helper.setRotation(rotation);
        helper.emitParts(merged, state, cullTest);
        helper.setRotation(null);
    }

    private static void emitBell(Predicate<@Nullable Direction> cullTest, RandomSource random,
                                 BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        if (!shouldRenderLid(blockEntity)) {
            return;
        }

        Map<String, BlockStateModel> pairs = getPairs(ModelLayers.BELL);
        if (pairs.isEmpty()) {
            return;
        }

        BlockStateModel bellBody = pairs.get("bell_body");
        if (bellBody == null) {
            return;
        }

        List<BlockStateModelPart> bellBodyParts = new ArrayList<>();
        bellBody.collectParts(random, bellBodyParts);
        if (bellBodyParts.isEmpty()) {
            return;
        }

        SpriteId bellBodyMaterial = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");

        helper.setSourceSprite(QuadTransform.getSprite(GeometryRegistry.PlaceHolderSpriteIdentifiers.BELL_BODY));
        helper.setMaterial(bellBodyMaterial);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        helper.emitParts(bellBodyParts, state, cullTest);
    }

    private static void emitDecoratedPot(Predicate<@Nullable Direction> cullTest, RandomSource random,
                                         BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        if (!shouldRenderLid(blockEntity)) {
            return;
        }

        if (!(blockEntity instanceof DecoratedPotBlockEntity potBE)) {
            return;
        }

        Map<String, BlockStateModel> basePairs = getPairs(ModelLayers.DECORATED_POT_BASE);
        Map<String, BlockStateModel> sidePairs = getPairs(ModelLayers.DECORATED_POT_SIDES);
        if (basePairs.isEmpty() || sidePairs.isEmpty()) {
            return;
        }

        ArrayList<BlockStateModelPart> baseParts = partsBuf();
        addAllParts(baseParts, basePairs.values(), random);
        if (!baseParts.isEmpty()) {
            helper.setSourceSprite(QuadTransform.getSprite(GeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_BASE));
            helper.setMaterial(Sheets.DECORATED_POT_BASE);
            helper.setRendertype(ChunkSectionLayer.SOLID);
            helper.emitParts(baseParts, state, cullTest);
        }

        var decorations = potBE.getDecorations();
        for (Map.Entry<String, BlockStateModel> e : sidePairs.entrySet()) {
            String key = e.getKey();
            BlockStateModel m = e.getValue();
            if (m == null) {
                continue;
            }

            List<BlockStateModelPart> sideParts = new ArrayList<>();
            m.collectParts(random, sideParts);
            if (sideParts.isEmpty()) {
                continue;
            }

            SpriteId sideMaterial = switch (key) {
                case "back"  -> MaterialSelector.getDPSideMaterial(decorations.back());
                case "front" -> MaterialSelector.getDPSideMaterial(decorations.front());
                case "left"  -> MaterialSelector.getDPSideMaterial(decorations.left());
                case "right" -> MaterialSelector.getDPSideMaterial(decorations.right());
                default      -> MaterialSelector.getDPSideMaterial(java.util.Optional.empty());
            };

            helper.setSourceSprite(QuadTransform.getSprite(GeometryRegistry.PlaceHolderSpriteIdentifiers.DECORATED_POT_SIDES));
            helper.setMaterial(sideMaterial);
            helper.setRendertype(ChunkSectionLayer.SOLID);
            helper.emitParts(sideParts, state, cullTest);
        }
    }

    private static void emitBanner(Predicate<@Nullable Direction> cullTest, RandomSource random,
                                   BlockState state, BlockRenderHelper helper, BlockEntity blockEntity) {
        if (!(blockEntity instanceof BannerBlockEntity bannerBE)) {
            return;
        }

        boolean isWallBanner = !state.hasProperty(BlockStateProperties.ROTATION_16);
        ModelLayerLocation baseLayer = isWallBanner ? ModelLayers.WALL_BANNER : ModelLayers.STANDING_BANNER;
        ModelLayerLocation flagLayer = isWallBanner ? ModelLayers.WALL_BANNER_FLAG : ModelLayers.STANDING_BANNER_FLAG;

        Map<String, BlockStateModel> basePairs = getPairs(baseLayer);
        Map<String, BlockStateModel> canvasPairs = getPairs(flagLayer);
        if (basePairs.isEmpty() || canvasPairs.isEmpty()) {
            return;
        }

        ArrayList<BlockStateModelPart> baseParts = partsBuf();
        addAllParts(baseParts, basePairs.values(), random);

        ArrayList<BlockStateModelPart> canvasParts = new ArrayList<>(32);
        for (BlockStateModel m : canvasPairs.values()) {
            List<BlockStateModelPart> parts = new ArrayList<>();
            m.collectParts(random, parts);
            if (!parts.isEmpty()) {
                canvasParts.addAll(parts);
            }
        }

        TextureAtlasSprite bannerPlaceholder = QuadTransform.getSprite(GeometryRegistry.PlaceHolderSpriteIdentifiers.BANNER);

        if (!baseParts.isEmpty()) {
            helper.setSourceSprite(bannerPlaceholder);
            helper.setMaterial(Sheets.BANNER_BASE);
            helper.setRendertype(ChunkSectionLayer.SOLID);
            helper.emitParts(baseParts, state, cullTest);
        }

        if (canvasParts.isEmpty()) {
            return;
        }

        int fancy = EnumTypes.BannerGraphicsType.FANCY.ordinal();
        ChunkSectionLayer rt = (ConfigCache.bannerGraphics == fancy) ? ChunkSectionLayer.TRANSLUCENT : ChunkSectionLayer.CUTOUT;

        helper.setSourceSprite(bannerPlaceholder);
        helper.setColor(bannerBE.getBaseColor().getTextureDiffuseColor());
        helper.setRendertype(rt);
        helper.emitParts(canvasParts, state, cullTest);

        for (BannerPatternLayers.Layer layer : bannerBE.getPatterns().layers()) {
            SpriteId layerMaterial = MaterialSelector.getBannerMaterial(layer.pattern());
            DyeColor layerColor = layer.color();

            helper.setSourceSprite(bannerPlaceholder);
            helper.setMaterial(layerMaterial);
            helper.setRendertype(rt);
            helper.setColor(layerColor.getTextureDiffuseColor());
            helper.emitParts(canvasParts, state, cullTest);
        }

        helper.setColor(-1);
    }

    private static void emitCopperGolemStatue(Predicate<@Nullable Direction> cullTest, RandomSource random,
                                              BlockState state, BlockRenderHelper helper) {
        ModelLayerLocation layer = ModelLayers.COPPER_GOLEM;
        var pose = state.getValue(BlockStateProperties.COPPER_GOLEM_POSE);
        if (pose == CopperGolemStatueBlock.Pose.SITTING) layer = ModelLayers.COPPER_GOLEM_SITTING;
        else if (pose == CopperGolemStatueBlock.Pose.RUNNING) layer = ModelLayers.COPPER_GOLEM_RUNNING;
        else if (pose == CopperGolemStatueBlock.Pose.STAR) layer = ModelLayers.COPPER_GOLEM_STAR;

        Map<String, BlockStateModel> pairs = getPairs(layer);
        if (pairs.isEmpty()) {
            return;
        }

        ArrayList<BlockStateModelPart> merged = partsBuf();
        addAllParts(merged, pairs.values(), random);
        if (merged.isEmpty()) {
            return;
        }

        Identifier texture = CopperGolemOxidationLevels.getOxidationLevel(
                ((CopperGolemStatueBlock) state.getBlock()).getWeatheringState()).texture();

        String path = texture.getPath();
        if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
        if (path.startsWith("textures/")) path = path.substring("textures/".length());

        Identifier strippedTexture = Identifier.withDefaultNamespace(path);
        TextureAtlasSprite sprite = QuadTransform.getSprite(strippedTexture);

        helper.setSourceSprite(QuadTransform.getSprite(GeometryRegistry.PlaceHolderSpriteIdentifiers.COPPER_GOLEM_STATUE));
        helper.setSprite(sprite);
        helper.setRendertype(ChunkSectionLayer.SOLID);
        helper.emitParts(merged, state, cullTest);
        helper.setSprite(null);
    }

    /**
     * True, when the block entity is in IMMEDIATE rendering mode (animating) — its lid should NOT be baked.
     */
    private static boolean shouldRenderLid(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }

        BlockEntityExt ext = (BlockEntityExt) blockEntity;
        return ext.rootbeer_utils$renderingMode() == RenderingMode.TERRAIN;
    }

    private static @Nullable BlockEntity tryGetBlockEntity(BlockPos pos, BlockAndTintGetter level) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) {
                return null;
            }

            return AltRenderers.hasRendererOverride(be.getType()) ? null : be;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, BlockStateModel> tryGetPairs(ModelLayerLocation location) {
        try {
            BlockStateModel m = GeometryRegistry.getModel(location);
            if (m instanceof MultiPartBlockModel multi) {
                return multi.getPairs();
            }

            return Map.of();
        } catch (Exception e) {
            TaskScheduler.schedule(() -> {
                if (ResourceTasks.populateGeometryRegistry() == ResourceTasks.FAILED) {
                    BBE.getLogger().error("Failed to repopulate geometry registry after a missing-layer lookup");
                    return;
                }

                SectionUpdateDispatcher.queueUpdateAllSections();
            });
            return Map.of();
        }
    }

    private static Map<String, BlockStateModel> getPairs(ModelLayerLocation location) {
        return tryGetPairs(location);
    }

    private static ArrayList<BlockStateModelPart> partsBuf() {
        ArrayList<BlockStateModelPart> buf = PARTS_BUF.get();
        buf.clear();
        return buf;
    }

    private static void addParts(ArrayList<BlockStateModelPart> out, BlockStateModel model, RandomSource random) {
        if (model == null) {
            return;
        }

        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);
        if (!parts.isEmpty()) {
            out.addAll(parts);
        }
    }

    private static void addAllParts(ArrayList<BlockStateModelPart> out, Iterable<BlockStateModel> models, RandomSource random) {
        for (BlockStateModel m : models) {
            addParts(out, m, random);
        }
    }
}
