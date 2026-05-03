-- Minimal jeecg tables for BpmOrgService tests

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    is_leader TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    post VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_depart (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT
);

CREATE TABLE IF NOT EXISTS sys_user_depart (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_position (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL
);

-- Roles
INSERT INTO sys_role(id, role_code) VALUES(10, 'admin'), (11, 'auditor');

-- Users
INSERT INTO sys_user(id, is_leader, status, post) VALUES
    (1, 0, 1, NULL),
    (2, 0, 1, NULL),
    (3, 0, 1, NULL),
    (4, 1, 1, NULL),
    (7, 0, 1, 'PM'),
    (9, 1, 1, NULL);

-- User-role
INSERT INTO sys_user_role(id, user_id, role_id) VALUES
    (1, 1, 10), (2, 2, 10), (3, 3, 11);

-- Departments: 100 is child of 50
INSERT INTO sys_depart(id, parent_id) VALUES(50, NULL), (100, 50);

-- User-depart: 9 in dept 100, 4 in dept 50
INSERT INTO sys_user_depart(id, user_id, dept_id) VALUES
    (1, 9, 100), (2, 4, 50);

-- Position
INSERT INTO sys_position(id, code) VALUES(1, 'PM');
