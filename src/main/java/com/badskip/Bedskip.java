package com.badskip;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Bedskip implements ModInitializer {
	public static final String MOD_ID = "bedskip";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger L = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		final Integer[] percent = new Integer[1];
		final Integer[] playersCount = new Integer[1];
		playersCount[0] = 0;

		Path path = FabricLoader.getInstance().getConfigDir();
		Path configpath = Path.of(path.toString(), "/bedskip.properties");
		try {
			if (!Files.exists(configpath)) {
				Files.createFile(configpath);
				Files.writeString(configpath, "#percent of players to skip 1-100\nsleep-percent=50");
			}
			List<String> config = Files.readAllLines(configpath);
			config.forEach(line -> {
				if (line.startsWith("sleep-percent")){
					percent[0] = Integer.parseInt(line.split("=")[1]);
				}
			});


		}catch (IOException e) {
			throw new RuntimeException(e);
		}

		EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
			MinecraftServer server = entity.getServer();
			long time = server.getWorld(World.OVERWORLD).getTimeOfDay();
			int day = 0;
			while (time>24000){
				time-=24000;
				day++;
			}

			L.info(String.valueOf(time));
			if (entity.isPlayer() &
					12542 <= time &
					time <= 23459)
			{
				playersCount[0]++;
				if (playersCount[0] / server.getCurrentPlayerCount() * 100 >= percent[0]) {
					L.info("SKIPING");
					playersCount[0]=0;
					server.getCommandManager().executeWithPrefix(server.getCommandSource(),
							"time add " + (int) (24000 - time)
					);
					int finalDay = day;
					server.getPlayerManager().getPlayerList().forEach(player -> {
						player.networkHandler.sendPacket(
								new TitleFadeS2CPacket(20,0,40)
						);
						player.networkHandler.sendPacket(
								new SubtitleS2CPacket(Text.of("День номер "+ finalDay))
						);
						player.networkHandler.sendPacket(
								new TitleS2CPacket(Text.literal("Доброе утро, "+player.getName().getString()).formatted(Formatting.AQUA))
						);
					});
				}
			}

		});


		L.info("Mod initialized!");
	}
}