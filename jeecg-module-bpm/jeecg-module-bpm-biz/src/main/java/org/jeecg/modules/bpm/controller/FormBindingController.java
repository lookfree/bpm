package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.common.BpmResult;
import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.service.form.FormBindingService;
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
    public BpmResult<Map<String, String>> bind(@RequestBody Map<String, String> body) {
        String id = service.bind(body.get("defId"), body.get("formId"), body.get("purpose"));
        return BpmResult.ok(Map.of("id", id));
    }

    @GetMapping
    public BpmResult<List<FormBinding>> list(@RequestParam String defId) {
        return BpmResult.ok(service.listByDef(defId));
    }

    @DeleteMapping("/{id}")
    public BpmResult<Void> unbind(@PathVariable String id) {
        service.unbind(id);
        return BpmResult.ok();
    }
}
