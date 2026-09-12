package com.rootbeerutils.client.mixin;

import com.rootbeerutils.client.shulkerbox.PeekScreen;
import com.rootbeerutils.client.shulkerbox.ShulkerBoxPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class ECAbstractContainerScreenMixin {
    @Shadow
    protected Slot hoveredSlot;

    @Inject(at = @At("HEAD"), method = "extractTooltip", cancellable = true)
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int x, int y, CallbackInfo info) {
        if (hoveredSlot == null) {
            return;
        }

        ItemStack focusedStack = hoveredSlot.getItem();

        if (focusedStack.getItem() == Items.ENDER_CHEST && ShulkerBoxPreview.echestWasOpened) {
            Minecraft mc = Minecraft.getInstance();
            if (ShulkerBoxPreview.isExpandKeyPressed()) {
                    SimpleContainer peekInventory = new SimpleContainer(9 * 3);

                    for (int i = 0; i < ShulkerBoxPreview.enderChestItems.size(); i++) {
                        peekInventory.setItem(i, ShulkerBoxPreview.enderChestItems.get(i));
                    }

                    mc.gui.setScreen(new PeekScreen(focusedStack.getHoverName(), peekInventory));
                    info.cancel();
            }
        }
    }
}