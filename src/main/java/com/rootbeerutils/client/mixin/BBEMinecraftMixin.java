/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 *
 * Wires {@link com.rootbeerutils.client.bbe.api.AltRenderDispatcher} into existence during
 * {@code Minecraft.<init>}. Without this hook the dispatcher stays null forever and every alt
 * renderer is dead code; the world-renderer mixin's null-checks just early-return.
 */
package com.rootbeerutils.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.rootbeerutils.client.bbe.BBE;
import com.rootbeerutils.client.bbe.api.AltRenderDispatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public abstract class BBEMinecraftMixin {

    @Shadow @Final public Font font;
    @Shadow @Final private ModelManager modelManager;
    @Shadow @Final private ItemModelResolver itemModelResolver;
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow @Final private AtlasManager atlasManager;
    @Shadow @Final private PlayerSkinRenderCache playerSkinRenderCache;
    @Shadow @Final private ReloadableResourceManager resourceManager;

    /**
     * Hook the {@code Tesselator.init()} call at the end of {@code Minecraft.<init>} — by that
     * point all the @Shadow fields above are populated. Constructs the dispatcher, registers it
     * as a resource-reload listener, then chains through to the original Tesselator init.
     */
    @WrapOperation(
            method = "<init>(Lnet/minecraft/client/main/GameConfig;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/Tesselator;init()V"
            ),
            require = 0
    )
    private void rbutils$bbe$initDispatcher(Operation<Void> original) {
        try {
            BBE.GlobalScope.altRenderDispatcher = new AltRenderDispatcher(
                    this.font,
                    this.modelManager.entityModels(),
                    new BlockModelResolver(this.modelManager),
                    this.itemModelResolver,
                    this.entityRenderDispatcher,
                    this.atlasManager,
                    this.playerSkinRenderCache
            );
            this.resourceManager.registerReloadListener(BBE.GlobalScope.altRenderDispatcher);
            BBE.getLogger().info("BBE alt-renderer dispatcher initialized");
        } catch (Throwable t) {
            BBE.getLogger().error("Failed to initialize BBE alt-renderer dispatcher", t);
        }

        original.call();
    }
}
