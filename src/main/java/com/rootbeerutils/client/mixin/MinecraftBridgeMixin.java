package com.rootbeerutils.client.mixin;

import com.rootbeerutils.client.gui.CapturedFrame;
import com.rootbeerutils.client.gui.ReconfigBridgeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.ServerReconfigScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftBridgeMixin {

    @ModifyVariable(method = "setScreenAndShow", at = @At("HEAD"), argsOnly = true, name = "screen")
    private Screen rbutils$swapReconfigScreen(Screen screen) {
        if (screen instanceof ServerReconfigScreen reconfig) {
            return new ReconfigBridgeScreen(((ServerReconfigScreenAccessor) reconfig).rbutils$getConnection());
        }
        return screen;
    }

    @Inject(method = "clearClientLevel", at = @At("HEAD"))
    private void rbutils$captureBeforeClear(Screen screen, CallbackInfo ci) {
        CapturedFrame.captureLastFrame();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void rbutils$clearCaptureOnDisconnect(Screen screen, boolean keepResourcePacks, boolean stopSound, CallbackInfo ci) {
        CapturedFrame.initialJoin = true;
        CapturedFrame.clearCapturedTexture();
    }
}
