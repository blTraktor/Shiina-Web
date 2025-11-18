package dev.osunolimits.routes.ap.get.groups;


import java.sql.ResultSet;
import java.util.ArrayList;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.models.Group;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.GroupRegistry;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class ProcessGroup extends Shiina {

    private Gson gson;

    public ProcessGroup() {
        gson = new Gson();
    }


    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);

        if(!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if(!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.ADMINISTRATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        String id = req.queryParams("id");
        if(id == null) {
            return notFound(res, shiina);
        }

        String groupId = req.queryParams("group");
        if(groupId == null) {
            return notFound(res, shiina);
        }

        String action = req.queryParams("action");
        if(action == null) {
            return notFound(res, shiina);
        }

        UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + id), UserInfoObject.class);
        if(userInfo == null) {
            return notFound(res, shiina);
        }

        if(action.equals("add")) {
            ResultSet groupResult = shiina.mysql.Query("SELECT * FROM `sh_groups` WHERE `id` = ?", groupId);
            if(!groupResult.next()) {
                return notFound(res, shiina);
            }
            Group g = new Group();
            g.id = groupResult.getInt("id");
            g.name = groupResult.getString("name");
            g.emoji = groupResult.getString("emoji");
            userInfo.groups.add(g);
            App.appCache.set("shiina:user:" + id, gson.toJson(userInfo));
            GroupRegistry groupRegistry = new GroupRegistry();
            ArrayList<dev.osunolimits.modules.utils.GroupRegistry.Group> groups = groupRegistry.getCurrentGroupRegistry();
            boolean foundMatch = false;
            for(int i = 0; i < groups.size(); i++) {
                if(groups.get(i).id == Integer.parseInt(groupId)) {
                    groups.get(i).userIds.add(Integer.parseInt(id));
                    foundMatch = true;
                    break;
                }
            }
            if(!foundMatch) {
                dev.osunolimits.modules.utils.GroupRegistry.Group group = groupRegistry.new Group();
                group.id = Integer.parseInt(groupId);
                group.userIds = new ArrayList<>();
                group.userIds.add(Integer.parseInt(id));
                groups.add(group);
            }

            App.appCache.set("shiina:groupRegistry", gson.toJson(groups));

            shiina.mysql.Exec("INSERT INTO `sh_groups_users` (`user_id`, `group_id`) VALUES (?, ?);", id, groupId);
        }else if(action.equals("remove")) {

            for (Group g : userInfo.groups) {
                if(g.id == Integer.parseInt(groupId)) {
                    userInfo.groups.remove(g);
                    break;
                }
                
            }
            App.appCache.set("shiina:user:" + id, gson.toJson(userInfo));

            GroupRegistry groupRegistry = new GroupRegistry();
            ArrayList<dev.osunolimits.modules.utils.GroupRegistry.Group> groups = groupRegistry.getCurrentGroupRegistry();
            for(int i = 0; i < groups.size(); i++) {
                if(groups.get(i).id == Integer.parseInt(groupId)) {
                    groups.get(i).userIds.remove((Object)Integer.parseInt(id));
                    break;
                }
            }

            App.appCache.set("shiina:groupRegistry", gson.toJson(groups));

            shiina.mysql.Exec("DELETE FROM `sh_groups_users` WHERE `user_id` = ? AND `group_id` = ?;", id, groupId);
        }else {
            return notFound(res, shiina);
        }
        
        return redirect(res, shiina, "/ap/user?id=" + id);
    }
    
}
