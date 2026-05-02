package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JeecgBpmUserContextTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void putToken(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Access-Token", token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void noRequestContextReturnsNull() {
        JeecgBpmUserContext ctx = new JeecgBpmUserContext(mock(ISysBaseAPI.class));
        assertThat(ctx.currentUserId()).isNull();
        assertThat(ctx.currentUsername()).isNull();
        assertThat(ctx.currentDeptId()).isNull();
        assertThat(ctx.currentRoleCodes()).isEmpty();
    }

    @Test
    void resolvesUsernameFromJwtAndDelegatesToSysBaseApi() {
        putToken("fake-token");
        ISysBaseAPI api = mock(ISysBaseAPI.class);
        when(api.getDepartIdsByUsername("alice")).thenReturn(Arrays.asList("dep-1"));
        when(api.getRolesByUsername("alice")).thenReturn(Arrays.asList("admin", "approver"));

        try (MockedStatic<JwtUtil> jwt = Mockito.mockStatic(JwtUtil.class)) {
            jwt.when(() -> JwtUtil.getUsername("fake-token")).thenReturn("alice");

            JeecgBpmUserContext ctx = new JeecgBpmUserContext(api);

            assertThat(ctx.currentUsername()).isEqualTo("alice");
            assertThat(ctx.currentUserId()).isNotNull();
            assertThat(ctx.currentDeptId()).isNotNull();
            assertThat(ctx.currentRoleCodes()).containsExactlyInAnyOrder("admin", "approver");
        }
    }
}
