package com.rootbeerutils.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public class HideLogoRendererMixin {

    @Inject(
        method = {
            "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V",
            "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V"
        },
        at = @At("HEAD"),
        cancellable = true
    )
    private void rbutils$gateLogo(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof TitleScreen) || mc.gui.overlay() != null) {
            ci.cancel();
        }
    }
}
