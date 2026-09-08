package com.rootbeerutils.client.crosshair;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Crosshairindicator implements ClientModInitializer{
    public static final Logger LOGGER = LoggerFactory.getLogger("RBU-CrosshairIndicator");

    public Crosshairindicator() {
    }

    public void on() {
        LOGGER.info("CrosshairIndicator Loaded");
    }

    @Override
    public void onInitializeClient() {
    }
}
