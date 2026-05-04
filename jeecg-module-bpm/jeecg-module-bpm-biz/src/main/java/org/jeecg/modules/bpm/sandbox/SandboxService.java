package org.jeecg.modules.bpm.sandbox;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SandboxService {

    private final BpmProcessDefinitionMapper defMapper;
    private final SandboxRunService sandboxRunService;
    private final InstanceMetaMapper instanceMetaMapper;
    private final RuntimeService runtimeService;

    public SandboxService(BpmProcessDefinitionMapper defMapper,
                          SandboxRunService sandboxRunService,
                          InstanceMetaMapper instanceMetaMapper,
                          RuntimeService runtimeService) {
        this.defMapper = defMapper;
        this.sandboxRunService = sandboxRunService;
        this.instanceMetaMapper = instanceMetaMapper;
        this.runtimeService = runtimeService;
    }

    public Long start(String sandboxDefId, Map<String, Object> formData, Long runnerId) {
        BpmProcessDefinition def = defMapper.selectById(sandboxDefId);
        if (def == null) throw new IllegalArgumentException("definition not found: " + sandboxDefId);
        if (!"SANDBOX".equals(def.getCategory())) {
            throw new IllegalArgumentException("definition is not a SANDBOX definition: " + sandboxDefId);
        }

        Long runId = sandboxRunService.start(sandboxDefId, runnerId);

        try {
            Map<String, Object> vars = new HashMap<>();
            if (formData != null) vars.putAll(formData);
            vars.put("formData", formData);

            ProcessInstance pi = runtimeService.startProcessInstanceByKey(def.getDefKey(), vars);

            InstanceMeta meta = new InstanceMeta();
            meta.setActInstId(pi.getId());
            meta.setDefId(def.getId());
            meta.setDefVersion(def.getVersion() != null ? def.getVersion() : 0);
            meta.setState("SANDBOX");
            meta.setStartTime(LocalDateTime.now());
            instanceMetaMapper.insert(meta);

            sandboxRunService.appendLog(runId, "Process instance started: " + pi.getId());
        } catch (Exception e) {
            sandboxRunService.finish(runId, SandboxResult.FAIL);
            try {
                sandboxRunService.appendLog(runId, "ERROR: " + e.getMessage());
            } catch (Exception ignored) {
                // run already finished, ignore log failure
            }
        }

        return runId;
    }

    public SandboxRun getRun(Long runId) {
        return sandboxRunService.findById(runId);
    }
}
