package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmOrgService;

import java.util.Collections;
import java.util.List;

public class NoopBpmOrgService implements BpmOrgService {
    @Override public List<Long> findDeptLeaders(Long deptId) { return Collections.emptyList(); }
    @Override public List<Long> findUpperDeptLeaders(Long deptId) { return Collections.emptyList(); }
    @Override public List<Long> findUsersByRole(String roleCode) { return Collections.emptyList(); }
    @Override public List<Long> findUsersByPosition(String positionCode) { return Collections.emptyList(); }
    @Override public boolean isUserActive(Long userId) { return userId != null; }
}
