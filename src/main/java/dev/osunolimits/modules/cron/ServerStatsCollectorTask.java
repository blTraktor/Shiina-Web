package dev.osunolimits.modules.cron;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import dev.osunolimits.api.BanchoStats;
import dev.osunolimits.api.BanchoStats.PlayerCountResponse;
import dev.osunolimits.main.App;
import dev.osunolimits.modules.cron.engine.RunnableCronTask;

public class ServerStatsCollectorTask extends RunnableCronTask {

    private static final String REDIS_KEY = "shiina:hourly:histo";
    private static final long ONE_DAY_SECONDS = 86400;

    private static HourlyPlayerStatsStore store = new HourlyPlayerStatsStore();

    public static HourlyPlayerStatsStore getStore() {
        return store;
    }

    @Override
    public void run() {
        BanchoStats stats = new BanchoStats();
        PlayerCountResponse playerCount = stats.getPlayerCount();
        if (playerCount != null) {
            int onlinePlayers = playerCount.getOnline();
            store.addHourlyValue(onlinePlayers, logger);
            logger.debug("Added hourly player count: {}", onlinePlayers);
        } else {
            logger.warn("Failed to fetch player count from BanchoStats");
        }

        logger.debug("HourlyPlayerStatsThread stopped");
    }

    public static class HourlyPlayerStatsStore {
        public void addHourlyValue(int value, Logger logger) {
            // Round timestamp to the hour boundary to ensure only one entry per hour
            long now = Instant.now().getEpochSecond();
            long hourBoundary = (now / 3600) * 3600; // Round down to hour boundary
            String entry = hourBoundary + ":" + value;

            // Add value with hour boundary timestamp as score
            App.appCache.zadd(REDIS_KEY, hourBoundary, entry);

            // Remove entries older than 24 hours (keep exactly 24 hours of data)
            long cutoffTime = hourBoundary - ONE_DAY_SECONDS;
            App.appCache.zremrangeByScore(REDIS_KEY, "-inf", String.valueOf(cutoffTime));

            // Additional safety: ensure we don't exceed 24 entries
            long count = App.appCache.zcard(REDIS_KEY);
            if (count > 24) {
                App.appCache.zremrangeByRank(REDIS_KEY, 0, count - 25); // Keep last 24
            }

            logger.debug("Stored hourly value {} at timestamp {}. Total entries: {}", value, hourBoundary,
                    App.appCache.zcard(REDIS_KEY));
        }

        public List<Map<String, Object>> getLast24Hours() {
            // Get all entries (up to 24 hours)
            List<String> entries = App.appCache.zrange(REDIS_KEY, 0, -1);

            return entries.stream().map(entry -> {
                String[] parts = entry.split(":");
                long timestampSec = Long.parseLong(parts[0]);
                int value = Integer.parseInt(parts[1]);

                Map<String, Object> map = new HashMap<>();
                map.put("time", timestampSec * 1000); // convert to milliseconds
                map.put("value", value);
                return map;
            }).collect(Collectors.toList());
        }

        public int getStoredHoursCount() {
            return (int) App.appCache.zcard(REDIS_KEY);
        }
    }

    @Override
    public String getName() {
        return "ServerStatsCollectorTask";
    }

}