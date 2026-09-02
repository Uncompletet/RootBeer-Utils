/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 *
 * Hot-path-friendly flat view onto {@link BBEGameOptions}. The BBE chunk pipeline is allergic to
 * indirection on every block-emit so we cache settings into static fields and refresh on config
 * apply / load. Write through {@link #refreshFrom(BBEGameOptions)} only — never poke directly.
 */
package com.rootbeerutils.client.bbe.config;

import java.util.Arrays;

public final class ConfigCache {

    private ConfigCache() {
    }

    public static volatile boolean masterOptimize;
    public static volatile boolean skipVanillaForDedicated;

    public static volatile boolean optimizeChests;
    public static volatile boolean optimizeShulker;
    public static volatile boolean optimizeSigns;
    public static volatile boolean optimizeBells;
    public static volatile boolean optimizeBanners;
    public static volatile boolean optimizeDecoratedPots;
    public static volatile boolean optimizeCopperGolemStatue;

    public static volatile boolean christmasChests;

    public static volatile boolean chestAnims;
    public static volatile boolean shulkerAnims;
    public static volatile boolean bellAnims;
    public static volatile boolean potAnims;

    public static volatile boolean signText;
    public static volatile int signTextRenderDistance;
    public static volatile boolean signTextCulling;

    public static volatile int bannerGraphics;
    public static volatile int bannerPose;

    public static volatile int updateType;

    /**
     * Per-opt-kind quick-lookup table — indexed by {@code BlockEntityExt.optKind() & 0xFF}.
     */
    public static final boolean[] ENABLED = new boolean[256];

    public static void refreshFrom(BBEGameOptions options) {
        if (options == null) return;
        BBEGameOptions.OptimizationSettings opt = options.optimizations;

        masterOptimize = opt.enabled;
        skipVanillaForDedicated = opt.skipVanillaForDedicated;

        optimizeChests = opt.optimizeChests;
        optimizeShulker = opt.optimizeShulkerBoxes;
        optimizeSigns = opt.optimizeSigns;
        optimizeBells = opt.optimizeBells;
        optimizeBanners = opt.optimizeBanners;
        optimizeDecoratedPots = opt.optimizeDecoratedPots;
        optimizeCopperGolemStatue = opt.optimizeCopperGolemStatues;

        christmasChests = opt.christmasChests;

        chestAnims = options.animations.chests;
        shulkerAnims = options.animations.shulkers;
        bellAnims = options.animations.bells;
        potAnims = options.animations.pots;

        signText = options.signs.text;
        signTextRenderDistance = options.signs.textRenderDistance;
        signTextCulling = options.signs.textCulling;

        bannerGraphics = options.banners.graphics;
        bannerPose = options.banners.pose;

        updateType = options.scheduler.updateType;

        // Mirror per-opt-kind toggles into the dense table (slot 0 == NONE => always false).
        Arrays.fill(ENABLED, false);
        ENABLED[com.rootbeerutils.client.bbe.manager.InstancedBlockEntityManager.OptKind.CHEST  & 0xFF] = optimizeChests;
        ENABLED[com.rootbeerutils.client.bbe.manager.InstancedBlockEntityManager.OptKind.SIGN   & 0xFF] = optimizeSigns;
        ENABLED[com.rootbeerutils.client.bbe.manager.InstancedBlockEntityManager.OptKind.SHULKER& 0xFF] = optimizeShulker;
        ENABLED[com.rootbeerutils.client.bbe.manager.InstancedBlockEntityManager.OptKind.POT    & 0xFF] = optimizeDecoratedPots;
        ENABLED[com.rootbeerutils.client.bbe.manager.InstancedBlockEntityManager.OptKind.BANNER & 0xFF] = optimizeBanners;
        ENABLED[com.rootbeerutils.client.bbe.manager.InstancedBlockEntityManager.OptKind.BELL   & 0xFF] = optimizeBells;
        ENABLED[com.rootbeerutils.client.bbe.manager.InstancedBlockEntityManager.OptKind.CGS    & 0xFF] = optimizeCopperGolemStatue;
    }
}
