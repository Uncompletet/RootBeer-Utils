package com.rootbeerutils.client.mixin;

import com.rootbeerutils.client.bbe.api.AltRenderers;
import com.rootbeerutils.client.bbe.config.ConfigCache;
import com.rootbeerutils.client.bbe.render.bers.*;

import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(BlockEntityRenderers.class)
public class BBEBlockEntityRenderersMixin {

    /**
     * To replace vanilla renderers, we can't mixin into the static initializer as we need this function's
     * reload capabilities.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "createEntityRenderers", at = @At("HEAD"))
    private static void replaceVanillaRenderers(CallbackInfoReturnable<Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>>> cir) {
        BlockEntityType<?> chest = type("chest");
        if (AltRenderers.hasRendererOverride(chest)) {
            removeRegistration(chest);
        } else {
            BlockEntityRendererProvider r2 =
                    ConfigCache.optimizeChests ? BBEChestRenderer::new : ChestRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(chest, r2);
        }

        BlockEntityType<?> enderChest = type("ender_chest");
        if (AltRenderers.hasRendererOverride(enderChest)) {
            removeRegistration(enderChest);
        } else {
            BlockEntityRendererProvider r3 =
                    ConfigCache.optimizeChests ? BBEChestRenderer::new : ChestRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(enderChest, r3);
        }

        BlockEntityType<?> trappedChest = type("trapped_chest");
        if (AltRenderers.hasRendererOverride(trappedChest)) {
            removeRegistration(trappedChest);
        } else {
            BlockEntityRendererProvider r4 =
                    ConfigCache.optimizeChests ? BBEChestRenderer::new : ChestRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(trappedChest, r4);
        }

        BlockEntityType<?> banner = type("banner");
        if (AltRenderers.hasRendererOverride(banner)) {
            removeRegistration(banner);
        } else {
            BlockEntityRendererProvider r5 =
                    ConfigCache.optimizeBanners ? BBEBannerRenderer::new : BannerRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(banner, r5);
        }

        BlockEntityType<?> shulkerBox = type("shulker_box");
        if (AltRenderers.hasRendererOverride(shulkerBox)) {
            removeRegistration(shulkerBox);
        } else {
            BlockEntityRendererProvider r6 =
                    ConfigCache.optimizeShulker ? BBEShulkerBoxRenderer::new : ShulkerBoxRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(shulkerBox, r6);
        }

        BlockEntityType<?> bell = type("bell");
        if (AltRenderers.hasRendererOverride(bell)) {
            removeRegistration(bell);
        } else {
            BlockEntityRendererProvider r8 =
                    ConfigCache.optimizeBells ? BBEBellRenderer::new : BellRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(bell, r8);
        }

        BlockEntityType<?> decoratedPot = type("decorated_pot");
        if (AltRenderers.hasRendererOverride(decoratedPot)) {
            removeRegistration(decoratedPot);
        } else {
            BlockEntityRendererProvider r9 =
                    ConfigCache.optimizeDecoratedPots ? BBEDecoratedPotRenderer::new : DecoratedPotRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(decoratedPot, r9);
        }

        BlockEntityType<?> copperGolemStatue = type("copper_golem_statue");
        if (AltRenderers.hasRendererOverride(copperGolemStatue)) {
            removeRegistration(copperGolemStatue);
        } else {
            BlockEntityRendererProvider r10 =
                    ConfigCache.optimizeCopperGolemStatue ? BBECopperGolemStatueBlockRenderer::new : CopperGolemStatueBlockRenderer::new;
            BlockEntityRenderersAccessor.invokeRegister(copperGolemStatue, r10);
        }
    }

    private static BlockEntityType<?> type(String name) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(name));
    }

    @Unique
    private static void removeRegistration(BlockEntityType<?> blockEntityType) {
        BlockEntityRenderersAccessor.invokeRegister(blockEntityType, _ -> new BBEDummyRenderer());
    }
}
