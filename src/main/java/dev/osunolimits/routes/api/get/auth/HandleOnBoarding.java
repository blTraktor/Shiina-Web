package dev.osunolimits.routes.api.get.auth;

import java.sql.ResultSet;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.UserInfoCache;
import dev.osunolimits.routes.api.get.ShiinaAPIHandler;
import lombok.Data;
import spark.Request;
import spark.Response;

public class HandleOnBoarding extends Shiina {


    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if (shiina.user == null) {
            return redirect(res, shiina, "/login");
        }

        ShiinaAPIHandler shiinaAPIHandler = new ShiinaAPIHandler();

        ResultSet dbPrivData = shiina.mysql.Query("SELECT priv FROM users WHERE id = ?", shiina.user.id);
        OnBoardingData data = new OnBoardingData();
        if(dbPrivData.next()) {
            data.setPriv(dbPrivData.getInt("priv"));
            data.setId(shiina.user.id);

            UserInfoCache userInfoCache = new UserInfoCache();
            userInfoCache.reloadUser(shiina.user.id);
        }

        return shiinaAPIHandler.renderJSON(data, shiina, res);
    }

    @Data
    public class OnBoardingData {
        private int priv = 0;
        private int id = 0;
    }
}
