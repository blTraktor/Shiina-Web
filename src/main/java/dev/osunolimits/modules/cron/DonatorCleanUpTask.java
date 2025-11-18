package dev.osunolimits.modules.cron;

import java.sql.ResultSet;

import com.google.gson.Gson;

import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.cron.engine.RunnableCronTask;
import dev.osunolimits.utils.osu.PermissionHelper;
import dev.osunolimits.utils.osu.PermissionHelper.Privileges;

public class DonatorCleanUpTask extends RunnableCronTask {

    @Override
    public String getName() {
        return "DonatorCleanUpTask";
    }

    @Override
    public void run() {
        Gson gson = new Gson();

        MySQL mysql = Database.getConnection();

        try {

            ResultSet rs = mysql.Query("SELECT `donor_end`, `priv`, `id` FROM `users`");

            try {
                while (rs.next()) {
                    long donorEnd = rs.getLong("donor_end");
                    int priv = rs.getInt("priv");
                    int id = rs.getInt("id");
                    long currentTime = System.currentTimeMillis() / 1000L;
                    UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + id),
                            UserInfoObject.class);

                    if (donorEnd == 0) {
                        int newPriv = Privileges.removePrivilege(priv, Privileges.SUPPORTER);
                        if (newPriv != priv) {
                            userInfo.priv = newPriv;
                            String updatedValue = gson.toJson(userInfo);
                            App.appCache.set("shiina:user:" + id, updatedValue);
                            logger.info("Updated user ID " + userInfo.id + ": removed SUPPORTER privilege.");
                            continue;
                        }

                        if (donorEnd < currentTime && PermissionHelper.hasPrivileges(priv, Privileges.SUPPORTER)) {
                            // Donator status has expired, update the user

                            mysql.Exec("UPDATE `users` SET `priv` = ? WHERE `id` = ?", newPriv, userInfo.id);

                            // Update the cached user info
                            userInfo.priv = newPriv;
                            String updatedValue = gson.toJson(userInfo);
                            App.appCache.set("shiina:user:" + id, updatedValue);

                            logger.info("Updated user ID " + userInfo.id + ": removed SUPPORTER privilege.");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            mysql.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}