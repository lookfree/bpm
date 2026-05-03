package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.service.assignee.impl.FormFieldStrategy;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class FormFieldStrategyTest {
    FormFieldStrategy s = new FormFieldStrategy();

    @Test
    void picksUserIdsFromFormDataByFieldName() {
        ResolveContext ctx = ResolveContext.builder()
                .strategyPayload(Map.of("fieldName", "projectManager"))
                .formData(Map.of("projectManager", List.of(101, 102))).build();
        assertThat(s.resolve(ctx)).containsExactly(101L, 102L);
    }

    @Test
    void singleScalarValueAccepted() {
        ResolveContext ctx = ResolveContext.builder()
                .strategyPayload(Map.of("fieldName", "owner"))
                .formData(Map.of("owner", 999L)).build();
        assertThat(s.resolve(ctx)).containsExactly(999L);
    }

    @Test
    void missingFieldReturnsEmpty() {
        ResolveContext ctx = ResolveContext.builder()
                .strategyPayload(Map.of("fieldName", "absent"))
                .formData(Map.of()).build();
        assertThat(s.resolve(ctx)).isEmpty();
    }
}
