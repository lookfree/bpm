package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.service.task.BpmTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bpm/v1/task")
public class TaskController {

    private final BpmTaskService taskService;

    public TaskController(BpmTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/todo")
    public ResponseEntity<?> todo() {
        return ResponseEntity.ok(taskService.listTodo());
    }

    @GetMapping("/done")
    public ResponseEntity<?> done() {
        return ResponseEntity.ok(taskService.listDone());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        String comment = (String) body.get("comment");
        String targetUserId = (String) body.get("targetUserId");
        @SuppressWarnings("unchecked")
        Map<String, Object> formData = (Map<String, Object>) body.get("formData");
        try {
            taskService.complete(id, action, comment, targetUserId, formData);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("unsupported_action")) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/form")
    public ResponseEntity<?> getForm(@PathVariable String id) {
        try {
            return ResponseEntity.ok(taskService.getTaskForm(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
