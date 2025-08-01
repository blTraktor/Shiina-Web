package dev.osunolimits.modules.cron.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnableCronTask implements CronTask {

    protected Logger logger = LoggerFactory.getLogger("CronTask [" + getName() + "]");

    @Override
    public void initialize() { }

    @Override
    public void run() { }

    @Override
    public void shutdown() { }

    @Override
    public String getName() {
        return "RunnableCronTask";
    }

}
