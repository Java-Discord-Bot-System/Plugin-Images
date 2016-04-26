package com.almightyalpaca.discord.bot.plugin.images;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import com.almightyalpaca.discord.bot.system.command.Command;
import com.almightyalpaca.discord.bot.system.command.CommandHandler;
import com.almightyalpaca.discord.bot.system.command.arguments.special.Rest;
import com.almightyalpaca.discord.bot.system.config.Config;
import com.almightyalpaca.discord.bot.system.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.bot.system.config.exception.WrongTypeException;
import com.almightyalpaca.discord.bot.system.events.commands.CommandEvent;
import com.almightyalpaca.discord.bot.system.exception.PluginLoadingException;
import com.almightyalpaca.discord.bot.system.exception.PluginUnloadingException;
import com.almightyalpaca.discord.bot.system.plugins.Plugin;
import com.almightyalpaca.discord.bot.system.plugins.PluginInfo;
import com.almightyalpaca.discord.bot.system.util.URLUtils;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageBuilder.Formatting;

public class ImagesPlugin extends Plugin {

	class CatCommand extends Command {

		public CatCommand() {
			super("cat", "I want cats!", "");
		}

		@CommandHandler(dm = true, guild = true, async = true)
		private void onCommand(final CommandEvent event) {
			try {
				event.sendMessage(URLUtils.expandSingleLevel("http://thecatapi.com/api/images/get?api_key=" + ImagesPlugin.this.thecatapiConfig.getString("token")));
			} catch (JsonIOException | JsonSyntaxException | WrongTypeException | KeyNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	class GifCommand extends Command {

		public GifCommand() {
			super("gif", "Show me gifs!", "");
		}

		@CommandHandler(dm = true, guild = true)
		private void onCommand(final CommandEvent event, final Rest tags) {
			try {
				final String query = URLEncoder.encode(tags.getString(), "UTF-8");

				final JSONObject response = Unirest.get("http://api.giphy.com/v1/gifs/search?q=" + query + "&limit=1&api_key=" + ImagesPlugin.this.giphyConfig.getString("token")).asJson().getBody()
					.getObject();

				final JSONArray data = response.getJSONArray("data");

				final int status = response.getJSONObject("meta").getInt("status");

				if (status != 200) {
					final String message = response.getJSONObject("meta").getString("msg");
					event.sendMessage("Something went wrong while searching for GIFs. Giphy returned status code " + status + " with message\"" + message + "\".");
					return;
				}

				final int total = response.getJSONObject("pagination").getInt("total_count");

				if (total == 0) {
					event.sendMessage("No GIFs found.");
					return;
				}

				final MessageBuilder builder = new MessageBuilder();

				final JSONObject first = data.getJSONObject(new Random().nextInt(data.length()));
				final String url = first.getJSONObject("images").getJSONObject("original").getString("url");
				builder.appendString("Found ").appendString(Integer.toString(total), Formatting.BOLD).appendString(" GIFs. ");

				final String username = first.getString("username");

				if (!username.contentEquals("")) {
					builder.appendString("First GIF is from ").appendString(username, Formatting.BOLD).appendString(": ");
				} else {
					builder.appendString("First GIF: ");
				}
				builder.appendString(url);

				event.sendMessage(builder);

			} catch (UnirestException | JsonIOException | JsonSyntaxException | WrongTypeException | KeyNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static final PluginInfo INFO = new PluginInfo("com.almightyalpaca.discord.bot.plugin.images", "1.0.0", "Almighty Alpaca", "Image Plugin", "I want pics!");

	protected Config giphyConfig;

	protected Config thecatapiConfig;

	public ImagesPlugin() {
		super(ImagesPlugin.INFO);
	}

	@Override
	public void load() throws PluginLoadingException {
		this.giphyConfig = this.getSharedConfig("giphy");
		this.thecatapiConfig = this.getSharedConfig("thecatapi");

		boolean tokenMissing = false;
		if (this.giphyConfig.getString("token", "Your token") == "Your token") {
			tokenMissing = true;
		}
		if (this.thecatapiConfig.getString("token", "Your token") == "Your token") {
			tokenMissing = true;
		}

		if (tokenMissing) {
			throw new PluginLoadingException("Pls add your giphy and thecatapi tokens to the config");
		}

		this.registerCommand(new GifCommand());
		this.registerCommand(new CatCommand());
	}

	@Override
	public void unload() throws PluginUnloadingException {}
}
