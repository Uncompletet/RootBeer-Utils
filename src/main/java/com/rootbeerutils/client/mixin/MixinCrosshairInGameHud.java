package com.rootbeerutils.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Hud.class})
public class MixinCrosshairInGameHud {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    Identifier CUSTOM_CROSSHAIR = Identifier.fromNamespaceAndPath("rootbeerutils", "crosshair");

    public MixinCrosshairInGameHud() {
    }

    @Inject(
            method = {"extractCrosshair"},
            at = {@At("TAIL")}
    )
    private void drawCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (this.minecraft.crosshairPickEntity instanceof Entity) {
            int scaledWidth = 15;
            int scaledHeight = 15;
            context.blitSprite(RenderPipelines.CROSSHAIR, this.CUSTOM_CROSSHAIR, (context.guiWidth() - scaledWidth) / 2, (context.guiHeight() - scaledHeight) / 2, scaledWidth, scaledHeight);
        }

    }
}
