package org.jeecg.modules.bpm.monitor.service;

import org.jeecg.modules.bpm.monitor.dto.*;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitorStatsServiceTest {

    @Test
    void byDefinitionComputesRates() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        StatsByDefinitionRow row = new StatsByDefinitionRow();
        row.setDefKey("leave");
        row.setInstanceCount(10L);
        row.setCompletedCount(7L);
        row.setOverdueCount(2L);
        row.setAvgDurationMs(123456.0);
        when(mapper.selectStatsByDefinition(any())).thenReturn(List.of(row));

        MonitorStatsService svc = new MonitorStatsService(mapper, mock(BpmOrgService.class));
        StatsResponse r = svc.compute(new StatsQuery(), Set.of("byDefinition"));

        assertThat(r.getByDefinition()).hasSize(1);
        StatsByDefinitionRow o = r.getByDefinition().get(0);
        assertThat(o.getCompletionRate()).isEqualTo(0.7);
        assertThat(o.getOverdueRate()).isEqualTo(0.2);
    }

    @Test
    void byDefinitionHandlesZeroInstances() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        StatsByDefinitionRow row = new StatsByDefinitionRow();
        row.setDefKey("empty");
        row.setInstanceCount(0L);
        row.setCompletedCount(0L);
        row.setOverdueCount(0L);
        when(mapper.selectStatsByDefinition(any())).thenReturn(List.of(row));

        MonitorStatsService svc = new MonitorStatsService(mapper, mock(BpmOrgService.class));
        StatsResponse r = svc.compute(new StatsQuery(), Set.of("byDefinition"));

        StatsByDefinitionRow o = r.getByDefinition().get(0);
        assertThat(o.getCompletionRate()).isEqualTo(0.0);
        assertThat(o.getOverdueRate()).isEqualTo(0.0);
    }

    @Test
    void byNodeComputesOverdueRate() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        StatsByNodeRow row = new StatsByNodeRow();
        row.setNodeId("approve");
        row.setTaskCount(5L);
        row.setOverdueCount(2L);
        when(mapper.selectStatsByNode(any())).thenReturn(List.of(row));

        MonitorStatsService svc = new MonitorStatsService(mapper, mock(BpmOrgService.class));
        StatsResponse r = svc.compute(new StatsQuery(), Set.of("byNode"));

        assertThat(r.getByNode()).hasSize(1);
        assertThat(r.getByNode().get(0).getOverdueRate()).isEqualTo(0.4);
    }

    @Test
    void byApplyDeptEnrichesDeptName() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        BpmOrgService org = mock(BpmOrgService.class);
        StatsByApplyDeptRow row = new StatsByApplyDeptRow();
        row.setApplyDeptId(3L);
        row.setInstanceCount(5L);
        when(mapper.selectStatsByApplyDept(any())).thenReturn(List.of(row));
        when(org.findDeptName(3L)).thenReturn("研发部");

        MonitorStatsService svc = new MonitorStatsService(mapper, org);
        StatsResponse r = svc.compute(new StatsQuery(), Set.of("byApplyDept"));

        assertThat(r.getByApplyDept().get(0).getApplyDeptName()).isEqualTo("研发部");
    }

    @Test
    void nullScopeMeansAll() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        when(mapper.selectStatsByDefinition(any())).thenReturn(Collections.emptyList());
        when(mapper.selectStatsByNode(any())).thenReturn(Collections.emptyList());
        when(mapper.selectStatsByApplyDept(any())).thenReturn(Collections.emptyList());
        when(mapper.selectStatsByApplyDeptOverTime(any())).thenReturn(Collections.emptyList());

        MonitorStatsService svc = new MonitorStatsService(mapper, mock(BpmOrgService.class));
        svc.compute(new StatsQuery(), null);

        verify(mapper).selectStatsByDefinition(any());
        verify(mapper).selectStatsByNode(any());
        verify(mapper).selectStatsByApplyDept(any());
        verify(mapper).selectStatsByApplyDeptOverTime(any());
    }
}
