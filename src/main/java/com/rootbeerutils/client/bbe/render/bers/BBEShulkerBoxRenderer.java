package com.rootbeerutils.client.bbe.render.bers;

import com.rootbeerutils.client.bbe.render.OverlayRenderer;
import com.rootbeerutils.client.bbe.ext.BlockEntityRenderStateExt;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;

import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public class BBEShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity, ShulkerBoxRenderState> {

    private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, BBEShulkerBoxRenderer::createModelTransform);
    private final SpriteGetter sprites;
    private final BBEShulkerBoxModel model;

    public BBEShulkerBoxRenderer(final BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public BBEShulkerBoxRenderer(final EntityModelSet context, final SpriteGetter sprites) {
        this.sprites = sprites;
        this.model = new BBEShulkerBoxModel(context.bakeLayer(ModelLayers.SHULKER_BOX));
    }

    public @NonNull ShulkerBoxRenderState createRenderState() {
        return new ShulkerBoxRenderState();
    }

    public void extractRenderState(final @NonNull ShulkerBoxBlockEntity blockEntity,
                                   final @NonNull ShulkerBoxRenderState state,
                                   final float partialTicks,
                                   final @NonNull Vec3 cameraPosition,
                                   final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.direction = blockEntity.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
        state.color = blockEntity.getColor();
        state.progress = blockEntity.getProgress(partialTicks);

        ((BlockEntityRenderStateExt)state).rootbeer_utils$blockEntity(blockEntity);
    }

    public void submit(final ShulkerBoxRenderState state, final @NonNull PoseStack poseStack, final @NonNull SubmitNodeCollector submitNodeCollector, final @NonNull CameraRenderState camera) {
        DyeColor color = state.color;
        SpriteId sprite;
        if (color == null) {
            sprite = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
        } else {
            sprite = Sheets.getShulkerBoxSprite(color);
        }

        this.submit(state, poseStack, submitNodeCollector, state.lightCoords, state.direction, state.progress, state.breakProgress, sprite);
    }

    private void submit(final ShulkerBoxRenderState state,
                        final PoseStack poseStack,
                        final SubmitNodeCollector submitNodeCollector,
                        final int lightCoords,
                        final Direction direction,
                        final float progress,
                        final ModelFeatureRenderer.CrumblingOverlay breakProgress,
                        final SpriteId sprite) {
        poseStack.pushPose();
        poseStack.mulPose(modelTransform(direction));
        this.submit(state, poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, progress, breakProgress, sprite, 0);
        poseStack.popPose();
    }

    public void submit(final ShulkerBoxRenderState state,
                       final PoseStack poseStack,
                       final SubmitNodeCollector submitNodeCollector,
                       final int lightCoords,
                       final int overlayCoords,
                       final float progress,
                       final ModelFeatureRenderer.CrumblingOverlay breakProgress,
                       final SpriteId sprite,
                       final int outlineColor) {
        this.model.setupAnim(progress);

        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)state;

        boolean managed = OverlayRenderer.manageCrumblingOverlay(stateExt.rootbeer_utils$blockEntity(), submitNodeCollector, poseStack, model, progress, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
        if (!managed) {
            submitNodeCollector.submitModel(this.model, progress, poseStack, lightCoords, overlayCoords, -1, sprite, this.sprites, outlineColor, breakProgress);
        }
    }

    private static Transformation createModelTransform(final Direction direction) {
        return new Transformation(
                new Matrix4f()
                        .translation(0.5F, 0.5F, 0.5F)
                        .scale(0.9995F, 0.9995F, 0.9995F)
                        .rotate(direction.getRotation())
                        .scale(1.0F, -1.0F, -1.0F)
                        .translate(0.0F, -1.0F, 0.0F)
        );
    }

    public static Transformation modelTransform(final Direction direction) {
        return TRANSFORMATIONS.get(direction);
    }

    private static class BBEShulkerBoxModel extends Model<Float> {

        private final ModelPart lid;

        public BBEShulkerBoxModel(final ModelPart root) {
            super(root, RenderTypes::entityCutout);
            this.lid = root.getChild("lid");
        }

        public void setupAnim(final @NonNull Float progress) {
            super.setupAnim(progress);

            this.lid.setPos(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
            this.lid.yRot = 270.0F * progress * (float) (Math.PI / 180.0);
        }
    }
}
