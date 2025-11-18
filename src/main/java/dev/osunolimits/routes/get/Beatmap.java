package dev.osunolimits.routes.get;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import dev.osunolimits.main.App;
import dev.osunolimits.models.FullBeatmap;
import dev.osunolimits.models.UserInfoObject;
import dev.osunolimits.modules.Shiina;
import dev.osunolimits.modules.ShiinaRoute;
import dev.osunolimits.modules.XmlConfig;
import dev.osunolimits.modules.ShiinaRoute.ShiinaRequest;
import dev.osunolimits.modules.utils.SEOBuilder;
import dev.osunolimits.modules.utils.ShiinaSupporterBadge;
import dev.osunolimits.utils.Validation;
import dev.osunolimits.utils.osu.OsuConverter;
import dev.osunolimits.utils.osu.PermissionHelper;
import spark.Request;
import spark.Response;

public class Beatmap extends Shiina {

    public static int pageSize = 25;
    private final Gson gson = new Gson();

    public Beatmap() {
        XmlConfig.getInstance().getOrDefault("comments.beatmaps.enabled", "false");
    }

    @Override
    public Object handle(Request req, Response res) throws Exception {
        ShiinaRequest shiina = new ShiinaRoute().handle(req, res);
        shiina.data.put("actNav", 3);

        Integer id = getBeatmapId(req);
        if (id == null) {
            return notFound(res, shiina);
        }

        Integer mode = getMode(req);
        String sort = getSort(req);
        int page = getPage(req);

        FullBeatmap fullBeatmap = new FullBeatmap();
        List<FullBeatmap.MapDiff> diffs = getBeatmapDiffs(id, shiina);

        if (diffs.isEmpty()) {
            return notFound(res, shiina);
        }

        fullBeatmap.setDiffs(diffs.toArray(new FullBeatmap.MapDiff[0]));
        ResultSet beatmapQuery = getBeatmapInfo(id, shiina);

        if (!beatmapQuery.next()) {
            return notFound(res, shiina);
        }

        populateBeatmapInfo(fullBeatmap, beatmapQuery);

        mode = adjustModeIfNeeded(fullBeatmap, mode);
        List<FullBeatmap.BeatmapScore> scores = getBeatmapScores(fullBeatmap, mode, sort, page, shiina);

        boolean hasNextPage = scores.size() == pageSize + 1;
        if (hasNextPage) {
            scores.remove(pageSize); // Remove the extra entry
        }

        if (shiina.loggedIn) {
            ResultSet favoriteCheckRs = shiina.mysql.Query(
                    "SELECT 1 FROM `favourites` WHERE `userid` = ? AND `setid` = ? LIMIT 1;", shiina.user.id,
                    fullBeatmap.getSetId());
            if (favoriteCheckRs.next()) {
                shiina.data.put("favorite", true);
            } else {
                shiina.data.put("favorite", false);
            }
        }

        fullBeatmap.setScores(scores.toArray(new FullBeatmap.BeatmapScore[0]));
        String peppyImageUrl = "https://assets.ppy.sh/beatmaps/" + fullBeatmap.getSetId() + "/covers/cover.jpg?1650681317";
        SEOBuilder seo = new SEOBuilder(fullBeatmap.getTitle(), App.customization.get("homeDescription").toString(), peppyImageUrl);
        shiina.data.put("seo", seo);
        shiina.data.put("commentsEnabled", Boolean.parseBoolean(XmlConfig.getInstance().getOrDefault("comments.beatmaps.enabled", "false")));
        shiina.data.put("beatmap", fullBeatmap);
        shiina.data.put("id", id);
        shiina.data.put("mode", mode);
        shiina.data.put("sort", sort);
        shiina.data.put("page", page);
        shiina.data.put("pageSize", pageSize);
        shiina.data.put("hasNextPage", hasNextPage);

        return renderTemplate("beatmap.html", shiina, res, req);
    }

    private Integer getBeatmapId(Request req) {
        if (req.params("id") != null && Validation.isNumeric(req.params("id"))) {
            return Integer.parseInt(req.params("id"));
        }
        return null;
    }

    private Integer getMode(Request req) {
        if (req.queryParams("mode") != null && Validation.isNumeric(req.queryParams("mode"))) {
            return Integer.parseInt(req.queryParams("mode"));
        }
        return 0;
    }

    private String getSort(Request req) {
        String sort = "pp";

        if(req.queryParams("sort") != null) {
            String sortParam = req.queryParams("sort").toLowerCase();
            if (sortParam.equals("pp") || sortParam.equals("score") || sortParam.equals("scorev2")) {
                sort = sortParam;
            }
        }

        return sort;
    }

    private int getPage(Request req) {
        if (req.queryParams("page") != null && Validation.isNumeric(req.queryParams("page"))
                && Integer.parseInt(req.queryParams("page")) > 0) {
            return Integer.parseInt(req.queryParams("page"));
        }
        return 1;
    }

    private List<FullBeatmap.MapDiff> getBeatmapDiffs(int id, ShiinaRequest shiina) throws Exception {
        List<FullBeatmap.MapDiff> diffs = new ArrayList<>();
        try (ResultSet queryDiffs = shiina.mysql.Query(
                "SELECT `id`, `version` FROM `maps` WHERE set_id = (SELECT set_id FROM `maps` WHERE id = ?) ORDER BY `maps`.`diff` ASC;",
                id)) {
            while (queryDiffs.next()) {
                FullBeatmap.MapDiff diff = new FullBeatmap().new MapDiff();
                diff.setId(queryDiffs.getInt("id"));
                diff.setVersion(queryDiffs.getString("version"));
                diffs.add(diff);
            }
        }
        return diffs;
    }

    private ResultSet getBeatmapInfo(int id, ShiinaRequest shiina) throws Exception {
        return shiina.mysql.Query(
                "SELECT `md5`, `set_id`, `title`, `status`, `artist`, `version`, `creator`, `last_update`, `total_length`, `max_combo`, `plays`, `passes`, `mode`, `bpm`, `cs`, `ar`, `od`, `hp`, `diff` FROM `maps` WHERE `id` = ?",
                id);
    }

    private void populateBeatmapInfo(FullBeatmap fullBeatmap, ResultSet beatmapQuery) throws Exception {
        fullBeatmap.setMd5(beatmapQuery.getString("md5"));
        fullBeatmap.setSetId(beatmapQuery.getInt("set_id"));
        fullBeatmap.setStatus(beatmapQuery.getInt("status"));
        fullBeatmap.setTitle(beatmapQuery.getString("title"));
        fullBeatmap.setArtist(beatmapQuery.getString("artist"));
        fullBeatmap.setVersion(beatmapQuery.getString("version"));
        fullBeatmap.setCreator(beatmapQuery.getString("creator"));
        fullBeatmap.setLastUpdate(beatmapQuery.getString("last_update"));
        fullBeatmap.setTotalLength(beatmapQuery.getInt("total_length"));
        fullBeatmap.setMaxCombo(beatmapQuery.getInt("max_combo"));
        fullBeatmap.setPlays(beatmapQuery.getInt("plays"));
        fullBeatmap.setPasses(beatmapQuery.getInt("passes"));
        fullBeatmap.setMode(beatmapQuery.getInt("mode"));
        fullBeatmap.setBpm(beatmapQuery.getDouble("bpm"));
        fullBeatmap.setCs(beatmapQuery.getDouble("cs"));
        fullBeatmap.setAr(beatmapQuery.getDouble("ar"));
        fullBeatmap.setOd(beatmapQuery.getDouble("od"));
        fullBeatmap.setHp(beatmapQuery.getDouble("hp"));
        fullBeatmap.setDiff(beatmapQuery.getDouble("diff"));
    }

    private Integer adjustModeIfNeeded(FullBeatmap fullBeatmap, Integer mode) {
        if (fullBeatmap.getMode() != 0 && mode < 4) {
            mode = fullBeatmap.getMode();
        }
        return mode;
    }

    private List<FullBeatmap.BeatmapScore> getBeatmapScores(FullBeatmap fullBeatmap, int mode, String sort, int page,
            ShiinaRequest shiina) throws Exception {
        String query = getScoreQuery(sort);
        if (query.isEmpty()) {
            return new ArrayList<>();
        }
        List<FullBeatmap.BeatmapScore> scores = new ArrayList<>();
        
        if (sort.equals("scorev2")) {
            // ScoreV2 query only needs 4 parameters
            try (ResultSet scoreQuery = shiina.mysql.Query(query, fullBeatmap.getMd5(), mode, pageSize + 1, ((page - 1) * pageSize))) {
                if (scoreQuery != null) {
                    processScoreResults(scoreQuery, scores);
                }
            }
        } else {
            // PP and Score queries need 5 parameters (including the subquery mode parameter)
            try (ResultSet scoreQuery = shiina.mysql.Query(query, fullBeatmap.getMd5(), mode, mode, pageSize + 1, ((page - 1) * pageSize))) {
                if (scoreQuery != null) {
                    processScoreResults(scoreQuery, scores);
                }
            }
        }
        
        return scores;
    }

    private void processScoreResults(ResultSet scoreQuery, List<FullBeatmap.BeatmapScore> scores) throws Exception {
        while (scoreQuery.next()) {
            FullBeatmap.BeatmapScore score = new FullBeatmap().new BeatmapScore();
            score.setId(scoreQuery.getInt("id"));
            score.setPp(scoreQuery.getInt("pp"));
            score.setScore(scoreQuery.getLong("score"));
            score.setGrade(scoreQuery.getString("grade"));
            score.setPlayTime(scoreQuery.getString("play_time"));
            score.setMods(OsuConverter.convertMods(scoreQuery.getInt("mods")));
            score.setUserId(scoreQuery.getInt("userid"));
            score.setName(scoreQuery.getString("name"));
            score.setCountry(scoreQuery.getString("country"));
            UserInfoObject userInfo = gson.fromJson(App.appCache.get("shiina:user:" + score.getUserId()),
                    UserInfoObject.class);

            if(PermissionHelper.hasPrivileges(userInfo.priv, PermissionHelper.Privileges.SUPPORTER)) {
                userInfo.groups.add(ShiinaSupporterBadge.getInstance().getGroup());
                score.setSupporter(true);
            }
            score.setUser(userInfo);
            scores.add(score);
        }
    }

    private String getScoreQuery(String sort) {
        switch (sort) {
            case "pp":
                return """
                        SELECT s.id, s.pp, s.score, s.grade, s.play_time, s.userid, s.mods,
                               u.name, u.country, u.priv
                        FROM scores AS s
                        LEFT JOIN users AS u ON s.userid = u.id
                        WHERE s.map_md5 = ? AND s.status = 2 AND s.mode = ?
                        AND (s.mods & 536870912) = 0
                        AND s.pp = (
                            SELECT MAX(inner_s.pp)
                            FROM scores AS inner_s
                            WHERE inner_s.map_md5 = s.map_md5
                            AND inner_s.userid = s.userid
                            AND inner_s.status = 2
                            AND inner_s.mode = ?
                            AND (inner_s.mods & 536870912) = 0
                        )
                        ORDER BY s.pp DESC, s.play_time ASC
                        LIMIT ? OFFSET ?
                        """;

            case "score":
                return """
                        SELECT s.id, s.pp, s.score, s.grade, s.play_time, s.userid, s.mods,
                               u.name, u.country, u.priv
                        FROM scores AS s
                        LEFT JOIN users AS u ON s.userid = u.id
                        WHERE s.map_md5 = ? AND s.status = 2 AND s.mode = ?
                        AND (s.mods & 536870912) = 0
                        AND s.score = (
                            SELECT MAX(inner_s.score)
                            FROM scores AS inner_s
                            WHERE inner_s.map_md5 = s.map_md5
                            AND inner_s.userid = s.userid
                            AND inner_s.status = 2
                            AND inner_s.mode = ?
                            AND (inner_s.mods & 536870912) = 0
                        )
                        ORDER BY s.score DESC, s.play_time ASC
                        LIMIT ? OFFSET ?
                        """;

            case "scorev2":
                return """
                        SELECT s.id, s.pp, s.score, s.grade, s.play_time, s.userid, s.mods,
                               u.name, u.country, u.priv
                        FROM scores AS s
                        LEFT JOIN users AS u ON s.userid = u.id
                        WHERE s.map_md5 = ? AND s.status = 2 AND s.mode = ?
                        AND (s.mods & 536870912) > 0
                        ORDER BY s.score DESC, s.play_time ASC
                        LIMIT ? OFFSET ?
                        """;

            default:
                // Default to PP leaderboard if sort parameter is invalid
                return """
                        SELECT s.id, s.pp, s.score, s.grade, s.play_time, s.userid, s.mods,
                               u.name, u.country, u.priv
                        FROM scores AS s
                        LEFT JOIN users AS u ON s.userid = u.id
                        WHERE s.map_md5 = ? AND s.status = 2 AND s.mode = ?
                        AND (s.mods & 536870912) = 0
                        AND s.pp = (
                            SELECT MAX(inner_s.pp)
                            FROM scores AS inner_s
                            WHERE inner_s.map_md5 = s.map_md5
                            AND inner_s.userid = s.userid
                            AND inner_s.status = 2
                            AND inner_s.mode = ?
                            AND (inner_s.mods & 536870912) = 0
                        )
                        ORDER BY s.pp DESC, s.play_time ASC
                        LIMIT ? OFFSET ?
                        """;
        }
    }

}
