package org.jeecg.modules.bpm.service.task;

import org.jeecg.modules.bpm.domain.entity.TaskHistory;
import org.jeecg.modules.bpm.mapper.TaskHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TaskHistoryWriter {

    private static final Logger log = LoggerFactory.getLogger(TaskHistoryWriter.class);
    private final TaskHistoryMapper mapper;

    public TaskHistoryWriter(TaskHistoryMapper mapper) {
        this.mapper = mapper;
    }

    public void write(Entry e) {
        TaskHistory h = new TaskHistory();
        h.setActTaskId(e.getTaskId());
        h.setInstId(e.getInstId());
        h.setNodeId(e.getNodeId());
        h.setAssigneeId(e.getAssigneeId());
        h.setAction(e.getAction());
        h.setComment(e.getComment());
        h.setAttachments(e.getAttachments());
        h.setOpTime(LocalDateTime.now());
        try {
            mapper.insert(h);
        } catch (DuplicateKeyException dup) {
            log.info("idempotent skip: task_history task={} action={} already exists", e.getTaskId(), e.getAction());
        }
    }

    public static class Entry {
        private final String taskId;
        private final String instId;
        private final String nodeId;
        private final Long assigneeId;
        private final String action;
        private final String comment;
        private final String attachments;

        public Entry(String taskId, String instId, String nodeId,
                     Long assigneeId, String action, String comment, String attachments) {
            this.taskId = taskId;
            this.instId = instId;
            this.nodeId = nodeId;
            this.assigneeId = assigneeId;
            this.action = action;
            this.comment = comment;
            this.attachments = attachments;
        }

        public String getTaskId() { return taskId; }
        public String getInstId() { return instId; }
        public String getNodeId() { return nodeId; }
        public Long getAssigneeId() { return assigneeId; }
        public String getAction() { return action; }
        public String getComment() { return comment; }
        public String getAttachments() { return attachments; }
    }
}
