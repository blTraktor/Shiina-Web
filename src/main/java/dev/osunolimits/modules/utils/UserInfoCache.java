package dev.osunolimits.modules.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.main.App;
import dev.osunolimits.models.Group;
import dev.osunolimits.models.UserInfoObject;

public class UserInfoCache {

    private Gson gson;
    private MySQL mysql;
    private Logger log = (Logger) LoggerFactory.getLogger("RedisUserInfoCache");

    public UserInfoCache() {
        gson = new Gson();
        mysql = Database.getConnection();
    }

    public void populateIfNeeded() {
        try {
            ResultSet usersRs = mysql.Query("SELECT `id`, `name`, `safe_name`, `priv` FROM `users`");
            List<UserInfoObject> users = new ArrayList<>();
            while (usersRs.next()) {
                UserInfoObject user = new UserInfoObject();
                user.id = usersRs.getInt("id");
                user.name = usersRs.getString("name");
                user.safe_name = usersRs.getString("safe_name");
                user.priv = usersRs.getInt("priv");
                user.groups = new ArrayList<>();
                users.add(user);
            }

            List<Group> groups = new ArrayList<>();
            ResultSet groupsRs = mysql.Query("SELECT `id`, `name`, `emoji` FROM `sh_groups`");

            while (groupsRs.next()) {
                Group group = new Group();
                group.id = groupsRs.getInt("id");
                group.name = groupsRs.getString("name");
                group.emoji = groupsRs.getString("emoji");
                groups.add(group);
            }

            ResultSet userGroupRs = mysql.Query("SELECT `user_id`, `group_id` FROM `sh_groups_users`");

            while (userGroupRs.next()) {
                int userId = userGroupRs.getInt("user_id");
                int groupId = userGroupRs.getInt("group_id");

                for (UserInfoObject user : users) {
                    if (user.id == userId) {
                        for (Group group : groups) {
                            if (group.id == groupId) {
                                user.groups.add(group);
                            }
                        }
                    }
                }
            }

            for (UserInfoObject user : users) {
                if (App.appCache.get("shiina:user:" + user.id) == null) {
                    App.appCache.set("shiina:user:" + user.id, gson.toJson(user));
                }
            }
        } catch (SQLException e) {
            log.error("SQL Error: ", e);
        }
        mysql.close();

    }

    public void reloadUser(int userId) {
        try {
            ResultSet userRs = mysql.Query("SELECT `id`, `name`, `safe_name`, `priv` FROM `users` WHERE `id` = ?", userId);
            if (userRs.next()) {
                UserInfoObject user = new UserInfoObject();
                user.id = userRs.getInt("id");
                user.name = userRs.getString("name");
                user.safe_name = userRs.getString("safe_name");
                user.priv = userRs.getInt("priv");
                user.groups = new ArrayList<>();
    
                ResultSet userGroupRs = mysql.Query("SELECT `group_id` FROM `sh_groups_users` WHERE `user_id` = ?", userId);
                while (userGroupRs.next()) {
                    int groupId = userGroupRs.getInt("group_id");
                    ResultSet groupRs = mysql.Query("SELECT `id`, `name`, `emoji` FROM `sh_groups` WHERE `id` = ?",
                            groupId);
                    if (groupRs.next()) {
                        Group group = new Group();
                        group.id = groupRs.getInt("id");
                        group.name = groupRs.getString("name");
                        group.emoji = groupRs.getString("emoji");
                        user.groups.add(group);
                    }
                }

                if (user.id == 0) {
                    log.error("User ID is 0, skipping reload");
                    return;
                }
    
                App.appCache.set("shiina:user:" + user.id, gson.toJson(user));
                
            }else {
                log.error("User not found in database: " + userId);
            }


        } catch (SQLException e) {
            log.error("SQL Error: ", e);
        }
        mysql.close();
    }

    public void reloadUserIfNotPresent(int userId) {
        if (App.appCache.get("shiina:user:" + userId) == null) {
            reloadUser(userId);
        }else {
            mysql.close();
        }
    }

}
