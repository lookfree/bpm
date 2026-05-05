package org.jeecg.modules.bpm.monitor.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.monitor.dto.InstanceDiagramVO;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceQuery;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceVO;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MonitorQueryService {

    private final MonitorMapper mapper;
    private final BpmOrgService orgService;
    private final InstanceService instanceService;
    private final BpmProcessDefinitionService definitionService;
    private final RuntimeService runtimeService;

    public MonitorQueryService(MonitorMapper mapper, BpmOrgService orgService,
                               InstanceService instanceService,
                               BpmProcessDefinitionService definitionService,
                               RuntimeService runtimeService) {
        this.mapper = mapper;
        this.orgService = orgService;
        this.instanceService = instanceService;
        this.definitionService = definitionService;
        this.runtimeService = runtimeService;
    }

    public Map<String, Object> listInstances(MonitorInstanceQuery q) {
        int pageNo = Math.max(1, q.getPageNo());
        int pageSize = Math.max(1, Math.min(200, q.getPageSize()));
        int offset = (pageNo - 1) * pageSize;

        long total = mapper.countInstances(q);
        List<MonitorInstanceVO> rows = mapper.selectInstances(q, offset, pageSize);
        for (MonitorInstanceVO row : rows) {
            if (row.getApplyUserId() != null) {
                row.setApplyUserName(orgService.findUserName(row.getApplyUserId()));
            }
            if (row.getApplyDeptId() != null) {
                row.setApplyDeptName(orgService.findDeptName(row.getApplyDeptId()));
            }
        }
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", rows);
        page.put("total", total);
        page.put("pageNo", pageNo);
        page.put("pageSize", pageSize);
        return page;
    }

    public InstanceDiagramVO getDiagram(String instMetaId) {
        InstanceMeta meta = instanceService.findMeta(instMetaId);
        if (meta == null) throw new IllegalArgumentException("instance not found: " + instMetaId);

        String bpmnXml = definitionService.loadBpmnXml(meta.getDefId(), meta.getDefVersion());

        InstanceDiagramVO vo = new InstanceDiagramVO();
        vo.setBpmnXml(bpmnXml);

        if ("RUNNING".equals(meta.getState()) || "SANDBOX".equals(meta.getState())
                || "SUSPENDED".equals(meta.getState())) {
            List<Execution> execs = runtimeService.createExecutionQuery()
                    .processInstanceId(meta.getActInstId())
                    .onlyChildExecutions()
                    .list();
            Set<String> nodes = new LinkedHashSet<>();
            for (Execution e : execs) {
                if (e.getActivityId() != null) nodes.add(e.getActivityId());
            }
            vo.setCurrentNodeIds(new ArrayList<>(nodes));
        } else {
            vo.setCurrentNodeIds(Collections.emptyList());
        }
        return vo;
    }
}
