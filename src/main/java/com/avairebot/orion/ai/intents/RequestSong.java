package com.avairebot.orion.ai.intents;

import ai.api.model.AIResponse;
import com.avairebot.orion.Orion;
import com.avairebot.orion.cache.CacheType;
import com.avairebot.orion.commands.CommandContainer;
import com.avairebot.orion.commands.CommandHandler;
import com.avairebot.orion.commands.music.PlayCommand;
import com.avairebot.orion.contracts.ai.Intent;
import com.avairebot.orion.factories.MessageFactory;
import com.avairebot.orion.utilities.RandomUtil;
import com.google.gson.JsonElement;
import net.dv8tion.jda.core.entities.Message;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.*;

public class RequestSong extends Intent {

    private static final Map<String, List<String>> CATEGORIES = new HashMap<>();

    static {
        CATEGORIES.put("electro", Arrays.asList("electro-hub", "electro-swing"));
        CATEGORIES.put("classical", Collections.singletonList("classical"));
        CATEGORIES.put("korean", Collections.singletonList("korean-madness"));
        CATEGORIES.put("hiphop", Collections.singletonList("hip-hop"));
        CATEGORIES.put("chill", Collections.singletonList("chill-corner"));
        CATEGORIES.put("metal", Collections.singletonList("metal-mix"));
        CATEGORIES.put("retro", Collections.singletonList("retro-renegade"));
        CATEGORIES.put("rock", Collections.singletonList("rock-n-roll"));
        CATEGORIES.put("jazz", Collections.singletonList("coffee-house-jazz"));
        CATEGORIES.put("pop", Collections.singletonList("purely-pop"));
    }

    public RequestSong(Orion orion) {
        super(orion);
    }

    @Override
    public String getAction() {
        return "request.song";
    }

    @Override
    public void onIntent(Message message, AIResponse response) {
        HashMap<String, JsonElement> parameters = response.getResult().getParameters();

        if (parameters.isEmpty() || !parameters.containsKey("music")) {
            MessageFactory.makeWarning(message, "Invalid or unsupported music category, more info coming here soon...").queue();
            return;
        }

        CommandContainer container = CommandHandler.getCommand(PlayCommand.class);
        if (container == null) {
            return;
        }

        String type = parameters.get("music").getAsString().toLowerCase();
        if (!CATEGORIES.containsKey(type)) {
            container.getCommand().onCommand(message, new String[]{type, "---leave-message"});
            return;
        }

        String category = (String) RandomUtil.pickRandom(CATEGORIES.get(type));
        String randomSong = getRandomSong(category);

        container.getCommand().onCommand(message, new String[]{randomSong == null ? type : randomSong, "---leave-message"});
    }

    private String getRandomSong(final String category) {
        Object cacheItem = orion.getCache().getAdapter(CacheType.FILE).remember("music-type." + category.toLowerCase(), 31536000, () -> {
            try {
                String item = Jsoup.connect(
                    String.format("https://libraries.amped.fm/libraries/%s/musicbot", category)
                ).execute().body();

                String[] items = item.split("\n");

                return Arrays.asList(Arrays.copyOfRange(items, 2, items.length));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });

        if (!(cacheItem instanceof ArrayList)) {
            return null;
        }

        return (String) RandomUtil.pickRandom((List<String>) cacheItem);
    }
}
