import type { PageDomain, BaseEntity } from "../common";

/** 工单查询参数 */
export interface WorkOrderQueryParams extends PageDomain {
  orderNo?: string;           // 工单编号模糊查询
  deviceName?: string;        // 设备名称（跨表查询）
  deviceId?: number;          // 精确设备ID
  reporterBy?: string;        // 报修人
  assignTo?: string;          // 维修员
  orderStatus?: string;       // 工单状态
  urgencyLevel?: string;      // 紧急程度
  faultType?: string;         // 故障类型
  params?: {
    beginTime?: string;       // 开始时间
    endTime?: string;         // 结束时间
  };
}

/** 工单实体 */
export interface WorkOrder extends BaseEntity {
  orderId?: number;
  orderNo?: string;
  deviceId?: number;
  deviceName?: string;        // 关联设备名称（非数据库字段）
  deviceCode?: string;        // 关联设备编码
  reporterBy?: string;
  reporterName?: string;      // 报修人姓名（非数据库字段）
  faultDesc?: string;
  faultType?: string;
  urgencyLevel?: string;
  orderStatus?: string;
  assignTo?: string;
  assignName?: string;        // 维修员姓名（非数据库字段）
  assignTime?: string;
  finishTime?: string;
  archiveTime?: string;
  archiveBy?: string;
  archiveRemark?: string;
  recordCount?: number;       // 维修记录数（非数据库字段）
}

/** 工单统计 */
export interface WorkOrderStats {
  totalCount: number;         // 当月工单总量
  pendingCount: number;       // 待处理工单数
  inProgressCount: number;    // 维修中工单数
  completedCount: number;     // 已完成工单数
  faultTopDevices: {          // 故障率Top设备
    deviceId: number;
    deviceName: string;
    faultCount: number;
  }[];
}