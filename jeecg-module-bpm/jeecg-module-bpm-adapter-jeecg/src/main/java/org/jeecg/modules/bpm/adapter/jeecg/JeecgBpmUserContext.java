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

/**
 * jeecg-boot v3.5.5 实现：从 X-Access-Token 头取 token，{@link JwtUtil} 解出 username，
 * 再调 {@link ISysBaseAPI} 反查角色与部门。
 *
 * <p><b>P1 stub: userId/deptId 长整型转换。</b>jeecg sys_user.id 是 String UUID，
 * 但 BPM SPI 统一 Long。当前用 {@code Math.abs(String.hashCode())} 做稳定映射 —
 * 存在哈希冲突风险，仅适用于 P1 单租户/小规模场景。
 * P2 必须切换到 adapter 维护的 {@code bpm_user_id_mapping} 双向表（TODO-P2）。
 *
 * <p><b>currentDeptId 取首部门：</b>jeecg 用户可能属多部门，当前返回 list 第一项；
 * P2 需走 sys_user.org_code 主部门标记 (TODO-P2)。
 */
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
