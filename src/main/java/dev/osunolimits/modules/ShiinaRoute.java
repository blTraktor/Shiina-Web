package dev.osunolimits.modules;

import java.util.HashMap;

import com.google.gson.Gson;

import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.utils.ThemeLoader;
import dev.osunolimits.utils.Auth;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class ShiinaRoute {

    private Gson gson;

    public ShiinaRoute() {
        gson = new Gson();
    }

    public class ShiinaRequest {
        public MySQL mysql;
        public HashMap<String, Object> data = new HashMap<>();
        public boolean loggedIn = false;
        public Auth.User user;
    }

    public ShiinaRequest handle(Request req, Response res) throws Exception {
        ShiinaRequest request = new ShiinaRequest();
        request.mysql = Database.getConnection();
        if (req.cookie("shiina") != null) {
            String userJson = App.appCache.get("shiina:auth:" + req.cookie("shiina"));

            Auth.SessionUser user = gson.fromJson(userJson, Auth.SessionUser.class);

            if (user != null) {
                Auth.User referenceUser = new Auth().new User();
                String userInfoJson = App.appCache.get("shiina:user:" + user.id);
                UserInfoObject infoObject = gson.fromJson(userInfoJson, UserInfoObject.class);
                referenceUser.id = user.id;
                referenceUser.name = infoObject.name;
                referenceUser.priv = infoObject.priv;
                referenceUser.safe_name = infoObject.safe_name;

                request.loggedIn = true;
                request.user = referenceUser;
                request.data.put("user", referenceUser);
                request.data.put("userPriv", PermissionHelper.Privileges.fromInt(referenceUser.priv));
            }
        }
        request.data.put("currentTheme", ThemeLoader.currentTheme);
        request.data.put("assetsUrl", App.env.get("ASSETSURL"));
        request.data.put("apiUrlPub", App.env.get("APIURLPUBLIC"));
        request.data.put("apiUrl", App.env.get("APIURL"));
        request.data.put("c", App.customization);
        request.data.put("turnstilePublic", App.env.get("TURNSTILE_KEY"));
        request.data.put("avatarServer", App.env.get("AVATARSRV"));
        request.data.put("loggedIn", request.loggedIn);
        return request;
    }

}
