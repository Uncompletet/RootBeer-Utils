/*
 * Derived from BetterBlockEntities (LGPL-3.0). See BBE.java for details.
 * Menu integration pattern adapted from "not-enough-vulkan" (NEV).
 *
 * Reflectively binds against VulkanMod so the mod degrades gracefully when VulkanMod is absent.
 */
package com.rootbeerutils.client.bbe.config;

import com.rootbeerutils.client.bbe.BBE;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.lang.reflect.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registers a "BBE" entry in VulkanMod's settings screen, populated with toggle widgets backed by
 * {@link BBEGameOptions}. All VulkanMod types are loaded reflectively to keep the dependency soft.
 */
@SuppressWarnings("JavaReflectionInvocation")
public final class BBEMenu {

    private BBEMenu() {
    }

    private static final String VM_REGISTRY = "net.vulkanmod.config.gui.ModSettingsRegistry";
    private static final String VM_ENTRY    = "net.vulkanmod.config.gui.ModSettingsEntry";
    private static final String VM_BLOCK    = "net.vulkanmod.config.gui.OptionBlock";
    private static final String VM_PAGE     = "net.vulkanmod.config.option.OptionPage";
    private static final String VM_OPTION   = "net.vulkanmod.config.option.Option";
    private static final String VM_SWITCH   = "net.vulkanmod.config.option.SwitchOption";

    /**
     * Called from the mod's client entrypoint after BBE bootstraps.
     */
    public static void registerIfPresent(BBEGameOptions options) {
        if (!FabricLoader.getInstance().isModLoaded("vulkanmod")) {
            BBE.getLogger().info("VulkanMod not present — skipping BBE settings-screen integration");
            return;
        }

        try {
            registerWithVulkanMod(options);
            BBE.getLogger().info("Registered BBE entry in VulkanMod settings");
        } catch (Throwable t) {
            BBE.getLogger().error("Failed to register BBE entry in VulkanMod settings", t);
        }
    }

    private static void registerWithVulkanMod(BBEGameOptions options) throws Exception {
        Class<?> registryClass = Class.forName(VM_REGISTRY);
        Class<?> entryClass    = Class.forName(VM_ENTRY);
        Class<?> pageClass     = Class.forName(VM_PAGE);

        Field instanceField = registryClass.getField("INSTANCE");
        Object registry = instanceField.get(null);

        Component name = Component.literal("BBE").withStyle(ChatFormatting.GOLD);
        Supplier<Identifier> iconSupplier = () -> Identifier.fromNamespaceAndPath("rootbeerutils", "textures/rootbeerutils.png");

        Supplier<List<?>> pagesSupplier = () -> {
            try {
                return List.of(buildMainPage(pageClass, options));
            } catch (Exception e) {
                BBE.getLogger().error("Failed to build BBE option pages", e);
                return List.of();
            }
        };

        Runnable onApply = () -> {
            // Snapshot the value about to be replaced so we can detect changes that need a
            // resource reload (item models bake christmas/normal at load time; runtime toggles
            // don't re-bake them otherwise).
            boolean prevChristmas = ConfigCache.christmasChests;

            options.writeChanges();

            boolean nextChristmas = ConfigCache.christmasChests;
            if (prevChristmas != nextChristmas) {
                Minecraft mc = Minecraft.getInstance();
                BBE.getLogger().info("christmasChests toggled ? triggering resource reload to re-bake chest item models");
                mc.reloadResourcePacks();
            }
        };

        Constructor<?> entryCtor = pickConstructor(entryClass);
        Object[] entryArgs = {
              name, iconSupplier, pagesSupplier, onApply
        };

        Object entry = entryCtor.newInstance(entryArgs);

        Method addModEntry = registryClass.getMethod("addModEntry", entryClass);
        addModEntry.invoke(registry, entry);
    }

    private static Object buildMainPage(Class<?> pageClass, BBEGameOptions options) throws Exception {
        Class<?> blockClass  = Class.forName(VM_BLOCK);
        Class<?> optionClass = Class.forName(VM_OPTION);
        Class<?> switchClass = Class.forName(VM_SWITCH);

        // SwitchOption(Component name, Consumer<Boolean> setter, Supplier<Boolean> getter)
        Constructor<?> switchCtor = switchClass.getConstructor(Component.class, Consumer.class, Supplier.class);

        Object[] generalOptions = new Object[] {
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.enabled",
                        v -> options.optimizations.enabled = v,
                        () -> options.optimizations.enabled),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.skip_vanilla",
                        v -> options.optimizations.skipVanillaForDedicated = v,
                        () -> options.optimizations.skipVanillaForDedicated),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.christmas_chests",
                        v -> options.optimizations.christmasChests = v,
                        () -> options.optimizations.christmasChests)
        };

        Object[] perBlockOptions = new Object[] {
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.optimize_chests",
                        v -> options.optimizations.optimizeChests = v,
                        () -> options.optimizations.optimizeChests),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.optimize_shulkers",
                        v -> options.optimizations.optimizeShulkerBoxes = v,
                        () -> options.optimizations.optimizeShulkerBoxes),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.optimize_signs",
                        v -> options.optimizations.optimizeSigns = v,
                        () -> options.optimizations.optimizeSigns),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.optimize_bells",
                        v -> options.optimizations.optimizeBells = v,
                        () -> options.optimizations.optimizeBells),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.optimize_banners",
                        v -> options.optimizations.optimizeBanners = v,
                        () -> options.optimizations.optimizeBanners),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.optimize_decorated_pots",
                        v -> options.optimizations.optimizeDecoratedPots = v,
                        () -> options.optimizations.optimizeDecoratedPots),
                makeSwitch(switchCtor, "rootbeerutils.bbe.option.optimize_copper_golem_statues",
                        v -> options.optimizations.optimizeCopperGolemStatues = v,
                        () -> options.optimizations.optimizeCopperGolemStatues),
        };

        // OptionBlock(String title, Option[] options) — second arg is Option[], built via
        // reflective Array.newInstance to satisfy the exact runtime parameter type.
        Constructor<?> blockCtor = blockClass.getConstructor(String.class,
                Array.newInstance(optionClass, 0).getClass());

        Object[] generalBlockArgs  = { "General",   asOptionArray(optionClass, generalOptions) };
        Object[] perBlockBlockArgs = { "Per-Block", asOptionArray(optionClass, perBlockOptions) };
        Object generalBlock  = blockCtor.newInstance(generalBlockArgs);
        Object perBlockBlock = blockCtor.newInstance(perBlockBlockArgs);

        // OptionPage(String name, OptionBlock[] blocks)
        Constructor<?> pageCtor = pageClass.getConstructor(String.class,
                Array.newInstance(blockClass, 0).getClass());

        Object blocksArr = Array.newInstance(blockClass, 2);
        Array.set(blocksArr, 0, generalBlock);
        Array.set(blocksArr, 1, perBlockBlock);

        Object[] pageArgs = { "Better Block Entities", blocksArr };
        return pageCtor.newInstance(pageArgs);
    }

    private static Object makeSwitch(Constructor<?> ctor,
                                     String translationKey,
                                     Consumer<Boolean> setter,
                                     Supplier<Boolean> getter) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Object[] args = { Component.translatable(translationKey), setter, getter };
        return ctor.newInstance(args);
    }

    private static Object asOptionArray(Class<?> optionClass, Object[] src) {
        Object dst = Array.newInstance(optionClass, src.length);
        for (int i = 0; i < src.length; i++) {
            Array.set(dst, i, src[i]);
        }

        return dst;
    }

    /**
     * Pick a single declared constructor by parameter count. Used to avoid binding to specific
     * static parameter types that the IDE then complains about (e.g. {@code FormattedText} vs
     * {@code Component}). Throws if there isn't exactly one match.
     */
    private static Constructor<?> pickConstructor(Class<?> cls) {
        Constructor<?> match = null;
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (c.getParameterCount() == 4) {
                if (match != null) {
                    throw new IllegalStateException("Ambiguous constructor on " + cls.getName()
                            + " with " + 4 + " parameters");
                }

                match = c;
            }
        }

        if (match == null) {
            throw new IllegalStateException("No constructor on " + cls.getName()
                    + " with " + 4 + " parameters");
        }

        match.setAccessible(true);
        return match;
    }
}
