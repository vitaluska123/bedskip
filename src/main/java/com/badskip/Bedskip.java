package com.badskip;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
		final Integer[] yearsInDay = new Integer[1];
		final String[] subtitles = new String[1];
		final String[] titles = new String[1];
		ArrayList<LivingEntity> playersCount = new ArrayList<>();

		Path path = FabricLoader.getInstance().getConfigDir();
		Path configpath = Path.of(path.toString(), "/bedskip.properties");
		try {
			if (!Files.exists(configpath)) {
				Files.createFile(configpath);
				Files.writeString(configpath, "#Utf-8" +
						"\n#Config:\n" +
						"#percent of players to skip 1-100\n" +
						"sleep-percent=50\n" +
						"#Length of a year (in days).\n" +
						"year_length_in_days=16\n\n" +
						"#Supported customization tags:\n" +
						"<dayOfYear>\n" +
						"<Year>\n" +
						"<PlayerName>\n" +
						"<TotalDays>\n" +
						"<DayTime>\n" +
						"#Castomization:\n" +
						"subtitle=<dayOfYear>day / <Year>year\n" +
						"title=Good morning, <PlayerName>",StandardCharsets.UTF_8);
			}
			List<String> config = Files.readAllLines(configpath, StandardCharsets.UTF_8);
			config.forEach(line -> {
				if (line.startsWith("sleep-percent")){
					percent[0] = Integer.parseInt(line.split("=")[1]);
				}
				if (line.startsWith("year_length_in_days")){
					yearsInDay[0] = Integer.parseInt(line.split("=")[1]);
				}
				if (line.startsWith("subtitle")){
					subtitles[0] = line.split("=")[1];
				}
				if (line.startsWith("title")){
					titles[0] = line.split("=")[1];
				}
			});


		}catch (IOException e) {
			throw new RuntimeException(e);
		}
		EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos)->{
			if (entity.isPlayer()){
				try {
					playersCount.remove(entity);
				}catch (Exception e){
					L.error(String.valueOf(e));
				}



			}
		});

		EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
			MinecraftServer server = entity.getServer();
			long time = server.getWorld(World.OVERWORLD).getTimeOfDay();
			int day = 0;
			while (time>24000){
				time-=24000;
				day++;
			}

			if (entity.isPlayer() &
					12542 <= time &
					time <= 23459)
			{
				playersCount.add(entity);
				if (playersCount.size()  / (double) server.getCurrentPlayerCount() * 100 >= percent[0]) {
					L.info("skipping night! day "+day+" -> "+(day+1));

					server.getCommandManager().executeWithPrefix(server.getCommandSource(),
							"time add " + (int) (24000 - time)
					);
					int TotalDay = day;
					long DayTime = time;
					server.getPlayerManager().getPlayerList().forEach(player -> {
						player.networkHandler.sendPacket(
								new TitleFadeS2CPacket(20,0,40)
						);
						long dayOfYear = TotalDay %yearsInDay[0];
						long Year = TotalDay / yearsInDay[0];
						String playerName = player.getName().getString();
						subtitles[0] = subtitles[0].replace("<dayOfYear>", String.valueOf(dayOfYear));
						subtitles[0] = subtitles[0].replace("<Year>", String.valueOf(Year));
						subtitles[0] = subtitles[0].replace("<PlayerName>", playerName);
						subtitles[0] = subtitles[0].replace("<TotalDays>", String.valueOf(TotalDay));
						subtitles[0] = subtitles[0].replace("<DayTime>", String.valueOf(DayTime));

						titles[0] = titles[0].replace("<dayOfYear>", String.valueOf(dayOfYear));
						titles[0] = titles[0].replace("<Year>", String.valueOf(Year));
						titles[0] = titles[0].replace("<PlayerName>", playerName);
						titles[0] = titles[0].replace("<TotalDays>", String.valueOf(TotalDay));
						titles[0] = titles[0].replace("<DayTime>", String.valueOf(DayTime));




						player.networkHandler.sendPacket(
								new SubtitleS2CPacket(Text.of(subtitles[0]))
						);
						player.networkHandler.sendPacket(
								new TitleS2CPacket(Text.literal(titles[0]).formatted(Formatting.AQUA))
						);
					});
				}
			}

		});


		L.info("Mod initialized!");
	}
}
