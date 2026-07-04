-- ============================================================
-- 性能优化 - 联合索引 DDL
-- 执行日期：2026-07-03
-- 适用项目：RuoYi-Vue 设备工单模块
-- 注意：请先在测试环境执行 EXPLAIN 验证执行计划，再上生产
-- ============================================================

-- 1. 工单列表最常用查询模式：按时间倒序 + 状态筛选
--    覆盖场景：首页列表、状态筛选、时间段筛选
--    说明：order_status 在前（等值筛选），create_time 在后（排序）
ALTER TABLE work_order ADD INDEX idx_status_create_time (order_status, create_time DESC);

-- 2. 工单统计看板：当月数据按设备分组 + 状态聚合
--    覆盖场景：故障排行 Top10、设备维度统计
ALTER TABLE work_order ADD INDEX idx_device_create_time (device_id, create_time);

-- 3. 设备表：状态 + 名称组合查询（前端的设备选择器）
ALTER TABLE device_info ADD INDEX idx_status_device_name (device_status, device_name);

-- 4. 工单记录表：按工单 ID 查询维修记录时间线
--    将原有单列索引 idx_order_id 升级为联合索引，覆盖时间排序，避免文件排序
ALTER TABLE work_order_record DROP INDEX idx_order_id;
ALTER TABLE work_order_record ADD INDEX idx_order_create_time (order_id, create_time DESC);

-- ============================================================
-- 验证索引是否生效
-- ============================================================
-- EXPLAIN SELECT wo.* FROM work_order wo
-- WHERE wo.order_status = '0'
-- ORDER BY wo.create_time DESC
-- LIMIT 20;
-- 预期：type=ref, key=idx_status_create_time, Extra=Using index condition
--
-- EXPLAIN SELECT COUNT(*) FROM work_order
-- WHERE order_status = '0' AND create_time >= '2026-01-01';
-- 预期：type=range, key=idx_status_create_time, Extra=Using where; Using index

-- ============================================================
-- 导出任务记录表（异步导出优化）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_export_task (
    task_id      BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    task_name    VARCHAR(100)  NOT NULL COMMENT '任务名称',
    module       VARCHAR(50)   NOT NULL COMMENT '所属模块（workorder/user/...）',
    query_params JSON          COMMENT '查询参数（JSON 序列化）',
    file_path    VARCHAR(500)  COMMENT '导出文件路径',
    file_size    BIGINT        COMMENT '文件大小（字节）',
    status       CHAR(1)       NOT NULL DEFAULT '0' COMMENT '状态（0待处理 1处理中 2已完成 3失败）',
    error_msg    VARCHAR(2000) COMMENT '错误信息',
    create_by    VARCHAR(64)   COMMENT '创建者',
    create_time  DATETIME      COMMENT '创建时间',
    update_time  DATETIME      COMMENT '更新时间',
    KEY idx_status (status),
    KEY idx_create_by (create_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务记录表';