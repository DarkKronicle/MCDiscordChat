package top.xujiayao.mcdiscordchat.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import top.xujiayao.mcdiscordchat.multiServer.MultiServer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static top.xujiayao.mcdiscordchat.Main.CHANNEL;
import static top.xujiayao.mcdiscordchat.Main.CHANNEL_TOPIC_MONITOR_TIMER;
import static top.xujiayao.mcdiscordchat.Main.CHECK_UPDATE_TIMER;
import static top.xujiayao.mcdiscordchat.Main.CONFIG;
import static top.xujiayao.mcdiscordchat.Main.CONSOLE_LOG_CHANNEL;
import static top.xujiayao.mcdiscordchat.Main.CONSOLE_LOG_THREAD;
import static top.xujiayao.mcdiscordchat.Main.HTTP_CLIENT;
import static top.xujiayao.mcdiscordchat.Main.JDA;
import static top.xujiayao.mcdiscordchat.Main.LOGGER;
import static top.xujiayao.mcdiscordchat.Main.MSPT_MONITOR_TIMER;
import static top.xujiayao.mcdiscordchat.Main.MULTI_SERVER;
import static top.xujiayao.mcdiscordchat.Main.SERVER;
import static top.xujiayao.mcdiscordchat.Main.SERVER_STARTED_TIME;
import static top.xujiayao.mcdiscordchat.Main.TEXTS;
import static top.xujiayao.mcdiscordchat.Main.UPDATE_NOTIFICATION_CHANNEL;
import static top.xujiayao.mcdiscordchat.Main.VERSION;

/**
 * @author Xujiayao
 */
public class Utils {

	public static String adminsMentionString() {
		StringBuilder text = new StringBuilder();

		for (String id : CONFIG.generic.adminsIds) {
			text.append(Objects.requireNonNull(JDA.getUserById(id)).getAsMention()).append(" ");
		}

		return text.toString();
	}

	public static String checkUpdate(boolean isManualCheck) {
		try {
			Request request = new Request.Builder()
					.url("https://cdn.jsdelivr.net/gh/Xujiayao/MCDiscordChat@master/update/version.json")
					.build();

			try (Response response = HTTP_CLIENT.newCall(request).execute()) {
				String result = Objects.requireNonNull(response.body()).string();

				JsonObject latestJson = new Gson().fromJson(result, JsonObject.class);
				String latestVersion = latestJson.get("version").getAsString();

				StringBuilder message = new StringBuilder();

				CONFIG.latestVersion = latestVersion;
				ConfigManager.update();

				if (!latestVersion.equals(VERSION)) {
					if (CONFIG.latestVersion.equals(latestVersion)
							&& CONFIG.latestCheckTime > (System.currentTimeMillis() - 172800000)
							&& !isManualCheck) {
						return "";
					}

					CONFIG.latestCheckTime = System.currentTimeMillis();
					ConfigManager.update();

					message.append(CONFIG.generic.useEngInsteadOfChin ? "**A new version is available!**" : "**新版本可用！**");
					message.append("\n\n");
					message.append("MCDiscordChat **").append(VERSION).append("** -> **").append(latestVersion).append("**");
					message.append("\n\n");
					message.append(CONFIG.generic.useEngInsteadOfChin ? "Download link: <https://github.com/Xujiayao/MCDiscordChat/blob/master/README.md#Download>" : "下载链接：<https://github.com/Xujiayao/MCDiscordChat/blob/master/README_CN.md#%E4%B8%8B%E8%BD%BD>");
					message.append("\n\n");
					message.append(CONFIG.generic.useEngInsteadOfChin ? "Changelog: " : "更新日志：").append(latestJson.get("changelog").getAsString());
					message.append("\n\n");

					if (CONFIG.generic.mentionAdminsForUpdates) {
						message.append(adminsMentionString());
					}

					return message.toString();
				} else {
					message.append("MCDiscordChat **").append(VERSION).append("**");
					message.append("\n\n");
					message.append(CONFIG.generic.useEngInsteadOfChin ? "**MCDiscordChat is up to date!**" : "**当前版本已经是最新版本！**");

					return isManualCheck ? message.toString() : "";
				}
			}
		} catch (Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
			return "";
		}
	}

	public static String reload() {
		try {
			MSPT_MONITOR_TIMER.cancel();
			CHANNEL_TOPIC_MONITOR_TIMER.cancel();
			CHECK_UPDATE_TIMER.cancel();

			if (CONFIG.multiServer.enable) {
				MULTI_SERVER.bye();
				MULTI_SERVER.stopMultiServer();
			}

			ConfigManager.init(true);

			Utils.testJsonValid();

			Utils.setBotActivity();

			CHANNEL = JDA.getTextChannelById(CONFIG.generic.channelId);
			CONSOLE_LOG_THREAD.interrupt();
			CONSOLE_LOG_THREAD.join(5000);
			if (!CONFIG.generic.consoleLogChannelId.isEmpty()) {
				CONSOLE_LOG_CHANNEL = JDA.getTextChannelById(CONFIG.generic.consoleLogChannelId);
				CONSOLE_LOG_THREAD = new Thread(new ConsoleLogListener(false));
				CONSOLE_LOG_THREAD.start();
			}
			if (!CONFIG.generic.updateNotificationChannelId.isEmpty()) {
				UPDATE_NOTIFICATION_CHANNEL = JDA.getTextChannelById(CONFIG.generic.updateNotificationChannelId);
			}
			if (UPDATE_NOTIFICATION_CHANNEL == null) {
				UPDATE_NOTIFICATION_CHANNEL = CHANNEL;
			}

			Utils.updateBotCommands();

			CHECK_UPDATE_TIMER = new Timer();
			Utils.initCheckUpdateTimer();

			MSPT_MONITOR_TIMER = new Timer();
			if (CONFIG.generic.announceHighMspt) {
				Utils.initMsptMonitor();
			}

			if (CONFIG.multiServer.enable) {
				MULTI_SERVER = new MultiServer();
				MULTI_SERVER.start();
			}

			CHANNEL_TOPIC_MONITOR_TIMER = new Timer();
			if (CONFIG.generic.updateChannelTopic) {
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						if (!CONFIG.multiServer.enable) {
							Utils.initChannelTopicMonitor();
						} else if (MULTI_SERVER.server != null) {
							MULTI_SERVER.initMultiServerChannelTopicMonitor();
						}
					}
				}, 2000);
			}

			return CONFIG.generic.useEngInsteadOfChin ? "**Config file reloaded successfully!**" : "**配置文件重新加载成功！**";
		} catch (Exception ex) {
			LOGGER.error(ExceptionUtils.getStackTrace(ex));
			return CONFIG.generic.useEngInsteadOfChin ? "**Config file reload failed!**" : "**配置文件重新加载失败！**";
		}
	}

	public static String getInfoCommandMessage() {
		StringBuilder message = new StringBuilder()
				.append("=============== ")
				.append(CONFIG.generic.useEngInsteadOfChin ? "Server Status" : "运行状态")
				.append(" ===============\n\n");

		// Online players
		List<ServerPlayerEntity> onlinePlayers = SERVER.getPlayerManager().getPlayerList();
		message.append(CONFIG.generic.useEngInsteadOfChin ? "Online players (" : "在线玩家 (")
				.append(onlinePlayers.size())
				.append("/")
				.append(SERVER.getPlayerManager().getMaxPlayerCount())
				.append(")")
				.append(CONFIG.generic.useEngInsteadOfChin ? ":" : "：")
				.append("\n");

		if (onlinePlayers.isEmpty()) {
			message.append(CONFIG.generic.useEngInsteadOfChin ? "No players online!\n" : "当前没有在线玩家！\n");
		} else {
			for (ServerPlayerEntity player : onlinePlayers) {
				message.append("[").append(player.pingMilliseconds).append("ms] ").append(player.getEntityName()).append("\n");
			}
		}

		// Server TPS
		double serverTickTime = MathHelper.average(SERVER.lastTickLengths) * 1.0E-6D;
		message.append(CONFIG.generic.useEngInsteadOfChin ? "\nServer TPS:\n" : "\n服务器 TPS：\n")
				.append(String.format("%.2f", Math.min(1000.0 / serverTickTime, 20)));

		// Server MSPT
		message.append(CONFIG.generic.useEngInsteadOfChin ? "\n\nServer MSPT:\n" : "\n\n服务器 MSPT：\n")
				.append(String.format("%.2f", serverTickTime));

		// Server used memory
		message.append(CONFIG.generic.useEngInsteadOfChin ? "\n\nServer used memory:\n" : "\n\n服务器已用内存：\n")
				.append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024)
				.append(" MB / ")
				.append(Runtime.getRuntime().totalMemory() / 1024 / 1024)
				.append(" MB");

		return message.toString();
	}

	public static String getStatsCommandMessage(String type, String name) {
		StringBuilder message = new StringBuilder()
				.append("=============== ")
				.append(CONFIG.generic.useEngInsteadOfChin ? "Scoreboard" : "排行榜")
				.append(" ===============\n");

		Map<String, Integer> stats = new HashMap<>();

		try {
			JsonArray players = new Gson().fromJson(IOUtils.toString(new File(FabricLoader.getInstance().getGameDir().toAbsolutePath() + "/usercache.json").toURI(), StandardCharsets.UTF_8), JsonArray.class);

			//#if MC >= 11600
			FileUtils.listFiles(new File((SERVER.getSaveProperties().getLevelName() + "/stats/")), null, false).forEach(file -> {
			//#else
			//$$ FileUtils.listFiles(new File((SERVER.getLevelName() + "/stats/")), null, false).forEach(file -> {
			//#endif
				try {
					for (JsonElement player : players) {
						if (player.getAsJsonObject().get("uuid").getAsString().equals(file.getName().replace(".json", ""))) {
							JsonObject json = new Gson().fromJson(IOUtils.toString(file.toURI(), StandardCharsets.UTF_8), JsonObject.class);

							try {
								stats.put(player.getAsJsonObject().get("name").getAsString(), json
										.getAsJsonObject("stats")
										.getAsJsonObject("minecraft:" + type)
										.get("minecraft:" + name)
										.getAsInt());
							} catch (NullPointerException ignored) {
							}
						}
					}
				} catch (Exception e) {
					LOGGER.error(ExceptionUtils.getStackTrace(e));
				}
			});
		} catch (Exception e) {
			LOGGER.error(ExceptionUtils.getStackTrace(e));
		}

		if (stats.isEmpty()) {
			message.append(CONFIG.generic.useEngInsteadOfChin ? "\nNo result" : "\n无结果");
		} else {
			List<Map.Entry<String, Integer>> sortedlist = new ArrayList<>(stats.entrySet());
			sortedlist.sort((c1, c2) -> c2.getValue().compareTo(c1.getValue()));

			for (Map.Entry<String, Integer> entry : sortedlist) {
				message.append(String.format("\n%-8d %-8s", entry.getValue(), entry.getKey()));
			}
		}

		return message.toString();
	}

	public static void reloadTexts() {
		if (CONFIG.generic.useEngInsteadOfChin) {
			TEXTS = new Texts(CONFIG.textsEN.unformattedResponseMessage,
					CONFIG.textsEN.unformattedChatMessage,
					CONFIG.textsEN.unformattedOtherMessage,
					CONFIG.textsEN.unformattedCommandNotice,
					new Gson().toJson(CONFIG.textsEN.formattedResponseMessage),
					new Gson().toJson(CONFIG.textsEN.formattedChatMessage),
					new Gson().toJson(CONFIG.textsEN.formattedOtherMessage),
					new Gson().toJson(CONFIG.textsEN.formattedCommandNotice),
					CONFIG.textsEN.serverStarted,
					CONFIG.textsEN.serverStopped,
					CONFIG.textsEN.joinServer,
					CONFIG.textsEN.leftServer,
					CONFIG.textsEN.deathMessage,
					CONFIG.textsEN.advancementTask,
					CONFIG.textsEN.advancementChallenge,
					CONFIG.textsEN.advancementGoal,
					CONFIG.textsEN.highMspt,
					CONFIG.textsEN.offlineChannelTopic,
					CONFIG.textsEN.onlineChannelTopic,
					CONFIG.textsEN.onlineChannelTopicForMultiServer);
		} else {
			TEXTS = new Texts(CONFIG.textsZH.unformattedResponseMessage,
					CONFIG.textsZH.unformattedChatMessage,
					CONFIG.textsZH.unformattedOtherMessage,
					CONFIG.textsZH.unformattedCommandNotice,
					new Gson().toJson(CONFIG.textsZH.formattedResponseMessage),
					new Gson().toJson(CONFIG.textsZH.formattedChatMessage),
					new Gson().toJson(CONFIG.textsZH.formattedOtherMessage),
					new Gson().toJson(CONFIG.textsZH.formattedCommandNotice),
					CONFIG.textsZH.serverStarted,
					CONFIG.textsZH.serverStopped,
					CONFIG.textsZH.joinServer,
					CONFIG.textsZH.leftServer,
					CONFIG.textsZH.deathMessage,
					CONFIG.textsZH.advancementTask,
					CONFIG.textsZH.advancementChallenge,
					CONFIG.textsZH.advancementGoal,
					CONFIG.textsZH.highMspt,
					CONFIG.textsZH.offlineChannelTopic,
					CONFIG.textsZH.onlineChannelTopic,
					CONFIG.textsZH.onlineChannelTopicForMultiServer);
		}
	}

	public static void setBotActivity() {
		if (!CONFIG.generic.botPlayingStatus.isEmpty()) {
			JDA.getPresence().setActivity(Activity.playing(CONFIG.generic.botPlayingStatus));
		} else if (!CONFIG.generic.botListeningStatus.isEmpty()) {
			JDA.getPresence().setActivity(Activity.listening(CONFIG.generic.botListeningStatus));
		} else {
			JDA.getPresence().setActivity(null);
		}
	}

	public static void updateBotCommands() {
		JDA.updateCommands()
				.addCommands(Commands.slash("info", CONFIG.generic.useEngInsteadOfChin ? "Query server running status" : "查询服务器运行状态"))
				.addCommands(Commands.slash("help", CONFIG.generic.useEngInsteadOfChin ? "Get a list of available commands" : "获取可用命令列表"))
				.addCommands(Commands.slash("update", CONFIG.generic.useEngInsteadOfChin ? "Check for update" : "检查更新"))
				.addCommands(Commands.slash("stats", CONFIG.generic.useEngInsteadOfChin ? "Query the scoreboard for a statistic" : "查询该统计信息的排行榜")
						.addOption(OptionType.STRING, "type", CONFIG.generic.useEngInsteadOfChin ? "Statistic type" : "统计类型", true)
						.addOption(OptionType.STRING, "name", CONFIG.generic.useEngInsteadOfChin ? "Statistic name" : "统计名称", true))
				.addCommands(Commands.slash("reload", CONFIG.generic.useEngInsteadOfChin ? "Reload MCDiscordChat config file (admin only)" : "重新加载 MCDiscordChat 配置文件（仅限管理员）"))
				.addCommands(Commands.slash("console", CONFIG.generic.useEngInsteadOfChin ? "Execute a command in the server console (admin only)" : "在服务器控制台中执行指令（仅限管理员）")
						.addOption(OptionType.STRING, "command", CONFIG.generic.useEngInsteadOfChin ? "Command to execute" : "要执行的命令", true))
				.addCommands(Commands.slash("log", CONFIG.generic.useEngInsteadOfChin ? "Get the specified server log (admin only)" : "获取指定的服务器日志（仅限管理员）")
						.addOption(OptionType.STRING, "file", CONFIG.generic.useEngInsteadOfChin ? "File name" : "文件名", true, true))
				.addCommands(Commands.slash("stop", CONFIG.generic.useEngInsteadOfChin ? "Stop the server (admin only)" : "停止服务器（仅限管理员）"))
				.queue();
	}

	public static void initMsptMonitor() {
		MSPT_MONITOR_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				double mspt = MathHelper.average(SERVER.lastTickLengths) * 1.0E-6D;

				if (mspt > CONFIG.generic.msptLimit) {
					CHANNEL.sendMessage(TEXTS.highMspt()
							.replace("%mspt%", String.format("%.2f", mspt))
							.replace("%msptLimit%", Integer.toString(CONFIG.generic.msptLimit))).queue();
					if (CONFIG.multiServer.enable) {
						MULTI_SERVER.sendMessage(false, false, false, null, MarkdownParser.parseMarkdown(TEXTS.highMspt()
								.replace("%mspt%", String.format("%.2f", mspt))
								.replace("%msptLimit%", Integer.toString(CONFIG.generic.msptLimit))));
					}
				}
			}
		}, 0, CONFIG.generic.msptCheckInterval);
	}

	public static void initChannelTopicMonitor() {
		CHANNEL_TOPIC_MONITOR_TIMER.schedule(new TimerTask() {
			@Override
			public void run() {
				String topic = TEXTS.onlineChannelTopic()
						.replace("%onlinePlayerCount%", Integer.toString(SERVER.getPlayerManager().getPlayerList().size()))
						.replace("%maxPlayerCount%", Integer.toString(SERVER.getPlayerManager().getMaxPlayerCount()))
						//#if MC >= 11600
						.replace("%uniquePlayerCount%", Integer.toString(FileUtils.listFiles(new File((SERVER.getSaveProperties().getLevelName() + "/stats/")), null, false).size()))
						//#else
						//$$ .replace("%uniquePlayerCount%", Integer.toString(FileUtils.listFiles(new File((SERVER.getLevelName() + "/stats/")), null, false).size()))
						//#endif
						.replace("%serverStartedTime%", SERVER_STARTED_TIME)
						.replace("%lastUpdateTime%", Long.toString(Instant.now().getEpochSecond()));

				CHANNEL.getManager().setTopic(topic).queue();

				if (!CONFIG.generic.consoleLogChannelId.isEmpty()) {
					CONSOLE_LOG_CHANNEL.getManager().setTopic(topic).queue();
				}
			}
		}, 0, CONFIG.generic.channelTopicUpdateInterval);
	}

	public static void initCheckUpdateTimer() {

	}

	public static void testJsonValid() throws JsonSyntaxException {
		new Gson().fromJson(CONFIG.textsZH.formattedResponseMessage, Object.class);
		new Gson().fromJson(CONFIG.textsZH.formattedChatMessage, Object.class);
		new Gson().fromJson(CONFIG.textsZH.formattedOtherMessage, Object.class);

		new Gson().fromJson(CONFIG.textsEN.formattedResponseMessage, Object.class);
		new Gson().fromJson(CONFIG.textsEN.formattedChatMessage, Object.class);
		new Gson().fromJson(CONFIG.textsEN.formattedOtherMessage, Object.class);
	}
}
