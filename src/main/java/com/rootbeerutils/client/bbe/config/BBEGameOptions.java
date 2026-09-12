/*
 * Derived from BetterBlockEntities (LGPL-3.0). See com.rootbeerutils.client.bbe.BBE for details.
 * Config file layout adapted from "not-enough-vulkan" (NEV) for VulkanMod.
 */
package com.rootbeerutils.client.bbe.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rootbeerutils.client.bbe.BBE;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

/**
 * Persistent config, serialized as JSON to {@code config/rootbeer-utils-bbe-options.json}.
 * Mirrors NEV's GSON setup. Read by {@link com.rootbeerutils.client.bbe.config.ConfigCache}
 * via {@link ConfigCache#refreshFrom(BBEGameOptions)} after every load and after every menu apply.
 */
public class BBEGameOptions {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public final OptimizationSettings optimizations = new OptimizationSettings();
    public final AnimationSettings animations = new AnimationSettings();
    public final SignSettings signs = new SignSettings();
    public final BannerSettings banners = new BannerSettings();
    public final SchedulerSettings scheduler = new SchedulerSettings();

    private File file;

    public static BBEGameOptions load(File file) {
        BBEGameOptions config = null;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = GSON.fromJson(reader, BBEGameOptions.class);
            } catch (Exception e) {
                BBE.getLogger().error("Could not parse BBE config; falling back to defaults", e);
            }
        }
        if (config == null) config = new BBEGameOptions();

        config.file = file;
        config.writeChanges();
        ConfigCache.refreshFrom(config);
        return config;
    }

    public void writeChanges() {
        if (this.file == null) {
            ConfigCache.refreshFrom(this);
            return;
        }
        File dir = this.file.getParentFile();
        if (dir != null) {
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("Could not create parent directories for BBE config");
            } else if (dir.exists() && !dir.isDirectory()) {
                throw new RuntimeException("BBE config parent path exists but is not a directory");
            }
        }
        try (FileWriter writer = new FileWriter(this.file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException("Could not save BBE configuration file", e);
        }
        ConfigCache.refreshFrom(this);
    }

    public static class OptimizationSettings {

        /**
         * Master switch. When false, BBE renderer-substitution and alt-renderer dispatch are no-ops.
         * Defaults off so the mod ships non-disruptively.
         */
        public boolean enabled;

        /** Skip vanilla extraction for types that have a dedicated alt renderer. Advanced. */
        public boolean skipVanillaForDedicated;

        public boolean optimizeChests = true;
        public boolean optimizeShulkerBoxes = true;
        public boolean optimizeSigns = true;
        public boolean optimizeBells = true;
        public boolean optimizeBanners = true;
        public boolean optimizeDecoratedPots = true;
        public boolean optimizeCopperGolemStatues = true;

        public boolean christmasChests = false;

        public OptimizationSettings() {
            this.enabled = false;
            this.skipVanillaForDedicated = false;
        }
    }

    public static class AnimationSettings {
        public boolean chests = true;
        public boolean shulkers = true;
        public boolean bells = true;
        public boolean pots = true;
    }

    public static class SignSettings {
        public boolean text = true;
        public int textRenderDistance = 32;
        public boolean textCulling = true;
    }

    public static class BannerSettings {
        /** 0 = FAST, 1 = FANCY (matches {@link EnumTypes.BannerGraphicsType#ordinal()}). */
        public int graphics = 1;
        /** 0–9, controls baked-flag flag-droop pose for FAST graphics. */
        public int pose = 1;
    }

    public static class SchedulerSettings {
        /** 0 = FAST (always emit base+lid), 1 = SMART (skip base when lid is animated). */
        public int updateType = 1;
    }
}
