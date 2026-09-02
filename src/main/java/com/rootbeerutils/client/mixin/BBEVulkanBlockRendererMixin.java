/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 *
 * Hook into VulkanMod's per-block chunk-mesh emission. Fires at the start of {@code renderBlock}
 * BEFORE VulkanMod runs the vanilla {@code BlockStateModel.emitQuads}. We dispatch to BBEEmitter
 * which emits substitution geometry (chest/banner/...) into the same {@code QuadEmitter}. The
 * original method continues afterward and emits the vanilla model (typically empty for these
 * blocks), so VulkanMod never sees an inconsistent emit state.
 *
 * <p>This is the equivalent of BBE's Sodium {@code BlockRendererMixin} which intercepted
 * {@code PlatformModelEmitter#emitModel} inside Sodium's {@code BlockRenderer.renderModel}.</p>
 */
package com.rootbeerutils.client.mixin;

import com.rootbeerutils.client.bbe.BBE;
import com.rootbeerutils.client.bbe.pipeline.BBEEmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Vector3f;

import java.util.function.Predicate;

@Pseudo
@Mixin(targets = "net.vulkanmod.render.chunk.build.renderer.BlockRenderer", remap = false)
public abstract class BBEVulkanBlockRendererMixin {

    @Inject(
            method = "renderBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lorg/joml/Vector3f;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;emitQuads(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/function/Predicate;)V"
            ),
            require = 0,
            allow = 1
    )
    private void rbutils$bbe$emitSubstitution(BlockState state, BlockPos pos, Vector3f offset, CallbackInfo ci) {
        AbstractBlockRenderContextAccessor self = (AbstractBlockRenderContextAccessor) this;
        Predicate<Direction> cullTest = self::invokeIsFaceCulled;
        try {
            BBEEmitter.emit(
                    self.invokeGetEmitter(),
                    self.getRenderRegion(),
                    self.getBlockPos(),
                    self.getBlockState(),
                    self.getRandom(),
                    cullTest
            );
        } catch (Throwable t) {
            // Never let a substitution failure abort chunk meshing — it would dirty the chunk.
            BBE.getLogger().error("BBE chunk-mesh substitution threw", t);
        }
    }
}
