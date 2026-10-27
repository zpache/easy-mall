-- ============================================================
-- 动态菜单：登录后菜单/路由完全由后端下发（vben accessMode = backend）
-- 工作台（Dashboard）也从菜单表读取，保证默认首页 /analytics 有对应路由
-- 幂等：可重复执行
-- ============================================================

INSERT IGNORE INTO `sys_menu` (`id`, `pid`, `name`, `title`, `type`, `auth_code`, `path`, `component`, `icon`,
                                `order_no`, `status`, `affix_tab`)
VALUES (10, 0, 'Dashboard', '工作台', 'catalog', NULL, '/dashboard', NULL, 'lucide:layout-dashboard', -1, 1, 0),
       (11, 10, 'Analytics', '分析页', 'menu', 'dashboard:analytics', '/analytics', '/dashboard/analytics/index',
        'lucide:area-chart', 1, 1, 1),
       (12, 10, 'Workspace', '工作台', 'menu', 'dashboard:workspace', '/workspace', '/dashboard/workspace/index',
        'carbon:workspace', 2, 1, 0);

-- 超级管理员：工作台 + 系统管理全部
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 10),
       (1, 11),
       (1, 12);

-- 运营人员：仅工作台（用于验证不同角色登录后菜单不同）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 10),
       (2, 11),
       (2, 12);
