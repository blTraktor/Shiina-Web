package dev.osunolimits.routes.post.settings.data;

import dev.osunolimits.main.App;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.DataExporter;
import spark.Request;
import spark.Response;

public class HandleDataRequest extends Shiina {
    @Override
    public Object handle(Request req, Response res) throws Exception {
       ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if (!shiina.loggedIn) {
            return redirect(res, shiina, "/login?path=/settings/data");
        }
        String cooldownKey = "shiina:user:" + shiina.user.id + ":data_cooldown";
        String timestamp = App.appCache.get(cooldownKey);

        if(timestamp != null && System.currentTimeMillis() < Long.parseLong(timestamp)) {
            return redirect(res, shiina, "/settings/data?error=You can download your data again in &timestamp=" + timestamp);
        }

        App.appCache.set(cooldownKey, String.valueOf(System.currentTimeMillis() + 3600000), 3600);
        DataExporter exporter = new DataExporter(shiina.user.id);
        byte[] data = exporter.exportData();
        res.type("application/zip");
        res.header("Content-Disposition", "attachment; filename=\"data_export_" + shiina.user.id + ".zip\"");
        res.header("Content-Length", String.valueOf(data.length));

        return data != null ? data : redirect(res, shiina, "/settings/data?error=Failed to export data");
    }
}
