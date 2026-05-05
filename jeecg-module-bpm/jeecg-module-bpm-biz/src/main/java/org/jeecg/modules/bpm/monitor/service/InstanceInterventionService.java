package org.jeecg.modules.bpm.monitor.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.monitor.dto.InterveneRequest;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstanceInterventionService {

    private final InstanceService instanceService;
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    public InstanceInterventionService(InstanceService instanceService,
                                       TaskService taskService,
                                       RuntimeService runtimeService) {
        this.instanceService = instanceService;
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    public void intervene(String instMetaId, InterveneRequest req) {
        InstanceMeta meta = instanceService.findMeta(instMetaId);
        if (meta == null) {
            throw new IllegalArgumentException("instance not found: " + instMetaId);
        }
        if (!"RUNNING".equals(meta.getState())) {
            throw new IllegalStateException("instance is not RUNNING: " + meta.getState());
        }

        switch (req.getAction()) {
            case "FORCE_COMPLETE_TASK":
                forceCompleteTask(meta);
                break;
            case "FORCE_CANCEL":
                forceCancel(meta, req.getComment());
                break;
            case "FORCE_REASSIGN":
                forceReassign(meta, req.getTargetUserId());
                break;
            default:
                throw new IllegalArgumentException("unknown action: " + req.getAction());
        }
    }

    private void forceCompleteTask(InstanceMeta meta) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(meta.getActInstId())
                .list();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("no active tasks for instance: " + meta.getActInstId());
        }
        for (Task task : tasks) {
            taskService.complete(task.getId());
        }
    }

    private void forceCancel(InstanceMeta meta, String reason) {
        String deleteReason = reason != null ? reason : "FORCE_CANCEL";
        runtimeService.deleteProcessInstance(meta.getActInstId(), deleteReason);
        instanceService.markCancelled(meta.getId());
    }

    private void forceReassign(InstanceMeta meta, String targetUserId) {
        if (targetUserId == null || targetUserId.isEmpty()) {
            throw new IllegalArgumentException("targetUserId is required for FORCE_REASSIGN");
        }
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(meta.getActInstId())
                .list();
        if (tasks.isEmpty()) {
            throw new IllegalStateException("no active tasks for instance: " + meta.getActInstId());
        }
        for (Task task : tasks) {
            taskService.setAssignee(task.getId(), targetUserId);
        }
    }
}
