package org.jeecg.modules.bpm.controller;

import org.flowable.engine.ProcessEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/bpm/v1")
public class BpmHealthController {

    private final ProcessEngine processEngine;

    public BpmHealthController(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("engine", "flowable");
        body.put("version", ProcessEngine.VERSION);
        body.put("name", processEngine.getName());
        return body;
    }
}
