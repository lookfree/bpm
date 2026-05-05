package org.jeecg.modules.bpm.scheduler;

import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.scheduler.service.NodeTimeoutHandler;
import org.jeecg.modules.bpm.scheduler.service.OverdueTaskRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NodeTimeoutCheckJob {

    private static final Logger log = LoggerFactory.getLogger(NodeTimeoutCheckJob.class);

    private final MonitorMapper monitorMapper;
    private final NodeTimeoutHandler handler;

    public NodeTimeoutCheckJob(MonitorMapper monitorMapper, NodeTimeoutHandler handler) {
        this.monitorMapper = monitorMapper;
        this.handler = handler;
    }

    @Scheduled(cron = "${bpm.scheduler.timeout.cron:0 */5 * * * ?}")
    public void run() {
        List<OverdueTaskRow> rows = monitorMapper.selectOverdueRunningTasks();
        if (rows.isEmpty()) {
            return;
        }
        log.info("[BPM-TIMEOUT] checking {} overdue tasks", rows.size());
        for (OverdueTaskRow row : rows) {
            try {
                handler.handle(row);
            } catch (Exception e) {
                log.error("[BPM-TIMEOUT] failed to handle task={}: {}", row.getTaskId(), e.getMessage());
            }
        }
    }
}
