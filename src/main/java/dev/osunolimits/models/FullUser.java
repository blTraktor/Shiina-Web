package dev.osunolimits.models;

import java.util.Map;

import lombok.Data;

@Data
public class FullUser {

    private String status;
    private Player player;

    @Data
    public class Player {
        private UserInfo info;
        private Map<String, UserStats> stats;
    }

    @Data
    public class UserStats {
        private int id;
        private int mode;
        private long tscore;
        private long rscore;
        private int pp;
        private int plays;
        private int playtime;
        private double acc;
        private int max_combo;
        private long total_hits;
        private long replay_views;
        private int xh_count;
        private int x_count;
        private int sh_count;
        private int s_count;
        private int a_count;
        private int rank;
        private int country_rank;
    }

    @Data
    public class UserInfo {
        private int id;
        private String name;
        private String safe_name;
        private int priv;
        private String country;
        private int silence_end;
        private int donor_end;
        private int creation_time;
        private int latest_activity;
        private int clan_id;
        private int clan_priv;
        private int preferred_mode;
        private int play_style;
        private String custom_badge_name;
        private String custom_badge_icon;
        private String userpage_content;
    }

}
