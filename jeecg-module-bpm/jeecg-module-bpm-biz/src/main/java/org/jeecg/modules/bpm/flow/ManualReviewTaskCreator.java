package org.jeecg.modules.bpm.flow;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Creates a standalone "manual review" task when process flow cannot proceed normally.
 * Assignee defaults to "admin" (spec §9 fallback).
 */
@Component
public class ManualReviewTaskCreator {

    private final TaskService taskService;

    public ManualReviewTaskCreator(@Lazy TaskService taskService) {
        this.taskService = taskService;
    }

    public void createForInstance(String processInstanceId, String reason) {
        Task t = taskService.newTask("manual_review_" + processInstanceId);
        t.setName("流程异常人工处理");
        t.setAssignee("admin");
        t.setDescription(reason != null ? reason : "no matching flow");
        taskService.saveTask(t);
    }
}
