package dev.osunolimits.main;

import java.util.ArrayList;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import dev.osunolimits.common.Database;
import dev.osunolimits.common.MySQL;
import spark.Spark;

public class Shutdown extends Thread {

    public static final Logger log = (Logger) LoggerFactory.getLogger("Shutdown");

    @Override
    public void run() {
        log.debug("Starting shutdown process...");
        long startTime = System.currentTimeMillis();

        if (App.webServer != null) {
            log.debug("Stopping web server...");
            Spark.awaitStop();
            App.webServer.shutdown();
            App.webServer = null;
        }
        
        if (App.cron != null) {
            log.debug("Shutting down cron tasks...");
            App.cron.shutdown();
        }

        log.debug("Closing active MySQL connections (" + Database.runningConnections.size() + " connections)...");
        for (MySQL connection : new ArrayList<>(Database.runningConnections)) {
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("Error closing MySQL connection: " + e.getMessage());
            }
        }

        if (Database.dataSource != null && !Database.dataSource.isClosed()) {
            log.debug("Closing HikariCP data source...");
            Database.dataSource.close();
        }

        if (App.appCache != null) {
            log.debug("Closing Redis connection pool...");
            App.appCache.close();
        }

        // Force JVM to perform garbage collection to release any lingering resources
        System.gc();

        // Additional delay to ensure all resources are properly cleaned up
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Shutdown completed successfully in " + (System.currentTimeMillis() - startTime) + " ms.");
    }
}
