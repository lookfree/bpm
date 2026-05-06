package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.common.BpmResult;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.jeecg.modules.bpm.service.instance.StartRequest;
import org.jeecg.modules.bpm.service.instance.StartResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bpm/v1/instance")
public class InstanceController {

    private final InstanceService service;

    public InstanceController(InstanceService service) {
        this.service = service;
    }

    @PostMapping
    public BpmResult<StartResponse> start(@RequestBody StartRequest req) {
        return BpmResult.ok(service.start(req));
    }

    @GetMapping("/{id}")
    public BpmResult<InstanceMeta> get(@PathVariable String id) {
        InstanceMeta meta = service.getById(id);
        return meta == null ? BpmResult.error("实例不存在") : BpmResult.ok(meta);
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
