package org.jeecg.modules.bpm.service.instance;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class InstanceService {

    private final RuntimeService runtimeService;
    private final BpmFormService formService;
    private final BpmUserContext userContext;
    private final InstanceMetaMapper instanceMetaMapper;
    private final BpmProcessDefinitionMapper definitionMapper;

    public InstanceService(RuntimeService runtimeService, BpmFormService formService,
                           BpmUserContext userContext, InstanceMetaMapper instanceMetaMapper,
                           BpmProcessDefinitionMapper definitionMapper) {
        this.runtimeService = runtimeService;
        this.formService = formService;
        this.userContext = userContext;
        this.instanceMetaMapper = instanceMetaMapper;
        this.definitionMapper = definitionMapper;
    }

    public StartResponse start(StartRequest req) {
        BpmProcessDefinition def = definitionMapper.selectById(req.getDefId());
        if (def == null) throw new IllegalArgumentException("definition not found: " + req.getDefId());

        String businessKey = formService.saveFormSubmission(req.getFormId(), req.getFormData());

        Long applyUserId = userContext.currentUserId();
        Long applyDeptId = userContext.currentDeptId();

        Map<String, Object> vars = new HashMap<>();
        vars.put("formData", req.getFormData());
        vars.put("applyUserId", applyUserId);
        vars.put("applyDeptId", applyDeptId);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                def.getDefKey(), businessKey, vars);

        InstanceMeta meta = new InstanceMeta();
        meta.setActInstId(pi.getId());
        meta.setDefId(def.getId());
        meta.setDefVersion(def.getVersion());
        meta.setBusinessKey(businessKey);
        meta.setApplyUserId(applyUserId);
        meta.setApplyDeptId(applyDeptId);
        meta.setState("RUNNING");
        meta.setStartTime(LocalDateTime.now());
        instanceMetaMapper.insert(meta);

        StartResponse resp = new StartResponse();
        resp.setInstanceId(meta.getId());
        resp.setActInstId(pi.getId());
        resp.setBusinessKey(businessKey);
        return resp;
    }

    public InstanceMeta getById(String instanceId) {
        return instanceMetaMapper.selectById(instanceId);
    }

    public InstanceMeta findMeta(String instanceMetaId) {
        return instanceMetaMapper.selectById(instanceMetaId);
    }

    public void markCancelled(String instanceMetaId) {
        InstanceMeta meta = instanceMetaMapper.selectById(instanceMetaId);
        if (meta == null) return;
        meta.setState("CANCELLED");
        meta.setEndTime(LocalDateTime.now());
        instanceMetaMapper.updateById(meta);
    }
}
