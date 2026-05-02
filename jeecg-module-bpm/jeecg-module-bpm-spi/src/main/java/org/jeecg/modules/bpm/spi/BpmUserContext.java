package org.jeecg.modules.bpm.spi;

import java.util.Set;

/**
 * 当前 HTTP/JWT 上下文中的用户信息访问器。
 * <p>未登录或上下文不可用时方法返回 null / 空集合，绝不抛异常。
 */
public interface BpmUserContext {

    /** 当前用户 id；未登录返回 null */
    Long currentUserId();

    /** 当前用户名（jeecg sys_user.username）；未登录返回 null */
    String currentUsername();

    /** 当前用户主部门 id；未登录或无部门返回 null */
    Long currentDeptId();

    /** 当前用户角色 code 集合；未登录返回 emptySet() */
    Set<String> currentRoleCodes();
}
