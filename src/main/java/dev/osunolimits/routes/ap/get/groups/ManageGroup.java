package dev.osunolimits.routes.ap.get.groups;


import java.sql.ResultSet;
import java.util.ArrayList;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.models.Action;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.GroupRegistry;
import dev.osunolimits.modules.utils.GroupRegistry.Group;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class ManageGroup extends Shiina {

    private Action action;
    private Gson gson;

    public ManageGroup(Action action) {
        this.action = action;
        gson = new Gson();
    }


    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 13);

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        if(action == Action.CREATE) {
            return renderTemplate("ap/groups/manage.html", shiina, res, req);
        }else if(action == Action.EDIT) {

            ResultSet selectGroup = shiina.mysql.Query("SELECT * FROM `sh_groups` WHERE `id` = ?", req.queryParams("id"));
            while (selectGroup.next()) {
                shiina.data.put("id", req.queryParams("id"));
                shiina.data.put("name", selectGroup.getString("name"));
                shiina.data.put("emoji", selectGroup.getString("emoji"));
                shiina.data.put("desc", selectGroup.getString("desc"));
            }
            return renderTemplate("ap/groups/manage.html", shiina, res, req);
        }else {
            String id = req.queryParams("id");
            if(id == null) {
                return notFound(res, shiina);
            }
            shiina.mysql.Exec("DELETE FROM `sh_groups` WHERE `id` = ?", id);
            shiina.mysql.Exec("DELETE FROM `sh_groups_users` WHERE `group_id` = ?", id);
            ArrayList<Group> groups = new GroupRegistry().getCurrentGroupRegistry();

            for(Group g : groups) {
                if(g.id == Integer.parseInt(req.queryParams("id"))) {
                    for (Integer uid : g.getUserIds()) {
                        App.log.debug("Try Removing group from user: " + uid + " group: " + id);
                        UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + uid), UserInfoObject.class);
                        for(int i = 0; i < userInfo.groups.size(); i++) {
                            if(userInfo.groups.get(i).id == Integer.parseInt(id)) {
                                userInfo.groups.remove(i);
                                App.log.debug("Removed group from user: " + uid + " group: " + id);
                            }
                        }
                        App.appCache.set("shiina:user:" + uid, gson.toJson(userInfo));
                    }
                   
                }
            }

            for(int i = 0; i < groups.size(); i++) {
                if(groups.get(i).id == Integer.parseInt(id)) {
                    groups.remove(i);
                }
            }

            App.appCache.set("shiina:groupRegistry", gson.toJson(groups));
            return redirect(res, shiina, "/ap/groups");
        }

    }
    
}
