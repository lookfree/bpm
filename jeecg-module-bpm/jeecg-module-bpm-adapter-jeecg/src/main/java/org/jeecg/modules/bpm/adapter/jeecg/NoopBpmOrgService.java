package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmOrgService;

import java.util.Collections;
import java.util.List;

public class NoopBpmOrgService implements BpmOrgService {
    @Override public List<Long> findDeptLeaders(Long deptId) { return Collections.emptyList(); }
    @Override public List<Long> findUpperDeptLeaders(Long deptId) { return Collections.emptyList(); }
    @Override public List<Long> findUsersByRole(String roleCode) { return Collections.emptyList(); }
    @Override public List<Long> findUsersByPosition(String positionCode) { return Collections.emptyList(); }
    /** P1 stub: 不知道用户状态，安全起见返回 false（避免给可能已停用的用户派活）。P2 走真实查询。 */
    @Override public boolean isUserActive(Long userId) { return false; }
    @Override public String findUserName(Long userId) { return null; }
    @Override public String findDeptName(Long deptId) { return null; }
    @Override public Long findUserMainDeptId(Long userId) { return null; }
}
