package org.jeecg.modules.bpm.support;

import org.jeecg.modules.bpm.spi.BpmUserContext;

import java.util.Collections;
import java.util.Set;

/**
 * 兜底实现：当宿主未提供 BpmUserContext 时（典型场景：测试 / 非 jeecg 集成且未实现 adapter）
 * 加载本 fallback，所有方法返回 null/empty。生产由 adapter-jeecg 覆盖。
 */
public class NoOpBpmUserContext implements BpmUserContext {
    @Override public Long currentUserId() { return null; }
    @Override public String currentUsername() { return null; }
    @Override public Long currentDeptId() { return null; }
    @Override public Set<String> currentRoleCodes() { return Collections.emptySet(); }
}
