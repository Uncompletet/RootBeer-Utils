package com.rootbeerutils.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class MinecraftSetScreenAndShowMixin {

    @WrapOperation(
        method = "setScreenAndShow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;renderFrame(Z)V"
        )
    )
    private void rbutils$skipForcedFrame(Minecraft instance, boolean renderLevel, Operation<Void> original) {
        if (instance.gui.overlay() instanceof LoadingOverlay) {
            return;
        }

        original.call(instance, renderLevel);
    }
}
