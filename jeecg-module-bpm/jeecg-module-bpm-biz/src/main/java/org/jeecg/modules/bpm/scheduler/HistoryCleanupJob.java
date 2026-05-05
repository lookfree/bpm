package org.jeecg.modules.bpm.scheduler;

import org.jeecg.modules.bpm.scheduler.cleanup.HistoryCleanupMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class HistoryCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(HistoryCleanupJob.class);

    private final HistoryCleanupMapper cleanupMapper;
    private final BpmSchedulerProperties props;

    public HistoryCleanupJob(HistoryCleanupMapper cleanupMapper, BpmSchedulerProperties props) {
        this.cleanupMapper = cleanupMapper;
        this.props = props;
    }

    @Scheduled(cron = "${bpm.scheduler.history-cleanup.cron:0 0 3 * * ?}")
    public void run() {
        int retentionDays = props.getHistoryCleanup().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("[BPM-CLEANUP] deleting history before {}", cutoff);

        int taskRows = cleanupMapper.deleteTaskHistory(cutoff);
        int metaRows = cleanupMapper.deleteInstanceMeta(cutoff);

        log.info("[BPM-CLEANUP] deleted taskHistory={} instanceMeta={}", taskRows, metaRows);
    }
}
