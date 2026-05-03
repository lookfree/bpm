package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.service.assignee.impl.DeptLeaderStrategy;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class DeptLeaderStrategyTest {
    BpmOrgService org = Mockito.mock(BpmOrgService.class);
    DeptLeaderStrategy s = new DeptLeaderStrategy(org);

    @Test
    void resolvesDeptLeader() {
        when(org.findDeptLeaders(5L)).thenReturn(List.of(100L));
        ResolveContext ctx = ResolveContext.builder().applyDeptId(5L).build();
        assertThat(s.resolve(ctx)).containsExactly(100L);
    }

    @Test
    void nullDeptIdReturnsEmpty() {
        ResolveContext ctx = ResolveContext.builder().build();
        assertThat(s.resolve(ctx)).isEmpty();
        Mockito.verifyNoInteractions(org);
    }
}
