package org.jeecg.modules.bpm.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bpm.scheduler")
public class BpmSchedulerProperties {

    private final Timeout timeout = new Timeout();
    private final HistoryCleanup historyCleanup = new HistoryCleanup();

    public Timeout getTimeout() { return timeout; }
    public HistoryCleanup getHistoryCleanup() { return historyCleanup; }

    public static class Timeout {
        private boolean enabled = true;
        private String cron = "0 */5 * * * ?";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }

    public static class HistoryCleanup {
        private boolean enabled = true;
        private String cron = "0 0 3 * * ?";
        private int retentionDays = 180;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }
}
