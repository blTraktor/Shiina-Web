package dev.osunolimits.api;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import dev.osunolimits.common.MySQL;
import dev.osunolimits.main.App;
import dev.osunolimits.models.Beatmap;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.utils.ShiinaSupporterBadge;
import dev.osunolimits.utils.osu.OsuConverter;
import dev.osunolimits.utils.osu.PermissionHelper;
import lombok.Data;

public class ScoreQuery {

    private MySQL mysql;
    private static final Gson gson = new Gson();

    public ScoreQuery(MySQL mysql) {
        this.mysql = mysql;
    }

    @Data
    public class Score {
        private int id;
        private long score;
        private int pp;
        private double acc;
        private int maxCombo;
        private String[] mods;
        private int n300;
        private int n100;
        private int n50;
        private int nmiss;
        private int nkatu;
        private int ngeki;
        private String grade;
        private int status;
        private int mode;
        private String playTime;
        private int userId;
        private String username;
        private String country;
        private int perfect;
        private Beatmap beatmap;
        private UserInfoObject user;
        private boolean supporter;
    }

    private final String SCORE_QUERY = "SELECT `scores`.`id` AS `score_id`, `score`, `pp`, `acc`, `scores`.`max_combo` AS `max_combo_scores`, `mods`, `n300`, `n100`, `n50`, `nmiss`, `ngeki`, `nkatu`, `grade`, `scores`.`status` AS `score_status`, `scores`.`mode` AS `score_mode`, `play_time`, `userid`, `users`.`name`, `users`.`country`, `perfect`, `maps`.`id` AS `bm_id`, `maps`.`set_id`, `maps`.`filename` AS `bm_title`, `maps`.`artist`, `maps`.`creator`, `maps`.`passes`, `maps`.`plays`, `maps`.`diff`, `maps`.`last_update`, `maps`.`status` AS `bm_status` FROM `scores` INNER JOIN `maps` ON `map_md5` = `maps`.`md5` LEFT JOIN `users` ON `userid` = `users`.`id` WHERE `scores`.`id` = ?;";

    public Score getScore(int id) throws SQLException {
        ResultSet scoreRs = mysql.Query(SCORE_QUERY, id);

        if (scoreRs.next()) {
            Score score = new Score();
            score.setId(scoreRs.getInt("score_id"));
            score.setScore(scoreRs.getLong("score"));
            score.setPp(scoreRs.getInt("pp"));
            score.setAcc(scoreRs.getDouble("acc"));
            score.setMaxCombo(scoreRs.getInt("max_combo_scores"));
            score.setMods(OsuConverter.convertMods(scoreRs.getInt("mods")));
            score.setN300(scoreRs.getInt("n300"));
            score.setN100(scoreRs.getInt("n100"));
            score.setN50(scoreRs.getInt("n50"));
            score.setNmiss(scoreRs.getInt("nmiss"));
            score.setNkatu(scoreRs.getInt("nkatu"));
            score.setNgeki(scoreRs.getInt("ngeki"));
            score.setGrade(scoreRs.getString("grade"));
            score.setStatus(scoreRs.getInt("score_status"));
            score.setMode(scoreRs.getInt("score_mode"));
            score.setPlayTime(scoreRs.getString("play_time"));
            score.setUserId(scoreRs.getInt("userid"));
            score.setUsername(scoreRs.getString("name"));
            score.setCountry(scoreRs.getString("country"));
            score.setPerfect(scoreRs.getInt("perfect"));

            Beatmap beatmap = new Beatmap();
            beatmap.setId(scoreRs.getInt("bm_id"));
            beatmap.setSet_id(scoreRs.getInt("set_id"));
            beatmap.setFilename(scoreRs.getString("bm_title"));
            beatmap.setArtist(scoreRs.getString("artist"));
            beatmap.setCreator(scoreRs.getString("creator"));
            beatmap.setPlays(scoreRs.getInt("plays"));
            beatmap.setPasses(scoreRs.getInt("passes"));
            beatmap.setDiff(scoreRs.getFloat("diff"));
            beatmap.setLast_update(scoreRs.getString("last_update"));
            beatmap.setStatus(scoreRs.getInt("bm_status"));

            UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + score.getUserId()),
                    UserInfoObject.class);
            if (PermissionHelper.hasPrivileges(userInfo.priv, PermissionHelper.Privileges.SUPPORTER)) {
                userInfo.groups.add(ShiinaSupporterBadge.getInstance().getGroup());
                score.setSupporter(true);
            }
            score.setUser(userInfo);

            score.setBeatmap(beatmap);

            return score;
        }

        return null;
    }

}
