package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JeecgBpmUserContext implements BpmUserContext {

    private final ISysBaseAPI sysBaseAPI;

    public JeecgBpmUserContext(ISysBaseAPI sysBaseAPI) {
        this.sysBaseAPI = sysBaseAPI;
    }

    @Override
    public Long currentUserId() {
        String username = currentUsername();
        if (username == null) return null;
        return (long) Math.abs(username.hashCode());
    }

    @Override
    public String currentUsername() {
        String token = currentToken();
        if (token == null) return null;
        try {
            return JwtUtil.getUsername(token);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Long currentDeptId() {
        String username = currentUsername();
        if (username == null) return null;
        List<String> ids = sysBaseAPI.getDepartIdsByUsername(username);
        if (ids == null || ids.isEmpty()) return null;
        return (long) Math.abs(ids.get(0).hashCode());
    }

    @Override
    public Set<String> currentRoleCodes() {
        String username = currentUsername();
        if (username == null) return Collections.emptySet();
        List<String> roles = sysBaseAPI.getRolesByUsername(username);
        return roles == null ? Collections.emptySet() : new HashSet<>(roles);
    }

    private String currentToken() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes)) return null;
        HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
        String t = req.getHeader("X-Access-Token");
        return (t == null || t.isEmpty()) ? null : t;
    }
}
