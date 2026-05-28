package ch.css.jobrunr.control.adapter.ui;

import org.jboss.logging.Logger;

public class PerformanceLogger {

    private static final Logger LOG = Logger.getLogger(PerformanceLogger.class);

    private final long startTime;
    private final String label;

    public PerformanceLogger(String label) {
        this.label = label;
        this.startTime = System.currentTimeMillis();
    }

    public void log() {
        long duration = System.currentTimeMillis() - startTime;
        LOG.info(label + ": " + duration + " ms");
    }
}
