/*
 * Derived from utility-mods (GNU General Public License v3.0). See com.rootbeerutils.client.shulkerbox.PeekScreen for details.
 */
package com.rootbeerutils.client.mixin;

import com.rootbeerutils.client.shulkerbox.ShulkerBoxPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {
    @Inject(at = @At(value = "HEAD"), method = "connect")
    private void connect(final Minecraft minecraft, final ServerAddress serverAddress, final ServerData serverData, @Nullable final TransferState transferState, CallbackInfo callbackInfo) {
        ShulkerBoxPreview.echestWasOpened = false;
    }
}
