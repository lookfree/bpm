package org.jeecg.modules.bpm.monitor.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.Execution;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.monitor.dto.InstanceDiagramVO;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MonitorQueryServiceDiagramTest {

    private MonitorQueryService buildSvc(InstanceService instSvc,
                                         BpmProcessDefinitionService defSvc,
                                         RuntimeService rtSvc) {
        return new MonitorQueryService(
                mock(MonitorMapper.class), mock(BpmOrgService.class),
                instSvc, defSvc, rtSvc);
    }

    @Test
    void runningInstanceReturnsCurrentNodeIds() {
        InstanceService instSvc = mock(InstanceService.class);
        BpmProcessDefinitionService defSvc = mock(BpmProcessDefinitionService.class);
        RuntimeService rtSvc = mock(RuntimeService.class);

        InstanceMeta meta = new InstanceMeta();
        meta.setActInstId("proc1");
        meta.setDefId("def1");
        meta.setDefVersion(2);
        meta.setState("RUNNING");
        when(instSvc.findMeta("m1")).thenReturn(meta);
        when(defSvc.loadBpmnXml("def1", 2)).thenReturn("<bpmn/>");

        Execution e1 = mock(Execution.class);
        when(e1.getActivityId()).thenReturn("task_a");
        Execution e2 = mock(Execution.class);
        when(e2.getActivityId()).thenReturn("task_b");

        org.flowable.engine.runtime.ExecutionQuery q = mock(org.flowable.engine.runtime.ExecutionQuery.class);
        when(rtSvc.createExecutionQuery()).thenReturn(q);
        when(q.processInstanceId("proc1")).thenReturn(q);
        when(q.onlyChildExecutions()).thenReturn(q);
        when(q.list()).thenReturn(Arrays.asList(e1, e2));

        MonitorQueryService svc = buildSvc(instSvc, defSvc, rtSvc);
        InstanceDiagramVO vo = svc.getDiagram("m1");

        assertThat(vo.getBpmnXml()).isEqualTo("<bpmn/>");
        assertThat(vo.getCurrentNodeIds()).containsExactlyInAnyOrder("task_a", "task_b");
    }

    @Test
    void completedInstanceReturnsEmptyNodeIds() {
        InstanceService instSvc = mock(InstanceService.class);
        BpmProcessDefinitionService defSvc = mock(BpmProcessDefinitionService.class);
        RuntimeService rtSvc = mock(RuntimeService.class);

        InstanceMeta meta = new InstanceMeta();
        meta.setActInstId("proc2");
        meta.setDefId("def1");
        meta.setDefVersion(1);
        meta.setState("COMPLETED");
        when(instSvc.findMeta("m2")).thenReturn(meta);
        when(defSvc.loadBpmnXml("def1", 1)).thenReturn("<bpmn/>");

        MonitorQueryService svc = buildSvc(instSvc, defSvc, rtSvc);
        InstanceDiagramVO vo = svc.getDiagram("m2");

        assertThat(vo.getCurrentNodeIds()).isEmpty();
        verify(rtSvc, never()).createExecutionQuery();
    }

    @Test
    void throwsWhenInstanceNotFound() {
        InstanceService instSvc = mock(InstanceService.class);
        when(instSvc.findMeta("bad")).thenReturn(null);

        MonitorQueryService svc = buildSvc(instSvc, mock(BpmProcessDefinitionService.class),
                mock(RuntimeService.class));
        assertThatThrownBy(() -> svc.getDiagram("bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
