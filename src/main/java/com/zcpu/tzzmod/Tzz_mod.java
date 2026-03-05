package com.zcpu.tzzmod;

import com.zcpu.tzzmod.ModItem.ModItemGroup;
import com.zcpu.tzzmod.ModItem.ModItems;
import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.network.DeathStatusPayload;
import com.zcpu.tzzmod.network.DeathSyncServer;
import com.zcpu.tzzmod.network.PhoneChatPayloads;
import com.zcpu.tzzmod.phone.chat.PhoneChatServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zcpu.tzzmod.command.SendMsgCommand;

public class Tzz_mod implements ModInitializer {
	public static final String MOD_ID = "tzz_mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModItems.initialize();
		ModItemGroup.inialize();
		// Ensure blocks are registered by forcing ModBlocks static initialization
		ModBlocks.init();

		DeathStatusPayload.register();
		DeathSyncServer.register();
		PhoneChatPayloads.register();
		PhoneChatServer.register();

        // Register server commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SendMsgCommand.register(dispatcher);
        });

		LOGGER.info("Hello Fabric world!");
	}
}