package com.rootbeerutils.client.bbe.render.bers;

import com.rootbeerutils.client.bbe.config.ConfigCache;
import com.rootbeerutils.client.bbe.model.ChestModelOverride;
import com.rootbeerutils.client.bbe.render.OverlayRenderer;
import com.rootbeerutils.client.bbe.ext.BlockEntityRenderStateExt;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.SpecialDates;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BBEChestRenderer<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<@NonNull T, ChestRenderState> {

    public static final MultiblockChestResources<ModelLayerLocation> LAYERS;

    private static final Map<Direction, Transformation> TRANSFORMATIONS;
    private final SpriteGetter sprites;

    private final MultiblockChestResources<ChestModel> models;
    private final MultiblockChestResources<ChestModel> bbeModels;

    public BBEChestRenderer(final BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.models = LAYERS.map((layer) -> new ChestModel(context.bakeLayer(layer)));
        this.bbeModels = LAYERS.map((layer) -> new ChestModelOverride(context.bakeLayer(layer)));
    }

    public static boolean xmasTextures() {
        return ConfigCache.christmasChests || SpecialDates.isExtendedChristmas();
    }

    public @NonNull ChestRenderState createRenderState() {
        return new ChestRenderState();
    }

    public void extractRenderState(final @NonNull T blockEntity,
                                   final @NonNull ChestRenderState state,
                                   final float partialTicks,
                                   final @NonNull Vec3 cameraPosition,
                                   final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        boolean hasLevel = blockEntity.getLevel() != null;
        BlockState blockState = hasLevel ? blockEntity.getBlockState() : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        state.type = blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        state.facing = blockState.getValue(ChestBlock.FACING);
        state.material = getChestMaterial(blockEntity, xmasTextures());
        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combineResult;
        if (hasLevel && blockState.getBlock() instanceof ChestBlock chestBlock) {
            combineResult = chestBlock.combine(blockState, blockEntity.getLevel(), blockEntity.getBlockPos(), true);
        } else {
            combineResult = DoubleBlockCombiner.Combiner::acceptNone;
        }

        state.open = combineResult.apply(ChestBlock.opennessCombiner(blockEntity)).get(partialTicks);
        if (state.type != ChestType.SINGLE) {
            state.lightCoords = combineResult.apply(new BrightnessCombiner<>()).applyAsInt(state.lightCoords);
        }

        ((BlockEntityRenderStateExt)state).rootbeer_utils$blockEntity(blockEntity);
    }

    public void submit(final ChestRenderState state, final PoseStack poseStack, final @NonNull SubmitNodeCollector submitNodeCollector, final @NonNull CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(modelTransformation(state.facing));
        float open = state.open;
        open = 1.0F - open;
        open = 1.0F - open * open * open;
        SpriteId spriteId = Sheets.chooseSprite(state.material, state.type);

        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)state;

        ChestModel model = this.models.select(state.type);
        boolean managed = OverlayRenderer.manageCrumblingOverlay(stateExt.rootbeer_utils$blockEntity(), submitNodeCollector, poseStack, model, open, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
        if (!managed) {
            model = this.bbeModels.select(state.type);
            submitNodeCollector.submitModel(model, open, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, spriteId, this.sprites, 0, state.breakProgress);
        }

        poseStack.popPose();
    }

    private static ChestRenderState.ChestMaterialType getChestMaterial(final BlockEntity entity, final boolean xmasTextures) {
        Block blockState = entity.getBlockState().getBlock();
        if (blockState instanceof CopperChestBlock copperChestBlock) {
            return getChestMaterialType(copperChestBlock);
        } else if (entity instanceof EnderChestBlockEntity) {
            return ChestRenderState.ChestMaterialType.ENDER_CHEST;
        } else if (xmasTextures) {
            return ChestRenderState.ChestMaterialType.CHRISTMAS;
        } else {
            return entity instanceof TrappedChestBlockEntity ? ChestRenderState.ChestMaterialType.TRAPPED : ChestRenderState.ChestMaterialType.REGULAR;
        }
    }

    private static ChestRenderState.@NonNull ChestMaterialType getChestMaterialType(CopperChestBlock copperChestBlock) {
        ChestRenderState.ChestMaterialType var10000;
        switch (copperChestBlock.getState()) {
            case UNAFFECTED -> var10000 = ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
            case EXPOSED -> var10000 = ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
            case WEATHERED -> var10000 = ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
            case OXIDIZED -> var10000 = ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
            default -> throw new MatchException(null, null);
        }

        return var10000;
    }

    public static Transformation modelTransformation(final Direction facing) {
        return TRANSFORMATIONS.get(facing);
    }

    private static Transformation createModelTransformation(final Direction facing) {
        return new Transformation((new Matrix4f()).rotationAround(Axis.YP.rotationDegrees(-facing.toYRot()), 0.5F, 0.0F, 0.5F));
    }

    static {
        LAYERS = new MultiblockChestResources(ModelLayers.CHEST, ModelLayers.DOUBLE_CHEST_LEFT, ModelLayers.DOUBLE_CHEST_RIGHT);
        TRANSFORMATIONS = Util.makeEnumMap(Direction.class, BBEChestRenderer::createModelTransformation);
    }
}
