package org.jeecg.modules.bpm.expression;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmExpressionContextBuilderTest {

    @Test
    void buildsFormSysUserMaps() {
        BpmUserContext userCtx = mock(BpmUserContext.class);
        BpmOrgService  orgSvc  = mock(BpmOrgService.class);
        BpmFormService formSvc = mock(BpmFormService.class);

        when(userCtx.currentUserId()).thenReturn(7L);
        when(userCtx.currentDeptId()).thenReturn(99L);
        when(userCtx.currentRoleCodes()).thenReturn(Set.of("admin", "approver"));
        when(formSvc.loadFormData("form_purchase", "biz_001"))
                .thenReturn(Map.of("amount", 12345, "title", "buy laptop"));

        Clock clock = Clock.fixed(Instant.parse("2026-05-02T10:00:00Z"), ZoneId.of("UTC"));
        BpmExpressionContextBuilder builder = new BpmExpressionContextBuilder(userCtx, orgSvc, formSvc, clock);
        Map<String,Object> ctx = builder.build("form_purchase", "biz_001");

        assertThat(ctx).containsKeys("form", "sys", "user");

        @SuppressWarnings("unchecked")
        Map<String,Object> form = (Map<String,Object>) ctx.get("form");
        assertThat(form).containsEntry("amount", 12345).containsEntry("title", "buy laptop");

        @SuppressWarnings("unchecked")
        Map<String,Object> sys = (Map<String,Object>) ctx.get("sys");
        assertThat(sys).containsKey("now").containsEntry("today", LocalDate.of(2026, 5, 2));

        @SuppressWarnings("unchecked")
        Map<String,Object> user = (Map<String,Object>) ctx.get("user");
        assertThat(user).containsEntry("id", 7L).containsEntry("deptId", 99L);
        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) user.get("roles");
        assertThat(roles).containsExactlyInAnyOrder("admin", "approver");
    }

    @Test
    void formMapEmptyWhenFormIdNull() {
        BpmExpressionContextBuilder builder = new BpmExpressionContextBuilder(
                mock(BpmUserContext.class), mock(BpmOrgService.class), mock(BpmFormService.class),
                Clock.systemUTC());
        Map<String,Object> ctx = builder.build(null, null);
        @SuppressWarnings("unchecked")
        Map<?,?> form = (Map<?,?>) ctx.get("form");
        assertThat(form).isEmpty();
    }
}
