package dev.osunolimits.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import dev.osunolimits.common.APIRequest;
import dev.osunolimits.main.App;
import lombok.Data;
import okhttp3.Request;
import okhttp3.Response;

public class BanchoStats extends APIQuery {

    @Data
    public class PlayerCountResponse {
        private int total;
        private int online;
    }

    @Data
    public class CustomCountResponse {
        private int clans;
        private int plays;
        private int beatmaps;
        private int totalLogins;
        private int loginsToday;
        private int staff;
        private int restricted;
    }

    public BanchoStats() {
        super("stats", 5, 20);
    }

    public CustomCountResponse getCustomCount() {
        Request requestMaps = APIRequest.build("/v2/maps");
        Request requestClans = APIRequest.build("/v2/clans");
        Request requestPlays = APIRequest.build("/v2/scores");

        try {
            Response responseMaps = client.newCall(requestMaps).execute();
            Response responseClans = client.newCall(requestClans).execute();
            Response responsePlays = client.newCall(requestPlays).execute();
            JsonElement mapsElement = JsonParser.parseString(responseMaps.body().string());
            JsonElement clansElement = JsonParser.parseString(responseClans.body().string());
            JsonElement playsElement = JsonParser.parseString(responsePlays.body().string());

            CustomCountResponse customCount = new CustomCountResponse();
            customCount
                    .setBeatmaps(mapsElement.getAsJsonObject().get("meta").getAsJsonObject().get("total").getAsInt());
            customCount.setClans(clansElement.getAsJsonObject().get("meta").getAsJsonObject().get("total").getAsInt());
            customCount.setPlays(playsElement.getAsJsonObject().get("meta").getAsJsonObject().get("total").getAsInt());
            return customCount;
        } catch (Exception e) {
            App.log.error("Failed to get custom counts", e);
            return null;
        }
    }

    public PlayerCountResponse getPlayerCount() {
        Request request = APIRequest.build("/v1/get_player_count");

        try {
            Response response = client.newCall(request).execute();
            JsonElement playerCountElement = JsonParser.parseString(response.body().string());
            JsonElement counts = playerCountElement.getAsJsonObject().get("counts");
            PlayerCountResponse playerCount = new PlayerCountResponse();
            playerCount.setTotal(counts.getAsJsonObject().get("total").getAsInt());
            playerCount.setOnline(counts.getAsJsonObject().get("online").getAsInt());
            return playerCount;
        } catch (Exception e) {
            App.log.error("Failed to get /v1/get_player_count", e);
            return null;
        }

    }

}
