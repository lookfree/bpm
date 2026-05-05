package org.jeecg.modules.bpm.spi;

import java.util.List;

public interface BpmOrgService {
    /** 部门负责人 user id 列表；找不到返回 emptyList() */
    List<Long> findDeptLeaders(Long deptId);

    /** 上级部门负责人 */
    List<Long> findUpperDeptLeaders(Long deptId);

    /** 按角色 code 反查用户 id 列表 */
    List<Long> findUsersByRole(String roleCode);

    /** 按岗位 code 反查 */
    List<Long> findUsersByPosition(String positionCode);

    /** 用户是否存在且启用 */
    boolean isUserActive(Long userId);

    /** 按 userId 取展示名（姓名）；找不到返回 null */
    String findUserName(Long userId);

    /** 按 deptId 取部门名称；找不到返回 null */
    String findDeptName(Long deptId);

    /** 取用户主部门 id；找不到返回 null */
    Long findUserMainDeptId(Long userId);
}
