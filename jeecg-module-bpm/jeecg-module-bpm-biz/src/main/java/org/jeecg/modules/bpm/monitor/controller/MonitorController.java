package org.jeecg.modules.bpm.monitor.controller;

import org.jeecg.modules.bpm.monitor.dto.InstanceDiagramVO;
import org.jeecg.modules.bpm.monitor.dto.InterveneRequest;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceQuery;
import org.jeecg.modules.bpm.monitor.dto.StatsQuery;
import org.jeecg.modules.bpm.monitor.dto.StatsResponse;
import org.jeecg.modules.bpm.monitor.service.InstanceInterventionService;
import org.jeecg.modules.bpm.monitor.service.MonitorQueryService;
import org.jeecg.modules.bpm.monitor.service.MonitorStatsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/bpm/v1/monitor")
public class MonitorController {

    private final MonitorQueryService queryService;
    private final MonitorStatsService statsService;
    private final InstanceInterventionService interventionService;

    public MonitorController(MonitorQueryService queryService, MonitorStatsService statsService,
                             InstanceInterventionService interventionService) {
        this.queryService = queryService;
        this.statsService = statsService;
        this.interventionService = interventionService;
    }

    @GetMapping("/instances")
    public Map<String, Object> instances(MonitorInstanceQuery query) {
        return queryService.listInstances(query);
    }

    @GetMapping("/instances/{id}/diagram")
    public InstanceDiagramVO diagram(@PathVariable String id) {
        return queryService.getDiagram(id);
    }

    @GetMapping("/stats")
    public StatsResponse stats(StatsQuery query,
                               @RequestParam(name = "scope", required = false) Set<String> scope) {
        return statsService.compute(query, scope);
    }

    @PostMapping("/instances/{id}/intervene")
    public void intervene(@PathVariable String id, @RequestBody InterveneRequest req) {
        interventionService.intervene(id, req);
    }
}
