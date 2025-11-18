package dev.osunolimits.routes.post.settings.auth;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.Auth;
import dev.osunolimits.utils.Auth.SessionUser;
import dev.osunolimits.utils.StringCipher;
import spark.Request;
import spark.Response;

public class HandleTokenDeletion extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if (!shiina.loggedIn) {
            return redirect(res, shiina, "/login?path=auth");
        }

        String token = req.queryParams("token");
        if (token == null || token.isEmpty()) {
            return redirect(res, shiina, "/settings/auth?error=Invalid token");
        }

        StringCipher cipher = new StringCipher(App.appSecret);
        token = cipher.decode(token);

        String userJson = App.appCache.get("shiina:auth:" + token);

        if (userJson == null || userJson.isEmpty()) {
            return redirect(res, shiina, "/settings/auth?error=Invalid token");
        }

        App.log.info(userJson);

        try {
            SessionUser user = new Gson().fromJson(userJson, Auth.SessionUser.class);

            if (user == null) {
                return redirect(res, shiina, "/settings/auth?error=Invalid token");
            }
            
            if (user.getId() != shiina.user.id) {
                return redirect(res, shiina, "/settings/auth?error=Invalid token");
            }

            App.appCache.del("shiina:auth:" + token);
            return redirect(res, shiina, "/settings/auth?info=Token was deleted");

        } catch (JsonSyntaxException e) {
            App.log.error("Failed to parse user JSON: " + userJson, e);
            return redirect(res, shiina, "/settings/auth?error=Invalid token format");
        }
    }
}