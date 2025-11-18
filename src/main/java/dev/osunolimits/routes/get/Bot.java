package dev.osunolimits.routes.get;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.SEOBuilder;
import spark.Request;
import spark.Response;

public class Bot extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 0);

        UserInfoObject userInfo = new Gson().fromJson(App.appCache.get("shiina:user:" + 1), UserInfoObject.class);

        shiina.data.put("u", userInfo);
        SEOBuilder seo = new SEOBuilder("Profile of " + userInfo.getName(), App.customization.get("homeDescription").toString(), App.env.get("AVATARSRV") + "/" + "1");
        shiina.data.put("seo", seo);
        return renderTemplate("bot.html", shiina, res, req);
    }
    
}
