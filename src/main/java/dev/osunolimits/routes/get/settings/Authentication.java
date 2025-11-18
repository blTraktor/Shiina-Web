package dev.osunolimits.routes.get.settings;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.SEOBuilder;
import dev.osunolimits.plugins.NavbarRegister;
import dev.osunolimits.utils.StringCipher;
import dev.osunolimits.utils.Auth.SessionUser;
import lombok.Data;
import spark.Request;
import spark.Response;
import ua_parser.Client;
import ua_parser.Parser;

public class Authentication extends Shiina {

    private static final Gson gson = new Gson();
    private static final Parser userAgentParser = new Parser();

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 101);

        if (!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if (req.queryParams("info") != null) {
            shiina.data.put("info", req.queryParams("info"));
        }

        if (req.queryParams("error") != null) {
            shiina.data.put("error", req.queryParams("error"));
        }

        List<Object> sessionTokens = App.appCache.lrange("shiina:user:" + shiina.user.id + ":tokens", 0, -1);
        List<WebSession> webSessions = new ArrayList<>();
        for (Object token : sessionTokens) {

            String userJson = App.appCache.get("shiina:auth:" + token);

            SessionUser user = gson.fromJson(userJson, SessionUser.class);

            if (user == null) {
                continue;
            }

            WebSession webSession = new WebSession();
            webSession.setCreated(user.getCreated());
            webSession.setCity(user.getCity());
            webSession.setCountry(user.getCountry());
            StringCipher cipher = new StringCipher(App.appSecret);
            webSession.setEncToken(cipher.encode((String) token));
            Client c = userAgentParser.parse(user.getUserAgent());
            if (c != null) {
                webSession.setBrowser(c.userAgent.family);
                webSession.setOs(c.os.family);
                webSession.setDevice(c.device.family);
            }
            webSessions.add(webSession);
        }

        shiina.data.put("webSessions", webSessions);

        shiina.data.put("pluginNav", NavbarRegister.getSettingsItems());
        shiina.data.put("seo", new SEOBuilder("Settings | Auth", App.customization.get("homeDescription").toString()));
        return renderTemplate("settings/auth.html", shiina, res, req);
    }

    @Data
    public class WebSession {
        private String encToken;
        private long created;
        private String browser;
        private String os;
        private String device;

        private String city;
        private String country;
    }

}
