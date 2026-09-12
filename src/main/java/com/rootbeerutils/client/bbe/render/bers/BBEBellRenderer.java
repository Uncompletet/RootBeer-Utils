package com.rootbeerutils.client.bbe.render.bers;

import com.rootbeerutils.client.bbe.render.OverlayRenderer;
import com.rootbeerutils.client.bbe.ext.BlockEntityRenderStateExt;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.NonNull;

public class BBEBellRenderer implements BlockEntityRenderer<BellBlockEntity, BellRenderState> {

    public static final SpriteId BELL_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");

    private final SpriteGetter sprites;
    private final BellModel model;

    public BBEBellRenderer(final BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.model = new BellModel(context.bakeLayer(ModelLayers.BELL));
    }

    public @NonNull BellRenderState createRenderState() {
        return new BellRenderState();
    }

    public void extractRenderState(final @NonNull BellBlockEntity blockEntity,
                                   final @NonNull BellRenderState state,
                                   final float partialTicks,
                                   final @NonNull Vec3 cameraPosition,
                                   final ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.ticks = blockEntity.ticks + partialTicks;
        state.shakeDirection = blockEntity.shaking ? blockEntity.clickDirection : null;

        ((BlockEntityRenderStateExt)state).rootbeer_utils$blockEntity(blockEntity);
    }

    public void submit(final BellRenderState state, final @NonNull PoseStack poseStack, final @NonNull SubmitNodeCollector submitNodeCollector, final @NonNull CameraRenderState camera) {
        BellModel.State modelState = new BellModel.State(state.ticks, state.shakeDirection);
        this.model.setupAnim(modelState);

        BlockEntityRenderStateExt stateExt = (BlockEntityRenderStateExt)state;

        boolean managed = OverlayRenderer.manageCrumblingOverlay(stateExt.rootbeer_utils$blockEntity(), submitNodeCollector, poseStack, model, modelState, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
        if (!managed) {
            submitNodeCollector.submitModel(
                    this.model, modelState, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, BELL_TEXTURE, this.sprites, 0, state.breakProgress
            );
        }
    }
}
