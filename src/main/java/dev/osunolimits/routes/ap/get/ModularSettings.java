package dev.osunolimits.routes.ap.get;

import java.util.ArrayList;
import java.util.HashMap;

import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.XmlConfig;
import dev.osunolimits.modules.utils.ThemeLoader;
import dev.osunolimits.utils.osu.PermissionHelper;
import lombok.Data;
import spark.Request;
import spark.Response;

public class ModularSettings extends Shiina {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 18);

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        HashMap<String, Object> xmlSettings = XmlConfig.getInstance().getAll();
        HashMap<String, ArrayList<ModularSetting>> groupedSettings = new HashMap<>();

        for (String key : xmlSettings.keySet()) {
            ModularSetting setting = new ModularSetting();
            setting.setKey(key);
            setting.setValue(xmlSettings.get(key).toString());
            setting.setSecret(XmlConfig.secretConfigKeys.contains(key));

            if(setting.getValue().equals("true") || setting.getValue().equals("false")) {
                setting.setType("boolean");
            } else if(setting.getValue().matches("\\d+")) {
                setting.setType("number");
            } else {
                setting.setType("text");
            }

            // Extract group name from key (first part before the dot)
            String groupName = key.contains(".") ? key.split("\\.")[0] : "general";
            
            if(!groupedSettings.containsKey(groupName)) {
                groupedSettings.put(groupName, new ArrayList<>());
            }
            
            groupedSettings.get(groupName).add(setting);
        }

        shiina.data.put("groupedSettings", groupedSettings);
        shiina.data.put("themes", ThemeLoader.themes);

        if(req.queryParams("state") != null && req.queryParams("message") != null) {
            shiina.data.put("state", req.queryParams("state"));
            shiina.data.put("message", req.queryParams("message"));
        }

        return renderTemplate("ap/settings.html", shiina, res, req);
    }

    @Data
    public class ModularSetting {
        private String type;
        private String key;
        private String value;
        private boolean secret;
    }
    
}
