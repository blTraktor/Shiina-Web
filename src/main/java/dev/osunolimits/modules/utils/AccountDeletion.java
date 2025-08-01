package dev.osunolimits.modules.utils;

import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import dev.osunolimits.main.App;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.plugins.events.actions.OnAccountDeletionEvent;

public class AccountDeletion {

    private MySQL mysql;
    private static Logger logger = LoggerFactory.getLogger("AccountDeletion");

    public AccountDeletion() {
        this.mysql = Database.getConnection();
    }

    public AccountDeletion(MySQL mysql) {
        this.mysql = mysql;
    }

    public void deleteAccount(int userId) throws Exception {
        // Delete all sessions
        SessionBuilder.deleteAllSessions(userId);

        for(int i = 0; i < 8; i++) {
            if (i == 7) continue;
            App.jedisPool.zrem("bancho:leaderboard:" + i, String.valueOf(userId));
        }

        ResultSet getUserCountryRs = mysql.Query("SELECT country FROM users WHERE id = ?", userId);
        getUserCountryRs.next();
        String country = getUserCountryRs.getString("country");

        for(int i = 0; i < 8; i++) {
            if (i == 7) continue;
            App.jedisPool.zrem("bancho:leaderboard:" + i + ":" + country, String.valueOf(userId));
        }

        UserInfoObject userInfo = new UserInfoObject(userId);
        new OnAccountDeletionEvent(userInfo).callListeners();
        App.jedisPool.del("shiina:user:" + userId);

        try {
            // Delete from all related tables
            mysql.Exec("DELETE FROM `client_hashes` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `comments` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `favourites` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `ingame_logins` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `logs` WHERE `from` = ? OR `to` = ?", userId, userId);
            mysql.Exec("DELETE FROM `mail` WHERE `from_id` = ? OR `to_id` = ?", userId, userId);
            mysql.Exec("DELETE FROM `ratings` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `relationships` WHERE `user1` = ? OR `user2` = ?", userId, userId);
            mysql.Exec("DELETE FROM `scores` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `sh_audit` WHERE `user_id` = ? OR `target_id` = ?", userId, userId);
            mysql.Exec("DELETE FROM `sh_clan_denied` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `sh_clan_pending` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `sh_detections` WHERE `user` = ? OR `target` = ?", userId, userId);
            mysql.Exec("DELETE FROM `sh_groups_users` WHERE `user_id` = ?", userId);
            mysql.Exec("DELETE FROM `sh_payments` WHERE `user_id` = ?", userId);
            mysql.Exec("DELETE FROM `sh_recovery` WHERE `user` = ?", userId);
            mysql.Exec("DELETE FROM `stats` WHERE `id` = ?", userId);
            mysql.Exec("DELETE FROM `userpages` WHERE `user_id` = ?", userId);
            mysql.Exec("DELETE FROM `user_achievements` WHERE `userid` = ?", userId);
            mysql.Exec("DELETE FROM `sh_rank_cache` WHERE `user_id` = ?", userId);
            // Finally delete the user record
            mysql.Exec("DELETE FROM `users` WHERE `id` = ?", userId);

            logger.info("Account deletion completed for user ID {}", userId);

        } catch (Exception e) {
            logger.error("Error deleting account for user ID " + userId, e);
            throw e;
        } finally {
            mysql.close();
        }
    }
}
