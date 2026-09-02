package com.rootbeerutils.client.bbe.render.bers;

import com.rootbeerutils.client.bbe.model.MaterialSelector;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.DecoratedPotRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;

import org.joml.Matrix4f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@NullMarked
@SuppressWarnings("DataFlowIssue")
public class BBEDecoratedPotRenderer implements BlockEntityRenderer<DecoratedPotBlockEntity, DecoratedPotRenderState> {

    private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, BBEDecoratedPotRenderer::createModelTransformation);
    private final SpriteGetter sprites;
    private final ModelPart neck;
    private final ModelPart frontSide;
    private final ModelPart backSide;
    private final ModelPart leftSide;
    private final ModelPart rightSide;
    private final ModelPart top;
    private final ModelPart bottom;

    public BBEDecoratedPotRenderer(final BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public BBEDecoratedPotRenderer(final EntityModelSet entityModelSet, final SpriteGetter sprites) {
        this.sprites = sprites;
        ModelPart baseRoot = entityModelSet.bakeLayer(ModelLayers.DECORATED_POT_BASE);
        this.neck = baseRoot.getChild("neck");
        this.top = baseRoot.getChild("top");
        this.bottom = baseRoot.getChild("bottom");
        ModelPart sidesRoot = entityModelSet.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
        this.frontSide = sidesRoot.getChild("front");
        this.backSide = sidesRoot.getChild("back");
        this.leftSide = sidesRoot.getChild("left");
        this.rightSide = sidesRoot.getChild("right");
    }

    private static SpriteId getSideSprite(final @Nullable Item item) {
        return MaterialSelector.getDPSideMaterial(java.util.Optional.ofNullable(item));
    }

    @Override
    public DecoratedPotRenderState createRenderState() {
        return new DecoratedPotRenderState();
    }

    @Override
    public void extractRenderState(final DecoratedPotBlockEntity blockEntity,
                                   final DecoratedPotRenderState state,
                                   final float partialTicks,
                                   final Vec3 cameraPosition,
                                   final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.decorations = blockEntity.getDecorations();
        state.direction = blockEntity.getDirection();
        DecoratedPotBlockEntity.WobbleStyle wobbleStyle = blockEntity.lastWobbleStyle;
        if (wobbleStyle != null && blockEntity.getLevel() != null) {
            state.wobbleProgress = ((float)(blockEntity.getLevel().getGameTime() - blockEntity.wobbleStartedAtTick) + partialTicks) / wobbleStyle.duration;
        } else {
            state.wobbleProgress = 0.0F;
        }
    }

    @Override
    public void submit(final DecoratedPotRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(modelTransformation(state.direction));
        if (state.wobbleProgress >= 0.0F && state.wobbleProgress <= 1.0F) {
            if (state.wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE) {
                float deltaTime = state.wobbleProgress * (float) (Math.PI * 2);
                float tiltX = -1.5F * (Mth.cos(deltaTime) + 0.5F) * Mth.sin(deltaTime / 2.0F);
                poseStack.rotateAround(Axis.XP.rotation(tiltX * 0.015625F), 0.5F, 0.0F, 0.5F);
                float tiltZ = Mth.sin(deltaTime);
                poseStack.rotateAround(Axis.ZP.rotation(tiltZ * 0.015625F), 0.5F, 0.0F, 0.5F);
            } else {
                float turnAngle = Mth.sin(-state.wobbleProgress * 3.0F * (float) Math.PI) * 0.125F;
                float linearDecayFactor = 1.0F - state.wobbleProgress;
                poseStack.rotateAround(Axis.YP.rotation(turnAngle * linearDecayFactor), 0.5F, 0.0F, 0.5F);
            }
        }

        this.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.decorations, 0);
        poseStack.popPose();
    }

    public static Transformation modelTransformation(final Direction facing) {
        return TRANSFORMATIONS.get(facing);
    }

    private static Transformation createModelTransformation(final Direction entityDirection) {
        return new Transformation(new Matrix4f().rotateAround(Axis.YP.rotationDegrees(180.0F - entityDirection.toYRot()), 0.5F, 0.5F, 0.5F));
    }

    public void submit(final PoseStack poseStack,
                       final SubmitNodeCollector submitNodeCollector,
                       final int lightCoords,
                       final int overlayCoords,
                       final PotDecorations decorations,
                       final int outlineColor) {
        RenderType renderType = Sheets.DECORATED_POT_BASE.renderType(RenderTypes::entitySolid);
        TextureAtlasSprite sprite = this.sprites.get(Sheets.DECORATED_POT_BASE);
        submitPart(submitNodeCollector, this.neck, poseStack, renderType, lightCoords, overlayCoords, sprite, outlineColor);
        submitPart(submitNodeCollector, this.top, poseStack, renderType, lightCoords, overlayCoords, sprite, outlineColor);
        submitPart(submitNodeCollector, this.bottom, poseStack, renderType, lightCoords, overlayCoords, sprite, outlineColor);
        SpriteId frontSprite = getSideSprite(decorations.front().orElse(null));
        submitPart(submitNodeCollector, this.frontSide, poseStack, frontSprite.renderType(RenderTypes::entitySolid),
                lightCoords, overlayCoords, this.sprites.get(frontSprite), outlineColor);
        SpriteId backSprite = getSideSprite(decorations.back().orElse(null));
        submitPart(submitNodeCollector, this.backSide, poseStack, backSprite.renderType(RenderTypes::entitySolid),
                lightCoords, overlayCoords, this.sprites.get(backSprite), outlineColor);
        SpriteId leftSprite = getSideSprite(decorations.left().orElse(null));
        submitPart(submitNodeCollector, this.leftSide, poseStack, leftSprite.renderType(RenderTypes::entitySolid),
                lightCoords, overlayCoords, this.sprites.get(leftSprite), outlineColor);
        SpriteId rightSprite = getSideSprite(decorations.right().orElse(null));
        submitPart(submitNodeCollector, this.rightSide, poseStack, rightSprite.renderType(RenderTypes::entitySolid),
                lightCoords, overlayCoords, this.sprites.get(rightSprite), outlineColor);
    }

    private static void submitPart(final SubmitNodeCollector collector, final ModelPart part, final PoseStack poseStack,
                                   final RenderType renderType, final int lightCoords, final int overlayCoords,
                                   final TextureAtlasSprite sprite, final int outlineColor) {
        collector.submitModelPart(part, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, outlineColor);
    }
}
