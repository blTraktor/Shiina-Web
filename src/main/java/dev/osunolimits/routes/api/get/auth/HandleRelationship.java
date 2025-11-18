package dev.osunolimits.routes.api.get.auth;

import java.sql.ResultSet;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.Validation;
import spark.Request;
import spark.Response;

public class HandleRelationship extends Shiina {

    private final String SEARCH_QUERY = "SELECT * FROM `relationships` WHERE `user1` = ? AND `user2` = ?";
    private final String DEL_QUERY = "DELETE FROM `relationships` WHERE `user1` = ? AND `user2` = ?";
    private final String INSERT_QUERY = "INSERT INTO `relationships` (`user1`, `user2`) VALUES (?, ?)";

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if(shiina.user == null) {
            return notFound(res, shiina);
        }

        int user1 = shiina.user.id;
        int user2 = 0;
        if (req.queryParams("u") != null && Validation.isNumeric(req.queryParams("u"))) {
            user2 = Integer.parseInt(req.queryParams("u"));
        }

        if(App.appCache.get("shiina:user:" + user2) == null) {
            shiina.mysql.close();
            return "invalid user";
        }
        
        ResultSet searchResult = shiina.mysql.Query(SEARCH_QUERY, user1, user2);
        if(searchResult.next()) {
            shiina.mysql.Exec(DEL_QUERY, user1, user2);
        }else {
            shiina.mysql.Exec(INSERT_QUERY, user1, user2);
        }
        res.status(200);
        shiina.mysql.close();
        return "";
    }
}
