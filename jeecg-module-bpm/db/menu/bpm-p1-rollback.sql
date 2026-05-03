-- 反向：按 perm_code/id 后缀 -2026p1 删
DELETE FROM sys_role_permission WHERE permission_id LIKE 'bpm-%-2026p1';
DELETE FROM sys_permission WHERE id LIKE 'bpm-%-2026p1';
