package dev.osunolimits.modules.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.main.App;
import lombok.Data;

public class GroupRegistry {
    private Gson gson;

    public GroupRegistry() {
        gson = new Gson();
    }

    @Data
    public class Group {
        public int id;
        public ArrayList<Integer> userIds;
    }

    public static void revalidate() {
        MySQL mysql = Database.getConnection();
        new GroupRegistry().revalidate(mysql);
        mysql.close();
    }

    public ArrayList<Group> getCurrentGroupRegistry() {
        String groupRegistry = App.appCache.get("shiina:groupRegistry");
        if (groupRegistry == null) {
            return new ArrayList<Group>();
        }

        return gson.fromJson(groupRegistry, new TypeToken<ArrayList<GroupRegistry.Group>>(){}.getType());
    }

    public void revalidate(MySQL mysql) {
        ArrayList<Group> groups = new ArrayList<>();

        ResultSet groupUserRs = mysql.Query("SELECT * FROM `sh_groups_users`");
        try {
            while (groupUserRs.next()) {
                int userId = groupUserRs.getInt("user_id");
                int groupId = groupUserRs.getInt("group_id");

                Group group = groups.stream().filter(g -> g.id == groupId).findFirst().orElse(null);
                if (group == null) {
                    group = new Group();
                    group.id = groupId;
                    group.userIds = new ArrayList<>();
                    groups.add(group);
                }

                group.userIds.add(userId);
                
            }
        } catch (SQLException e) {
            App.log.error("Failed to revalidate group registry", e);
        }

        App.appCache.set("shiina:groupRegistry", gson.toJson(groups));
    }


}
