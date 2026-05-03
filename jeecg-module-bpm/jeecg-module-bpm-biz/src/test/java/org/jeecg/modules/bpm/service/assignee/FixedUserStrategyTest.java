package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.domain.enums.AssigneeStrategyType;
import org.jeecg.modules.bpm.service.assignee.impl.FixedUserStrategy;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class FixedUserStrategyTest {
    private final FixedUserStrategy s = new FixedUserStrategy();

    @Test
    void typeIsUSER() {
        assertThat(s.type()).isEqualTo(AssigneeStrategyType.USER.name());
    }

    @Test
    void resolvesUserIdsFromPayload() {
        ResolveContext ctx = ResolveContext.builder()
                .strategyPayload(Map.of("userIds", List.of(1L, 2L, 3L))).build();
        assertThat(s.resolve(ctx)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void emptyPayloadReturnsEmptyList() {
        ResolveContext ctx = ResolveContext.builder().strategyPayload(Map.of()).build();
        assertThat(s.resolve(ctx)).isEmpty();
    }
}
