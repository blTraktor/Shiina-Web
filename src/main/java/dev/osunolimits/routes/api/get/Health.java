package dev.osunolimits.routes.api.get;

import java.util.List;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.MySQLRoute;
import dev.osunolimits.routes.api.get.ShiinaAPIHandler.ShiinaAPIParameter;
import lombok.Data;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import spark.Request;
import spark.Response;

public class Health extends MySQLRoute {
    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = getRequest();
        ShiinaAPIHandler shiinaAPIHandler = new ShiinaAPIHandler();

        try {
            shiina.mysql.Query("SELECT 1;");
        } catch (Exception e) {
            shiinaAPIHandler.addRequiredParameter("mysql", "service", "error");
        }

        boolean hasIssues = shiinaAPIHandler.hasIssues();
        HealthReponse healthReponse = new HealthReponse();
        healthReponse.setStatus(hasIssues ? "error" : "ok");
        healthReponse.setIssues(hasIssues ? shiinaAPIHandler.getIssues() : null);
        healthReponse.setMessage(hasIssues ? "There are issues with the services" : "All services are operational");
        return shiinaAPIHandler.renderJSON(healthReponse, shiina, res);
    }

    public static boolean keyWithPrefixExists(JedisPooled jedis, String prefix) {
        String cursor = "0";
        ScanParams scanParams = new ScanParams().match(prefix + "*").count(1);

        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            cursor = scanResult.getCursor();

            if (!scanResult.getResult().isEmpty()) {
                return true;
            }
        } while (!cursor.equals("0"));

        return false;
    }

    @Data 
    public class HealthReponse {
        private String status;
        private List<ShiinaAPIParameter> issues;
        private String message;
        private String shiina = "v" + App.version;
    }
}
