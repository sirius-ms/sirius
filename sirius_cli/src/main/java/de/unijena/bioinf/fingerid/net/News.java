package de.unijena.bioinf.fingerid.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marcus Ludwig on 25.01.17.
 */
public class News {
    final static public String PROPERTY_KEY = "de.unijena.bioinf.sirius.news";

    public final String id;
    public final String date;
    public final String message;

    public News(String id, String date, String message) {
        this.id = id;
        this.date = date;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getDateString() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public static List<News> parseJsonNews(String json){
        List<News> newsList = new ArrayList<>();

        JsonArray array = new JsonParser().parse(json).getAsJsonArray();
        for (JsonElement jsonElement : array) {
            final JsonObject obj = jsonElement.getAsJsonObject();
            final String id = obj.get("id").getAsString();
            final String message = obj.get("message").getAsString();
            final String begin = obj.get("date").getAsString();
            newsList.add(new News(id, begin, message));
        }
        return newsList;
    }
}
