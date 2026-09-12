package com.rootbeerutils.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoadingOverlay.class)
public class HideReloadOverlayMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private long fadeOutStart;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void rbutils$skipReloadRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                          float partialTick, CallbackInfo ci) {
        if (this.minecraft.level == null
            && this.minecraft.gui.screen() != null
            && this.minecraft.isGameLoadFinished()) {
            this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
        }

        if (this.fadeOutStart != -1L) {
            this.minecraft.gui.setOverlay(null);
        }

        ci.cancel();
    }

    @Inject(method = "isReadyToFadeOut", at = @At("RETURN"), cancellable = true)
    private void rbutils$alwaysReadyToFadeOut(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
