/*
 * Derived from AutoLapis. See com.rootbeerutils.main.AutoLapis.AutoLapisRefillService for details.
 */
package com.rootbeerutils.client.mixin;

import com.rootbeerutils.main.AutoLapis.AutoLapisRefillService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(method = "openMenu", at = @At("RETURN"))
    private void autolapis$refillEnchantingTable(MenuProvider menuProvider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (cir.getReturnValue().isPresent()) {
            AutoLapisRefillService.refillIfNeeded(player, player.containerMenu);
        }
    }
}
