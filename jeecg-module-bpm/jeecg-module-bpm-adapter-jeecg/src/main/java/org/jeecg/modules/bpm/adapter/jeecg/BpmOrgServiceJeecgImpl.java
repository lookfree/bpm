package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

public class BpmOrgServiceJeecgImpl implements BpmOrgService {

    private final JdbcTemplate jdbc;

    public BpmOrgServiceJeecgImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> findUsersByRole(String roleCode) {
        return jdbc.queryForList(
                "SELECT ur.user_id FROM sys_user_role ur " +
                "JOIN sys_role r ON r.id = ur.role_id " +
                "WHERE r.role_code = ?",
                Long.class, roleCode);
    }

    @Override
    public List<Long> findDeptLeaders(Long deptId) {
        return jdbc.queryForList(
                "SELECT u.id FROM sys_user_depart ud " +
                "JOIN sys_user u ON u.id = ud.user_id " +
                "WHERE ud.dept_id = ? AND u.is_leader = 1 AND u.status = 1",
                Long.class, deptId);
    }

    @Override
    public List<Long> findUpperDeptLeaders(Long deptId) {
        List<Long> parents = jdbc.queryForList(
                "SELECT parent_id FROM sys_depart WHERE id = ?",
                Long.class, deptId);
        if (parents.isEmpty() || parents.get(0) == null) return Collections.emptyList();
        return findDeptLeaders(parents.get(0));
    }

    @Override
    public List<Long> findUsersByPosition(String positionCode) {
        return jdbc.queryForList(
                "SELECT u.id FROM sys_user u " +
                "JOIN sys_position p ON u.post = p.code " +
                "WHERE p.code = ? AND u.status = 1",
                Long.class, positionCode);
    }

    @Override
    public boolean isUserActive(Long userId) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id = ? AND status = 1",
                Integer.class, userId);
        return cnt != null && cnt > 0;
    }
}
