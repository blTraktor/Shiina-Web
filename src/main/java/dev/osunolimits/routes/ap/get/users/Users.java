package dev.osunolimits.routes.ap.get.users;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.models.Group;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.utils.osu.PermissionHelper;
import dev.osunolimits.utils.osu.PermissionHelper.Privileges;
import lombok.Data;
import spark.Request;
import spark.Response;

public class Users extends Shiina {

    private Gson gson;

    public Users() {
        gson = new Gson();
    }

    @Data
    public class ApUser {
        public int id;
        public String name;
        public EnumSet<Privileges> priv;
        public List<Group> groups;
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 14);

        if (!shiina.loggedIn) {
            res.redirect("/login");
            return notFound(res, shiina);
        }

        if (!PermissionHelper.hasPrivileges(shiina.user.priv, PermissionHelper.Privileges.MODERATOR)) {
            res.redirect("/");
            return notFound(res, shiina);
        }

        int page = 0;
        if (req.queryParams("page") != null) {
            page = Integer.parseInt(req.queryParams("page"));
        }

        int offset = page * 10;
        String searchQuery = req.queryParams("search");
        List<Object> params = new ArrayList<>();

        String sql = "SELECT `id`, `name`, `priv` FROM `users`";
        if (searchQuery != null && !searchQuery.isEmpty()) {
            if (searchQuery.matches("\\d+")) { // Check if searchQuery is numeric
                sql += " WHERE `id` = ?";
                params.add(Integer.parseInt(searchQuery));
            } else {
                sql += " WHERE `name` LIKE ?";
                params.add("%" + searchQuery + "%");
            }
        }
        sql += " LIMIT ? OFFSET ?";
        params.add(11);
        params.add(offset);

        ResultSet userResult = shiina.mysql.Query(sql, params.toArray());

        List<ApUser> users = new ArrayList<>();
        boolean hasNextPage = false;
        while (userResult.next()) {
            if (users.size() >= 10) {
                hasNextPage = true;
                break;
            }
            ApUser user = new ApUser();
            user.id = userResult.getInt("id");
            UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + user.id), UserInfoObject.class);
            user.name = userInfo.name;
            user.priv = Privileges.fromInt(userInfo.priv);
            user.groups = userInfo.getGroups();
            users.add(user);
        }

        shiina.data.put("users", users);
        shiina.data.put("hasNextPage", hasNextPage);
        shiina.data.put("page", page);
        shiina.data.put("search", searchQuery);

        return renderTemplate("ap/users/users.html", shiina, res, req);
    }


}
