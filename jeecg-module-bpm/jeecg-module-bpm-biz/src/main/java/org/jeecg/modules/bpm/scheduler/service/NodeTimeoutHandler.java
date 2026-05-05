package org.jeecg.modules.bpm.scheduler.service;

import org.flowable.engine.TaskService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NodeTimeoutHandler {

    private static final Logger log = LoggerFactory.getLogger(NodeTimeoutHandler.class);

    private final TaskService taskService;
    private final BpmOrgService orgService;

    public NodeTimeoutHandler(TaskService taskService, BpmOrgService orgService) {
        this.taskService = taskService;
        this.orgService = orgService;
    }

    public void handle(OverdueTaskRow row) {
        if (row.getTimeoutAction() == null) {
            return;
        }
        switch (row.getTimeoutAction()) {
            case "REMIND":
                handleRemind(row);
                break;
            case "AUTO_PASS":
                handleAutoPass(row);
                break;
            case "ESCALATE":
                handleEscalate(row);
                break;
            default:
                log.warn("unknown timeoutAction={} for task={}", row.getTimeoutAction(), row.getTaskId());
        }
    }

    private void handleRemind(OverdueTaskRow row) {
        log.info("[BPM-TIMEOUT] REMIND task={} inst={} assignee={}",
                row.getTaskId(), row.getActInstId(), row.getAssignee());
    }

    private void handleAutoPass(OverdueTaskRow row) {
        try {
            taskService.complete(row.getTaskId());
            log.info("[BPM-TIMEOUT] AUTO_PASS task={} inst={}", row.getTaskId(), row.getActInstId());
        } catch (Exception e) {
            log.error("[BPM-TIMEOUT] AUTO_PASS failed task={}: {}", row.getTaskId(), e.getMessage());
        }
    }

    private void handleEscalate(OverdueTaskRow row) {
        try {
            String assignee = row.getAssignee();
            if (assignee == null) {
                log.warn("[BPM-TIMEOUT] ESCALATE skipped — no assignee for task={}", row.getTaskId());
                return;
            }
            Long userId;
            try {
                userId = Long.parseLong(assignee);
            } catch (NumberFormatException e) {
                log.warn("[BPM-TIMEOUT] ESCALATE skipped — non-numeric assignee={}", assignee);
                return;
            }
            Long managerId = orgService.findUserMainDeptId(userId);
            if (managerId == null) {
                log.warn("[BPM-TIMEOUT] ESCALATE skipped — no dept for user={}", userId);
                return;
            }
            taskService.setAssignee(row.getTaskId(), String.valueOf(managerId));
            log.info("[BPM-TIMEOUT] ESCALATE task={} reassigned to dept={}", row.getTaskId(), managerId);
        } catch (Exception e) {
            log.error("[BPM-TIMEOUT] ESCALATE failed task={}: {}", row.getTaskId(), e.getMessage());
        }
    }
}
