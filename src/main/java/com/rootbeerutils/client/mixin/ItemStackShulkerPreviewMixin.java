package com.rootbeerutils.client.mixin;

import com.rootbeerutils.client.shulkerbox.ShulkerBoxPreviewTooltipComponent;
import com.rootbeerutils.client.shulkerbox.ShulkerBoxPreview;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackShulkerPreviewMixin {

    @Inject(method = "getTooltipImage()Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private void rbutils$shulkerBoxPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() == Items.ENDER_CHEST && ShulkerBoxPreview.echestWasOpened) {
            cir.setReturnValue(Optional.of(
                    new ShulkerBoxPreviewTooltipComponent(ShulkerBoxPreview.enderChestItems)));
            return;
        }

        ItemContainerContents container = rbutils$shulkerContainer((ItemStack) (Object) this);
        if (container == null) {
            return;
        }

        NonNullList<ItemStack> inv = NonNullList.withSize(27, ItemStack.EMPTY);
        container.copyInto(inv);
        cir.setReturnValue(Optional.of(new ShulkerBoxPreviewTooltipComponent(inv)));
    }

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private <T extends TooltipProvider> void rbutils$skipShulkerContainerLines(DataComponentType<T> type, Item.TooltipContext context, TooltipDisplay display,
                                                                               Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci) {
        if (type != DataComponents.CONTAINER) {
            return;
        }

        if (rbutils$shulkerContainer((ItemStack) (Object) this) != null) {
            ci.cancel();
        }
    }

    @Unique
    private static ItemContainerContents rbutils$shulkerContainer(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return null;
        }

        return stack.get(DataComponents.CONTAINER);
    }
}
