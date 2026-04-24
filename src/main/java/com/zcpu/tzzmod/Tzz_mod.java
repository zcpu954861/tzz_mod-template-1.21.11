package com.zcpu.tzzmod;

import com.zcpu.tzzmod.core.bootstrap.TzzCommandBootstrap;
import com.zcpu.tzzmod.core.bootstrap.TzzContentBootstrap;
import com.zcpu.tzzmod.core.bootstrap.TzzLifecycleBootstrap;
import com.zcpu.tzzmod.core.bootstrap.TzzNetworkBootstrap;
import com.zcpu.tzzmod.core.bootstrap.TzzServerBootstrap;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tzz_mod implements ModInitializer {
    public static final String MOD_ID = "tzz_mod";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TzzContentBootstrap.register();
        TzzNetworkBootstrap.register();
        TzzServerBootstrap.register();
        TzzLifecycleBootstrap.register();
        TzzCommandBootstrap.register();

        LOGGER.info("Tzz_mod initialized successfully.");
    }
}
