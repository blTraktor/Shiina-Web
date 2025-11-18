package dev.osunolimits.modules.utils;

import java.util.List;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.queries.GeoLocQuery;
import dev.osunolimits.modules.queries.GeoLocQuery.GeoLocResponse;
import dev.osunolimits.utils.Auth;
import dev.osunolimits.utils.Auth.SessionUser;
import lombok.AllArgsConstructor;
import spark.Request;

@AllArgsConstructor
public class SessionBuilder {
    private static final Gson gson = new Gson();
    private static final GeoLocQuery geoLocQuery = new GeoLocQuery();

    private int id;
    private Request request;

    public String build() {
        String authToken = Auth.generateNewToken();

        SessionUser user = new SessionUser();
        user.id = id;
        user.created = (int) (System.currentTimeMillis() / 1000L);
        user.ip = request.ip();
        user.userAgent = request.userAgent() != null ? request.userAgent() : "Unknown";

        GeoLocResponse geoLocResponse = geoLocQuery.getCountryCodeSession(request.ip());
        if(geoLocResponse != null) {
            user.country = geoLocResponse.getCountry();
            user.city = geoLocResponse.getCity();
        } 

        App.appCache.set("shiina:auth:" + authToken, gson.toJson(user), 604800);
        App.appCache.lpush("shiina:user:" + id + ":tokens", authToken);
        return authToken;
    }

    public static void deleteAllSessions(int userId) {
        List<Object> sessionTokens = App.appCache.lrange("shiina:user:" + userId + ":tokens", 0, -1);
        
        for (Object token : sessionTokens) {
            App.appCache.del("shiina:auth:" + (String) token);
        }
        
        App.appCache.del("shiina:user:" + userId + ":tokens");
    }
}
