/*
 * Derived from utility-mods (GNU General Public License v3.0). See com.rootbeerutils.client.shulkerbox.PeekScreen for details.
 */
package com.rootbeerutils.client.mixin;

import com.rootbeerutils.client.shulkerbox.ShulkerBoxPreview;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(at = @At("TAIL"), method = "tick")
    public void tick(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
        if (ShulkerBoxPreview.mc.gui.screen() instanceof ContainerScreen genericContainerScreen) {
            if (genericContainerScreen.getTitle().toString().contains("enderchest")) {
                ShulkerBoxPreview.echestWasOpened = true;
                ShulkerBoxPreview.enderChestItems.clear();
                genericContainerScreen.getMenu().slots.forEach(slot -> {
                    if (ShulkerBoxPreview.enderChestItems.size() < 27)
                        ShulkerBoxPreview.enderChestItems.add(slot.getItem());
                });
            }
        }
    }
}