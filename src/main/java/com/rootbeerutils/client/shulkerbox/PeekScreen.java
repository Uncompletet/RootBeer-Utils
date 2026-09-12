/*
 * This file is derived from utility-mods by tranarchy, licensed under GNU General Public License v3.0.
 * Original source: https://github.com/tranarchy/utility-mods
 *
 * Modifications for VulkanMod / RootBeer-Utils integration are distributed under LGPL-3.0.
 */
package com.rootbeerutils.client.shulkerbox;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;

public class PeekScreen extends ContainerScreen {
    public PeekScreen(Component name, Container inventory) {
        super(ChestMenu.threeRows(-1, ShulkerBoxPreview.mc.player.getInventory(), inventory), ShulkerBoxPreview.mc.player.getInventory(), name);
    }
}
