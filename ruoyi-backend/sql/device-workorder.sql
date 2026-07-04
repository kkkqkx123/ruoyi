-- =========================================
-- 设备工单管理模块 — 初始化 SQL
-- 包含：3张业务表 + 字典数据 + 菜单权限
-- =========================================

-- 1. 设备信息表
CREATE TABLE IF NOT EXISTS device_info (
  device_id       bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  device_code     varchar(64)   NOT NULL COMMENT '设备编码',
  device_name     varchar(100)  NOT NULL COMMENT '设备名称',
  device_model    varchar(100)  DEFAULT NULL COMMENT '设备型号',
  location        varchar(255)  DEFAULT NULL COMMENT '安装位置',
  status          char(1)       DEFAULT '0' COMMENT '状态（0正常 1维修中 2报废）',
  purchase_time   datetime      DEFAULT NULL COMMENT '采购时间',
  price           decimal(10,2) DEFAULT NULL COMMENT '采购价格',
  responsible_by  varchar(64)   DEFAULT NULL COMMENT '负责人',
  remark          varchar(500)  DEFAULT NULL COMMENT '备注',
  create_by       varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time     datetime      DEFAULT NULL COMMENT '创建时间',
  update_by       varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time     datetime      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (device_id),
  UNIQUE KEY uk_device_code (device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- 2. 工单主表
CREATE TABLE IF NOT EXISTS work_order (
  order_id       bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  order_no       varchar(32)   NOT NULL COMMENT '工单编号',
  device_id      bigint(20)    NOT NULL COMMENT '设备ID',
  reporter_by    varchar(64)   NOT NULL COMMENT '报修人',
  fault_desc     varchar(1000) NOT NULL COMMENT '故障描述',
  fault_type     varchar(64)   DEFAULT NULL COMMENT '故障类型',
  urgency_level  char(1)       DEFAULT '1' COMMENT '紧急程度（1普通 2紧急 3特急）',
  order_status   char(1)       DEFAULT '0' COMMENT '工单状态（0未派单 1已派单 2维修中 3已完成 4已归档）',
  assign_to      varchar(64)   DEFAULT NULL COMMENT '维修员',
  assign_time    datetime      DEFAULT NULL COMMENT '派单时间',
  finish_time    datetime      DEFAULT NULL COMMENT '完成时间',
  archive_time   datetime      DEFAULT NULL COMMENT '归档时间',
  archive_by     varchar(64)   DEFAULT NULL COMMENT '归档人',
  archive_remark varchar(500)  DEFAULT NULL COMMENT '归档备注',
  create_by      varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time    datetime      DEFAULT NULL COMMENT '创建时间',
  update_by      varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time    datetime      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_device_id (device_id),
  KEY idx_order_status (order_status),
  KEY idx_assign_to (assign_to),
  KEY idx_reporter_by (reporter_by),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';

-- 3. 工单维修记录表
CREATE TABLE IF NOT EXISTS work_order_record (
  record_id        bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  order_id         bigint(20)    NOT NULL COMMENT '工单ID',
  repair_by        varchar(64)   NOT NULL COMMENT '维修人',
  repair_time      datetime      NOT NULL COMMENT '维修时间',
  repair_solution  varchar(2000) NOT NULL COMMENT '维修方案',
  part_consumption varchar(500)  DEFAULT NULL COMMENT '配件消耗',
  image_urls       varchar(2000) DEFAULT NULL COMMENT '图片附件',
  repair_result    char(1)       DEFAULT '0' COMMENT '维修结果（0已修复 1部分修复 2无法修复）',
  remark           varchar(500)  DEFAULT NULL COMMENT '备注',
  create_by        varchar(64)   DEFAULT '' COMMENT '创建者',
  create_time      datetime      DEFAULT NULL COMMENT '创建时间',
  update_by        varchar(64)   DEFAULT '' COMMENT '更新者',
  update_time      datetime      DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (record_id),
  KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单维修记录表';

-- =========================================
-- 字典数据初始化
-- =========================================
-- 查询字典类型ID的SQL（执行前先检查是否已有）：
-- SELECT dict_type, dict_id FROM sys_dict_type WHERE dict_type IN ('work_order_status','work_order_urgency','work_order_repair_result','work_order_fault_type');

-- 字典类型
INSERT IGNORE INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time)
VALUES ('工单状态', 'work_order_status', '0', 'admin', NOW()),
       ('紧急程度', 'work_order_urgency', '0', 'admin', NOW()),
       ('维修结果', 'work_order_repair_result', '0', 'admin', NOW()),
       ('故障类型', 'work_order_fault_type', '0', 'admin', NOW());

-- 字典数据（work_order_status）
INSERT IGNORE INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, create_by, create_time)
VALUES (1, '未派单', '0', 'work_order_status', 'default', 'admin', NOW()),
       (2, '已派单', '1', 'work_order_status', 'primary', 'admin', NOW()),
       (3, '维修中', '2', 'work_order_status', 'warning', 'admin', NOW()),
       (4, '已完成', '3', 'work_order_status', 'success', 'admin', NOW()),
       (5, '已归档', '4', 'work_order_status', 'info', 'admin', NOW());

-- 字典数据（work_order_urgency）
INSERT IGNORE INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, create_by, create_time)
VALUES (1, '普通', '1', 'work_order_urgency', 'default', 'admin', NOW()),
       (2, '紧急', '2', 'work_order_urgency', 'warning', 'admin', NOW()),
       (3, '特急', '3', 'work_order_urgency', 'danger', 'admin', NOW());

-- 字典数据（work_order_repair_result）
INSERT IGNORE INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, create_by, create_time)
VALUES (1, '已修复', '0', 'work_order_repair_result', 'success', 'admin', NOW()),
       (2, '部分修复', '1', 'work_order_repair_result', 'warning', 'admin', NOW()),
       (3, '无法修复', '2', 'work_order_repair_result', 'danger', 'admin', NOW());

-- 字典数据（work_order_fault_type）
INSERT IGNORE INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, create_by, create_time)
VALUES (1, '机械故障', '0', 'work_order_fault_type', 'default', 'admin', NOW()),
       (2, '电气故障', '1', 'work_order_fault_type', 'default', 'admin', NOW()),
       (3, '软件故障', '2', 'work_order_fault_type', 'default', 'admin', NOW()),
       (4, '网络故障', '3', 'work_order_fault_type', 'default', 'admin', NOW()),
       (5, '其他', '4', 'work_order_fault_type', 'default', 'admin', NOW());

-- =========================================
-- 菜单与权限初始化
-- =========================================
-- 注意：parent_id 需要根据实际数据库调整
-- 以下假设 sys_menu 中已有父菜单ID，实际执行时请替换
-- 建议先查询：SELECT menu_id, menu_name FROM sys_menu WHERE menu_name IN ('系统管理', '设备工单管理');

-- 设备工单管理（一级菜单目录）
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('设备工单管理', 0, 5, '/workorder', NULL, 1, 0, 'M', '0', '0', NULL, 'tool', 'admin', NOW());

-- 工单列表（菜单页面）
-- 获取刚插入的设备工单管理菜单ID作为parent_id
-- 实际执行时需要替换 @workorder_parent_id 为真实ID
-- SELECT @workorder_parent_id := menu_id FROM sys_menu WHERE menu_name = '设备工单管理' AND parent_id = 0;

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('工单列表', 2000, 1, 'order', 'workorder/order/index', 1, 0, 'C', '0', '0', 'workorder:order:list', 'list', 'admin', NOW()),
       ('工单详情', 2000, 2, 'order/detail', 'workorder/order/detail', 1, 0, 'C', '0', '0', 'workorder:order:list', 'edit', 'admin', NOW()),
       ('维修记录', 2000, 3, 'record', 'workorder/record/index', 1, 0, 'C', '0', '0', 'workorder:record:list', 'log', 'admin', NOW()),
       ('设备管理', 2000, 4, 'device', 'device/info/index', 1, 0, 'C', '0', '0', 'device:info:list', 'build', 'admin', NOW());

-- 工单管理按钮权限
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('工单新增', 2000, 1, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:order:add', '#', 'admin', NOW()),
       ('工单修改', 2000, 2, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:order:edit', '#', 'admin', NOW()),
       ('工单删除', 2000, 3, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:order:remove', '#', 'admin', NOW()),
       ('工单派单', 2000, 4, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:order:assign', '#', 'admin', NOW()),
       ('工单归档', 2000, 5, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:order:archive', '#', 'admin', NOW()),
       ('工单导出', 2000, 6, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:order:export', '#', 'admin', NOW()),
       ('工单统计', 2000, 7, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:order:stats', '#', 'admin', NOW()),
       ('记录新增', 2000, 8, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:record:add', '#', 'admin', NOW()),
       ('记录删除', 2000, 9, '#', NULL, 1, 0, 'F', '0', '0', 'workorder:record:remove', '#', 'admin', NOW());

-- 设备信息菜单（独立目录 + 列表 + 权限）
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('设备管理', 0, 4, '/device', NULL, 1, 0, 'M', '0', '0', NULL, 'build', 'admin', NOW());
-- 假设设备管理父菜单ID为2001（需替换）
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('设备列表', 2001, 1, 'info', 'device/info/index', 1, 0, 'C', '0', '0', 'device:info:list', '#', 'admin', NOW());

-- 注意：以上parent_id(2000/2001)为占位值
-- 执行前请查询真实ID：
-- SET @workorder_parent = (SELECT menu_id FROM sys_menu WHERE menu_name = '设备工单管理' AND parent_id = 0);
-- SET @device_parent = (SELECT menu_id FROM sys_menu WHERE menu_name = '设备管理' AND parent_id = 0);
-- 然后 replace 2000 -> @workorder_parent, 2001 -> @device_parent
-- 或者在执行前先运行菜单插入并回填ID