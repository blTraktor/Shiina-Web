package dev.osunolimits.routes.get.redirect;

import java.sql.ResultSet;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import spark.Request;
import spark.Response;

public class BeatmapSetRedirect extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        String beatmapId = req.params("id");
        if (beatmapId == null) {
            return notFound(res, shiina);
        }
            

        ResultSet beatmapRs = shiina.mysql.Query("SELECT * FROM `maps` WHERE `set_id` = ? LIMIT 1;", beatmapId);

        if(!beatmapRs.next()) {
            return notFound(res, shiina);
        }
        
        return redirect(res, shiina, "/b/" + beatmapRs.getString("id"));
    }
    
}
