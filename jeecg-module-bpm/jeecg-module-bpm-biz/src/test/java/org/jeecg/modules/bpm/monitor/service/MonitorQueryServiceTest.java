package org.jeecg.modules.bpm.monitor.service;

import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceQuery;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceVO;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MonitorQueryServiceTest {

    @Test
    void listInstancesEnrichesWithUserAndDeptNames() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        BpmOrgService org = mock(BpmOrgService.class);

        MonitorInstanceVO row = new MonitorInstanceVO();
        row.setId("inst1");
        row.setApplyUserId(7L);
        row.setApplyDeptId(3L);
        row.setStartTime(LocalDateTime.now());
        when(mapper.selectInstances(any(), anyInt(), anyInt()))
                .thenReturn(Collections.singletonList(row));
        when(mapper.countInstances(any())).thenReturn(1L);
        when(org.findUserName(7L)).thenReturn("alice");
        when(org.findDeptName(3L)).thenReturn("研发部");

        MonitorQueryService svc = new MonitorQueryService(mapper, org);
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        Map<String, Object> page = svc.listInstances(q);

        assertThat(page.get("total")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<MonitorInstanceVO> list = (List<MonitorInstanceVO>) page.get("records");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getApplyUserName()).isEqualTo("alice");
        assertThat(list.get(0).getApplyDeptName()).isEqualTo("研发部");
    }

    @Test
    void listInstancesDefaultPageSize() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        BpmOrgService org = mock(BpmOrgService.class);
        when(mapper.selectInstances(any(), eq(0), eq(20))).thenReturn(Collections.emptyList());
        when(mapper.countInstances(any())).thenReturn(0L);

        MonitorQueryService svc = new MonitorQueryService(mapper, org);
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        Map<String, Object> page = svc.listInstances(q);

        assertThat(page.get("pageSize")).isEqualTo(20);
        assertThat(page.get("pageNo")).isEqualTo(1);
    }

    @Test
    void listInstancesSkipsNullUserAndDept() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        BpmOrgService org = mock(BpmOrgService.class);

        MonitorInstanceVO row = new MonitorInstanceVO();
        row.setId("inst2");
        when(mapper.selectInstances(any(), anyInt(), anyInt()))
                .thenReturn(Collections.singletonList(row));
        when(mapper.countInstances(any())).thenReturn(1L);

        MonitorQueryService svc = new MonitorQueryService(mapper, org);
        svc.listInstances(new MonitorInstanceQuery());

        verify(org, never()).findUserName(any());
        verify(org, never()).findDeptName(any());
    }
}
