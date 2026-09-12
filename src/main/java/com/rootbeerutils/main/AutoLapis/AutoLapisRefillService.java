/*
 * This file is derived from AutoLapis by Nikrecs.
 * Modrinth: https://modrinth.com/mod/autolapis
 *
 * Modifications for VulkanMod / RootBeer-Utils integration are distributed under LGPL-3.0.
 */

package com.rootbeerutils.main.AutoLapis;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoLapisRefillService {
    private AutoLapisRefillService() {
    }

    public static void refillIfNeeded(Player player, AbstractContainerMenu menu) {
        if (menu instanceof EnchantmentMenu) {
            Slot lapisSlot = menu.getSlot(1);
            if (canRefill(lapisSlot)) {
                int refillCapacity = getRefillCapacity(lapisSlot);
                if (refillCapacity > 0) {
                    ItemStack movedLapis = extractLapis(player.getInventory(), refillCapacity);
                    if (!movedLapis.isEmpty()) {
                        ItemStack leftoverLapis = mergeIntoSlot(lapisSlot, movedLapis);
                        if (!leftoverLapis.isEmpty()) {
                            player.addItem(leftoverLapis);
                        }

                        menu.broadcastChanges();
                    }
                }
            }
        }
    }

    private static boolean canRefill(Slot lapisSlot) {
        ItemStack current = lapisSlot.getItem();
        return current.isEmpty() || current.is(Items.LAPIS_LAZULI);
    }

    private static int getRefillCapacity(Slot lapisSlot) {
        ItemStack current = lapisSlot.getItem();
        int slotLimit = Math.min(lapisSlot.getMaxStackSize(), 64);
        return Math.max(0, slotLimit - current.getCount());
    }

    private static ItemStack extractLapis(Inventory inventory, int targetAmount) {
        ItemStack collected = ItemStack.EMPTY;
        int remaining = targetAmount;

        for(int slot = 0; slot < inventory.getContainerSize() && remaining > 0; ++slot) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.LAPIS_LAZULI)) {
                int moved = Math.min(remaining, stack.getCount());
                ItemStack split = stack.split(moved);
                if (collected.isEmpty()) {
                    collected = split;
                } else {
                    collected.grow(split.getCount());
                }

                if (stack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }

                remaining -= moved;
            }
        }

        return collected;
    }

    private static ItemStack mergeIntoSlot(Slot lapisSlot, ItemStack movedLapis) {
        ItemStack current = lapisSlot.getItem();
        int slotLimit = Math.min(lapisSlot.getMaxStackSize(), 64);
        if (current.isEmpty()) {
            if (movedLapis.getCount() <= slotLimit) {
                lapisSlot.set(movedLapis);
                return ItemStack.EMPTY;
            } else {
                ItemStack slotStack = movedLapis.copy();
                slotStack.setCount(slotLimit);
                lapisSlot.set(slotStack);
                ItemStack leftover = movedLapis.copy();
                leftover.shrink(slotLimit);
                return leftover;
            }
        } else {
            int spaceLeft = Math.max(0, slotLimit - current.getCount());
            int movedCount = Math.min(spaceLeft, movedLapis.getCount());
            current.grow(movedCount);
            lapisSlot.setChanged();
            if (movedCount == movedLapis.getCount()) {
                return ItemStack.EMPTY;
            } else {
                ItemStack leftover = movedLapis.copy();
                leftover.shrink(movedCount);
                return leftover;
            }
        }
    }
}
