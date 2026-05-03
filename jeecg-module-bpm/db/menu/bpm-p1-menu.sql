-- BPM P1 菜单与权限点（注入到 jeecg sys_permission）
-- ID 用 UUID（jeecg sys_permission.id 是 varchar(32)）

-- 顶级菜单
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time)
VALUES('bpm-root-2026p1', NULL, '流程配置', '/bpm', 'layouts/RouteView', 'BPM', '/bpm/definition', 0, NULL, '1', 100, 1, 'ant-design:partition-outlined', 1, 0, 1, 0, 0, 'BPM 流程配置 P1', 1, 0, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), url=VALUES(url), update_time=NOW();

-- 二级 流程定义
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, menu_type, perms, perms_type, sort_no, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, status, del_flag, rule_flag, create_by, create_time)
VALUES('bpm-definition-2026p1', 'bpm-root-2026p1', '流程定义', '/bpm/definition', 'views/bpm/definition/DefinitionList', 'BpmDefinition', 1, NULL, '1', 110, 'ant-design:unordered-list-outlined', 1, 1, 1, 0, 0, 1, 0, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), update_time=NOW();

-- 二级（隐藏）流程设计器
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, menu_type, perms, perms_type, sort_no, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, status, del_flag, rule_flag, create_by, create_time)
VALUES('bpm-designer-2026p1', 'bpm-root-2026p1', '流程设计器', '/bpm/designer', 'views/bpm/designer/DesignerPage', 'BpmDesigner', 1, NULL, '1', 120, 'ant-design:edit-outlined', 1, 1, 0, 1, 0, 1, 0, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), update_time=NOW();

-- 权限点
INSERT INTO sys_permission(id, parent_id, name, perms, perms_type, menu_type, sort_no, status, del_flag, create_by, create_time) VALUES
 ('bpm-perm-def-view-2026p1',    'bpm-definition-2026p1', '查看流程定义',  'bpm:definition:view',    '2', 2, 1, 1, 0, 'admin', NOW()),
 ('bpm-perm-def-edit-2026p1',    'bpm-definition-2026p1', '编辑流程定义',  'bpm:definition:edit',    '2', 2, 2, 1, 0, 'admin', NOW()),
 ('bpm-perm-def-publish-2026p1', 'bpm-definition-2026p1', '发布流程定义',  'bpm:definition:publish', '2', 2, 3, 1, 0, 'admin', NOW()),
 ('bpm-perm-def-delete-2026p1',  'bpm-definition-2026p1', '删除流程定义',  'bpm:definition:delete',  '2', 2, 4, 1, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), update_time=NOW();

-- 给 admin 角色（jeecg 默认）授权
INSERT IGNORE INTO sys_role_permission(id, role_id, permission_id, create_time)
SELECT UUID_SHORT() AS id, r.id, p.id, NOW()
FROM sys_role r, sys_permission p
WHERE r.role_code = 'admin'
  AND p.id IN ('bpm-root-2026p1','bpm-definition-2026p1','bpm-designer-2026p1',
               'bpm-perm-def-view-2026p1','bpm-perm-def-edit-2026p1',
               'bpm-perm-def-publish-2026p1','bpm-perm-def-delete-2026p1');
