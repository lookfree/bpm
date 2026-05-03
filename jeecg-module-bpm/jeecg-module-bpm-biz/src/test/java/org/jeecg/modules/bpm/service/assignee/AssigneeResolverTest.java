package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.jeecg.modules.bpm.service.assignee.impl.FixedUserStrategy;
import org.jeecg.modules.bpm.service.assignee.impl.RoleStrategy;
import org.jeecg.modules.bpm.service.assignee.impl.ScriptStrategy;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssigneeResolverTest {
    BpmOrgService org = Mockito.mock(BpmOrgService.class);
    AssigneeResolver resolver;

    @BeforeEach
    void setup() {
        Map<String, AssigneeStrategy> map = new HashMap<>();
        FixedUserStrategy fixed = new FixedUserStrategy();
        RoleStrategy role = new RoleStrategy(org);
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        ScriptStrategy script = new ScriptStrategy(evaluator, ctxBuilder);
        map.put(fixed.type(), fixed);
        map.put(role.type(), role);
        map.put(script.type(), script);
        resolver = new AssigneeResolver(map, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void dispatchesByTypeJSON() {
        String json = "{\"type\":\"USER\",\"payload\":{\"userIds\":[1,2]}}";
        assertThat(resolver.resolve(json, ResolveContext.builder().build())).containsExactly(1L, 2L);
    }

    @Test
    void dispatchesRoleAndCallsOrgService() {
        when(org.findUsersByRole("R1")).thenReturn(java.util.List.of(7L));
        assertThat(resolver.resolve("{\"type\":\"ROLE\",\"payload\":{\"roleCode\":\"R1\"}}", ResolveContext.builder().build())).containsExactly(7L);
    }

    @Test
    void scriptStubReturnsEmpty() {
        assertThat(resolver.resolve("{\"type\":\"SCRIPT\",\"payload\":{\"script\":\"x\"}}", ResolveContext.builder().build())).isEmpty();
    }

    @Test
    void unknownTypeReturnsEmpty() {
        assertThat(resolver.resolve("{\"type\":\"UNKNOWN\",\"payload\":{}}", ResolveContext.builder().build())).isEmpty();
    }

    @Test
    void nullJsonReturnsEmpty() {
        assertThat(resolver.resolve(null, ResolveContext.builder().build())).isEmpty();
    }

    @Test
    void resolveDeduplicates() {
        assertThat(resolver.resolve("{\"type\":\"USER\",\"payload\":{\"userIds\":[1,1,2]}}", ResolveContext.builder().build())).containsExactly(1L, 2L);
    }
}
