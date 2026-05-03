package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.jeecg.modules.bpm.service.instance.StartRequest;
import org.jeecg.modules.bpm.service.instance.StartResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bpm/v1/instance")
public class InstanceController {

    private final InstanceService service;

    public InstanceController(InstanceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StartResponse> start(@RequestBody StartRequest req) {
        return ResponseEntity.ok(service.start(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstanceMeta> get(@PathVariable String id) {
        InstanceMeta meta = service.getById(id);
        if (meta == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(meta);
    }
}
