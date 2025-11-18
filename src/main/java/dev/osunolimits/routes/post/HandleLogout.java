package dev.osunolimits.routes.post;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import spark.Request;
import spark.Response;

public class HandleLogout extends Shiina {


    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 0);

        if(req.cookie("shiina") != null && App.appCache.get("shiina:auth:" + req.cookie("shiina")) != null) {
            App.appCache.del("shiina:auth:" + req.cookie("shiina"));
            shiina.loggedIn = false;
            shiina.data.remove("user");
            res.removeCookie("shiina");
        }
    
        shiina.data.put("info", "You have been logged out");
        return renderTemplate("login.html", shiina, res, req);
    }

    
}
