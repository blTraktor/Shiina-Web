package dev.osunolimits.routes.ap.post;

import java.util.ArrayList;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.GroupRegistry;
import dev.osunolimits.modules.utils.GroupRegistry.Group;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class ProcessManageGroup extends Shiina {

    private Gson gson;

    public ProcessManageGroup() {
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

        String emoji = req.queryParams("emoji");
        String name = req.queryParams("name");
        String desc = req.queryParams("desc");
        if(req.queryParams("id") != null) {
            String groupQuery = "UPDATE `sh_groups` SET `name` = ?, `emoji` = ?, `desc` = ? WHERE `id` = ?";
            shiina.mysql.Exec(groupQuery, name, emoji, desc, req.queryParams("id"));
            ArrayList<Group> groups = new GroupRegistry().getCurrentGroupRegistry();

            for(Group g : groups) {
                if(g.id == Integer.parseInt(req.queryParams("id"))) {
                    for (Integer uid : g.getUserIds()) {
                    UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + uid), UserInfoObject.class);
                    for (dev.osunolimits.models.Group gr : userInfo.groups) {
                        if(g.id == gr.id) {
                            gr.emoji = emoji;
                            gr.name = name;
                        }
                    }
                    App.appCache.set("shiina:user:" + uid, gson.toJson(userInfo));
                    }
                }
            }
        }else {
            String groupQuery = "INSERT INTO `sh_groups` (`name`, `emoji`, `desc`) VALUES (?, ?, ?)";
            shiina.mysql.Exec(groupQuery, name, emoji, desc);
            new GroupRegistry().revalidate(shiina.mysql);
        }

        return redirect(res, shiina, "/ap/groups");
    }
    
}
