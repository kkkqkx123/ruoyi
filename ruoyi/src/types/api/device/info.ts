import type { PageDomain, BaseEntity } from "../common";

/** 设备查询参数 */
export interface DeviceInfoQueryParams extends PageDomain {
  deviceCode?: string;       // 设备编码
  deviceName?: string;       // 设备名称
  status?: string;           // 设备状态
  responsibleBy?: string;    // 负责人
}

/** 设备信息 */
export interface DeviceInfo extends BaseEntity {
  deviceId?: number;
  deviceCode?: string;
  deviceName?: string;
  deviceModel?: string;
  location?: string;
  status?: string;           // 0正常 1维修中 2报废
  purchaseTime?: string;
  price?: number;
  responsibleBy?: string;
  responsibleName?: string;  // 负责人姓名（非数据库字段）
}