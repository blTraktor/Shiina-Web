package dev.osunolimits.main.init;

import dev.osunolimits.main.App;
import dev.osunolimits.main.WebServer;
import dev.osunolimits.main.init.engine.RunableInitTask;

public class StartupWebServerTask extends RunableInitTask {
    @Override
    public void execute() throws Exception {
        System.setProperty("java.awt.headless", "true");
        App.webServer = new WebServer();

        int minWebServerThreads = Integer.parseInt(App.env.get("MIN_THREADS"));
        int maxWebServerThreads = Integer.parseInt(App.env.get("MAX_THREADS"));
        int webServerTimeoutMs = Integer.parseInt(App.env.get("TIMEOUT_MS"));

        App.webServer.setThreadPool(minWebServerThreads, maxWebServerThreads, webServerTimeoutMs);
        
        App.webServer.createDefaultDirectories();
        App.webServer.setDefaultDirectories();

        int templateUpdateDelay = Integer.parseInt(App.env.get("TEMPLATE_UPDATE_DELAY"));

        App.webServer.setTemplateUpdateDelay(templateUpdateDelay);

        String host = App.env.get("HOST");
        int port = App.devMode ? Integer.parseInt(App.env.get("DEV_PORT")) : Integer.parseInt(App.env.get("PORT"));

        App.webServer.ignite(logger, host, port, templateUpdateDelay);
    }

    @Override
    public String getName() {
        return "StartupWebServerTask";
    }
}
