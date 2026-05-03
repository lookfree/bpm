package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.service.form.FormBindingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bpm/v1/form-binding")
public class FormBindingController {

    private final FormBindingService service;

    public FormBindingController(FormBindingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> bind(@RequestBody Map<String, String> body) {
        String id = service.bind(body.get("defId"), body.get("formId"), body.get("purpose"));
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping
    public ResponseEntity<List<FormBinding>> list(@RequestParam String defId) {
        return ResponseEntity.ok(service.listByDef(defId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unbind(@PathVariable String id) {
        service.unbind(id);
        return ResponseEntity.noContent().build();
    }
}
