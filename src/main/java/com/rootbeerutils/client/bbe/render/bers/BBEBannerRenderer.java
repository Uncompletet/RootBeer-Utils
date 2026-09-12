package com.rootbeerutils.client.bbe.render.bers;

import com.rootbeerutils.client.bbe.config.ConfigCache;
import com.rootbeerutils.client.bbe.render.OverlayRenderer;
import com.rootbeerutils.client.bbe.ext.BlockEntityRenderStateExt;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.WallAndGroundTransformations;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@SuppressWarnings("DataFlowIssue")
public class BBEBannerRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {

    private static final Vector3fc MODEL_SCALE = new Vector3f(0.6666667F, -0.6666667F, -0.6666667F);
    private static final Vector3fc MODEL_TRANSLATION = new Vector3f(0.5F, 0.0F, 0.5F);
    public static final WallAndGroundTransformations<Transformation> TRANSFORMATIONS = new WallAndGroundTransformations<>(
            BBEBannerRenderer::createWallTransformation, BBEBannerRenderer::createGroundTransformation, 16
    );
    private final SpriteGetter sprites;
    private final BannerModel standingModel;
    private final BannerModel wallModel;
    private final BannerFlagModel standingFlagModel;
    private final BannerFlagModel wallFlagModel;

    public BBEBannerRenderer(final BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public BBEBannerRenderer(final EntityModelSet modelSet, final SpriteGetter sprites) {
        this.sprites = sprites;
        this.standingModel = new BannerModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER));
        this.wallModel = new BannerModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
    }

    @Override
    public BannerRenderState createRenderState() {
        return new BannerRenderState();
    }

    @Override
    public void extractRenderState(final BannerBlockEntity blockEntity,
                                   final BannerRenderState state,
                                   final float partialTicks,
                                   final Vec3 cameraPosition,
                                   final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.baseColor = blockEntity.getBaseColor();
        state.patterns = blockEntity.getPatterns();
        BlockState blockState = blockEntity.getBlockState();

        if (blockState.getBlock() instanceof BannerBlock) {
            state.transformation = TRANSFORMATIONS.freeTransformations(blockState.getValue(BannerBlock.ROTATION));
            state.attachmentType = BannerBlock.AttachmentType.GROUND;
        } else {
            state.transformation = TRANSFORMATIONS.wallTransformation(blockState.getValue(WallBannerBlock.FACING));
            state.attachmentType = BannerBlock.AttachmentType.WALL;
        }

        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        BlockPos blockPos = blockEntity.getBlockPos();
        state.phase = ((float)Math.floorMod(blockPos.getX() * 7L + blockPos.getY() * 9L + blockPos.getZ() * 13L + gameTime, 100L) + partialTicks) / 100.0F;

        ((BlockEntityRenderStateExt)state).rootbeer_utils$blockEntity(blockEntity);
    }

    private BannerModel bannerModel(final BannerBlock.AttachmentType type) {
        return switch (type) {
            case WALL -> this.wallModel;
            case GROUND -> this.standingModel;
        };
    }

    private BannerFlagModel flagModel(final BannerBlock.AttachmentType type) {
        return switch (type) {
            case WALL -> this.wallFlagModel;
            case GROUND -> this.standingFlagModel;
        };
    }

    @Override
    public void submit(final BannerRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(state.transformation);

        submitBanner(
                state,
                this.sprites,
                poseStack,
                submitNodeCollector,
                state.lightCoords,
                this.bannerModel(state.attachmentType),
                this.flagModel(state.attachmentType),
                state.phase,
                state.baseColor,
                state.patterns,
                state.breakProgress
        );
        poseStack.popPose();
    }

    private static void submitBanner(final BannerRenderState state,
                                     final SpriteGetter sprites,
                                     final PoseStack poseStack,
                                     final SubmitNodeCollector collector,
                                     final int lightCoords,
                                     final BannerModel model,
                                     final BannerFlagModel flagModel,
                                     final float phase,
                                     final DyeColor baseColor,
                                     final BannerPatternLayers patterns,
                                     final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        SpriteId sprite = Sheets.BANNER_BASE;

        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)state;

        boolean managed = OverlayRenderer.manageCrumblingOverlay(stateExt.rootbeer_utils$blockEntity(), collector, poseStack, model, Unit.INSTANCE, lightCoords, OverlayTexture.NO_OVERLAY, 0, breakProgress);
        if (!managed) {
            collector.submitModel(model, Unit.INSTANCE, poseStack, lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, sprites, 0, breakProgress);
        }

        float step = -0.45f;
        float rot = step * ConfigCache.bannerPose;
        float rotClamped = Math.clamp(rot, -4.05f, -0.45f);
        flagModel.root().getChild("flag").xRot = (float)Math.toRadians(rotClamped);

        // BannerFlagModel extends Model<Float>; the deferred-overlay path runs setupAnim(phase) at
        // submit time, but at *queue* time we don't have the phase yet, so pass null and rely on
        // OverlayRenderer's null-check before the eventual setupAnim call.
        boolean managed2 = OverlayRenderer.manageCrumblingOverlay(stateExt.rootbeer_utils$blockEntity(), collector, poseStack, flagModel, null, lightCoords, OverlayTexture.NO_OVERLAY, 0, breakProgress);
        if (!managed2) {
            collector.submitModel(flagModel, phase, poseStack, lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, sprites, 0, breakProgress);
        }

        if (!managed && !managed2) {
            submitPatterns(sprites, poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, flagModel, phase, true, baseColor, patterns, breakProgress);
        }
    }

    public static <S> void submitPatterns(final SpriteGetter sprites,
                                          final PoseStack poseStack,
                                          final SubmitNodeCollector submitNodeCollector,
                                          final int lightCoords,
                                          final int overlayCoords,
                                          final Model<S> model,
                                          final S state,
                                          final boolean banner,
                                          final DyeColor baseColor,
                                          final BannerPatternLayers patterns,
                                          final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        submitPatternLayer(
                sprites,
                poseStack,
                submitNodeCollector,
                lightCoords,
                overlayCoords,
                model,
                state,
                banner ? Sheets.BANNER_PATTERN_BASE : Sheets.SHIELD_PATTERN_BASE,
                baseColor,
                breakProgress
        );

        for (int maskIndex = 0; maskIndex < 16 && maskIndex < patterns.layers().size(); maskIndex++) {
            BannerPatternLayers.Layer layer = patterns.layers().get(maskIndex);
            SpriteId sprite = banner ? Sheets.getBannerSprite(layer.pattern()) : Sheets.getShieldSprite(layer.pattern());
            submitPatternLayer(sprites, poseStack, submitNodeCollector, lightCoords, overlayCoords, model, state, sprite, layer.color(), null);
        }
    }

    private static <S> void submitPatternLayer(final SpriteGetter sprites,
                                               final PoseStack poseStack,
                                               final SubmitNodeCollector submitNodeCollector,
                                               final int lightCoords,
                                               final int overlayCoords,
                                               final Model<S> model,
                                               final S state,
                                               final SpriteId sprite,
                                               final DyeColor color,
                                               final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        int diffuseColor = color.getTextureDiffuseColor();
        submitNodeCollector.submitModel(
                model, state, poseStack, sprite.renderType(RenderTypes::bannerPattern), lightCoords, overlayCoords, diffuseColor, sprites.get(sprite), 0, breakProgress
        );
    }

    private static Transformation modelTransformation(final float angle) {
        return new Transformation(MODEL_TRANSLATION, Axis.YP.rotationDegrees(-angle), MODEL_SCALE, null);
    }

    private static Transformation createGroundTransformation(final int segment) {
        return modelTransformation(RotationSegment.convertToDegrees(segment));
    }

    private static Transformation createWallTransformation(final Direction direction) {
        return modelTransformation(direction.toYRot());
    }
}
