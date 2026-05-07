package org.jeecg.modules.bpm.controller;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.jeecg.modules.bpm.common.BpmResult;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.service.task.BpmTaskService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bpm/v1/task")
public class TaskController {

    private final BpmTaskService taskService;
    private final TaskService flowableTaskService;
    private final BpmUserContext userContext;

    public TaskController(BpmTaskService taskService, TaskService flowableTaskService, BpmUserContext userContext) {
        this.taskService = taskService;
        this.flowableTaskService = flowableTaskService;
        this.userContext = userContext;
    }

    @GetMapping("/todo")
    public BpmResult<?> todo() {
        return BpmResult.ok(taskService.listTodo());
    }

    @GetMapping("/done")
    public BpmResult<?> done() {
        return BpmResult.ok(taskService.listDone());
    }

    @GetMapping("/debug-all")
    public BpmResult<?> debugAll(HttpServletRequest req) {
        List<Task> all = flowableTaskService.createTaskQuery().list();
        String currentUserId = String.valueOf(userContext.currentUserId());
        String currentUsername = userContext.currentUsername();
        String tokenHeader = req.getHeader("X-Access-Token");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("currentUserId", currentUserId);
        info.put("currentUsername", currentUsername);
        info.put("hasTokenHeader", tokenHeader != null);
        info.put("tokenHeaderLen", tokenHeader != null ? tokenHeader.length() : 0);
        info.put("userContextClass", userContext.getClass().getName());

        List<Map<String, Object>> rows = all.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.getId());
            m.put("name", t.getName());
            m.put("assignee", t.getAssignee());
            m.put("processInstanceId", t.getProcessInstanceId());
            return m;
        }).collect(Collectors.toList());
        info.put("tasks", rows);
        return BpmResult.ok(info);
    }

    @PostMapping("/{id}/complete")
    public BpmResult<?> complete(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        String comment = (String) body.get("comment");
        String targetUserId = (String) body.get("targetUserId");
        @SuppressWarnings("unchecked")
        Map<String, Object> formData = (Map<String, Object>) body.get("formData");
        taskService.complete(id, action, comment, targetUserId, formData);
        return BpmResult.ok(Map.of("status", "ok"));
    }

    @GetMapping("/{id}/form")
    public BpmResult<?> getForm(@PathVariable String id) {
        return BpmResult.ok(taskService.getTaskForm(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BpmResult<String> onNotFound(IllegalArgumentException e) {
        return BpmResult.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BpmResult<String> onError(Exception e) {
        return BpmResult.error(e.getMessage());
    }
}
