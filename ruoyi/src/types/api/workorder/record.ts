import type { BaseEntity } from "../common";

/** 维修记录 */
export interface WorkOrderRecord extends BaseEntity {
  recordId?: number;
  orderId?: number;
  repairBy?: string;
  repairName?: string;       // 维修人姓名（非数据库字段）
  repairTime?: string;
  repairSolution?: string;
  partConsumption?: string;
  imageUrls?: string;        // JSON数组字符串
  imageList?: string[];      // 解析后的图片列表（前端使用）
  repairResult?: string;
}