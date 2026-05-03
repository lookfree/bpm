package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.service.assignee.impl.RoleStrategy;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class RoleStrategyTest {
    BpmOrgService org = Mockito.mock(BpmOrgService.class);
    RoleStrategy s = new RoleStrategy(org);

    @Test
    void resolvesByRoleCode() {
        when(org.findUsersByRole("FINANCE_MANAGER")).thenReturn(List.of(10L, 11L));
        ResolveContext ctx = ResolveContext.builder()
                .strategyPayload(Map.of("roleCode", "FINANCE_MANAGER")).build();
        assertThat(s.resolve(ctx)).containsExactly(10L, 11L);
    }

    @Test
    void missingRoleCodeReturnsEmpty() {
        ResolveContext ctx = ResolveContext.builder().strategyPayload(Map.of()).build();
        assertThat(s.resolve(ctx)).isEmpty();
        Mockito.verifyNoInteractions(org);
    }
}
