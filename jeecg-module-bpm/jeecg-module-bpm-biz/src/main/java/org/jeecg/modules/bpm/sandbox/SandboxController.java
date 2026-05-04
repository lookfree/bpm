package org.jeecg.modules.bpm.sandbox;

import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bpm/v1/sandbox")
public class SandboxController {

    private final SandboxService sandboxService;
    private final SandboxRunService sandboxRunService;
    private final BpmUserContext userContext;

    public SandboxController(SandboxService sandboxService,
                             SandboxRunService sandboxRunService,
                             BpmUserContext userContext) {
        this.sandboxService = sandboxService;
        this.sandboxRunService = sandboxRunService;
        this.userContext = userContext;
    }

    @PostMapping("/{defId}/start")
    public Map<String, Long> start(@PathVariable String defId,
                                    @RequestBody(required = false) Map<String, Object> formData) {
        Long runId = sandboxService.start(defId, formData, userContext.currentUserId());
        Map<String, Long> result = new HashMap<>();
        result.put("runId", runId);
        return result;
    }

    @GetMapping("/{runId}")
    public SandboxRun get(@PathVariable Long runId) {
        return sandboxRunService.findById(runId);
    }
}
