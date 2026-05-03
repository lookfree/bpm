package org.jeecg.modules.bpm.engine;

import org.flowable.task.service.delegate.DelegateTask;
import org.jeecg.modules.bpm.domain.entity.NodeConfig;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.service.assignee.AssigneeResolver;
import org.jeecg.modules.bpm.service.nodecfg.NodeConfigService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AssigneeAssignmentTaskListenerTest {

    NodeConfigService nodeCfg = mock(NodeConfigService.class);
    AssigneeResolver resolver = mock(AssigneeResolver.class);
    InstanceMetaMapper instanceMetaMapper = mock(InstanceMetaMapper.class);
    AssigneeAssignmentTaskListener listener = new AssigneeAssignmentTaskListener(nodeCfg, resolver, instanceMetaMapper);

    @Test
    void singleCandidateBecomesAssignee() {
        DelegateTask task = mock(DelegateTask.class);
        when(task.getProcessDefinitionId()).thenReturn("def_v1");
        when(task.getTaskDefinitionKey()).thenReturn("approve_finance");
        when(task.getProcessInstanceId()).thenReturn("pi1");
        when(task.getVariables()).thenReturn(Collections.emptyMap());

        NodeConfig cfg = new NodeConfig();
        cfg.setAssigneeStrategy("{\"type\":\"USER\",\"payload\":{\"userIds\":[42]}}");
        when(nodeCfg.findByActDefAndNode("def_v1", "approve_finance")).thenReturn(Optional.of(cfg));
        when(resolver.resolve(anyString(), any())).thenReturn(List.of(42L));

        listener.notify(task);

        verify(task).setAssignee("42");
        verify(task, never()).addCandidateUser(anyString());
    }

    @Test
    void multipleCandidatesAddedAsCandidateUsers() {
        DelegateTask task = mock(DelegateTask.class);
        when(task.getProcessDefinitionId()).thenReturn("d");
        when(task.getTaskDefinitionKey()).thenReturn("n");
        when(task.getProcessInstanceId()).thenReturn("pi2");
        when(task.getVariables()).thenReturn(Collections.emptyMap());

        NodeConfig cfg = new NodeConfig();
        cfg.setAssigneeStrategy("{\"type\":\"USER\",\"payload\":{\"userIds\":[1,2]}}");
        when(nodeCfg.findByActDefAndNode("d", "n")).thenReturn(Optional.of(cfg));
        when(resolver.resolve(anyString(), any())).thenReturn(List.of(1L, 2L));

        listener.notify(task);

        verify(task).addCandidateUser("1");
        verify(task).addCandidateUser("2");
        verify(task, never()).setAssignee(anyString());
    }

    @Test
    void noConfigSilentlyDoesNothing() {
        DelegateTask task = mock(DelegateTask.class);
        when(task.getProcessDefinitionId()).thenReturn("d");
        when(task.getTaskDefinitionKey()).thenReturn("n");
        when(nodeCfg.findByActDefAndNode("d", "n")).thenReturn(Optional.empty());

        listener.notify(task);

        verifyNoInteractions(resolver);
    }

    @Test
    void emptyCandidatesLogsWarnAndSkips() {
        DelegateTask task = mock(DelegateTask.class);
        when(task.getProcessDefinitionId()).thenReturn("d");
        when(task.getTaskDefinitionKey()).thenReturn("n");
        when(task.getProcessInstanceId()).thenReturn("pi3");
        when(task.getVariables()).thenReturn(Collections.emptyMap());

        NodeConfig cfg = new NodeConfig();
        cfg.setAssigneeStrategy("{\"type\":\"USER\",\"payload\":{\"userIds\":[]}}");
        when(nodeCfg.findByActDefAndNode("d", "n")).thenReturn(Optional.of(cfg));
        when(resolver.resolve(anyString(), any())).thenReturn(Collections.emptyList());

        listener.notify(task);

        verify(task, never()).setAssignee(anyString());
        verify(task, never()).addCandidateUser(anyString());
    }
}
