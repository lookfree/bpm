package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.service.assignee.impl.UpperDeptStrategy;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UpperDeptStrategyTest {
    BpmOrgService org = Mockito.mock(BpmOrgService.class);
    UpperDeptStrategy s = new UpperDeptStrategy(org);

    @Test
    void resolvesUpperDeptLeader() {
        when(org.findUpperDeptLeaders(5L)).thenReturn(List.of(200L));
        ResolveContext ctx = ResolveContext.builder().applyDeptId(5L).build();
        assertThat(s.resolve(ctx)).containsExactly(200L);
    }

    @Test
    void nullDeptIdReturnsEmpty() {
        ResolveContext ctx = ResolveContext.builder().build();
        assertThat(s.resolve(ctx)).isEmpty();
        Mockito.verifyNoInteractions(org);
    }
}
