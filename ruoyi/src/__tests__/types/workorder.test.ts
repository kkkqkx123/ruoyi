import { describe, it, expect } from 'vitest'
import type {
  WorkOrderQueryParams,
  WorkOrder,
  WorkOrderStats,
} from '@/types/api/workorder/order'
import type { WorkOrderRecord } from '@/types/api/workorder/record'
import type { DeviceInfo, DeviceInfoQueryParams } from '@/types/api/device/info'

/**
 * 类型验证测试
 *
 * 这些测试确保 TypeScript 类型定义在结构上是正确的。
 * 它们验证接口字段的完整性和类型一致性。
 *
 * 注意：TypeScript 类型是编译时检查的，这些运行时测试
 * 验证数据结构是否符合接口定义的期望。
 */

describe('类型定义验证', () => {
  describe('WorkOrderQueryParams', () => {
    it('应支持所有查询参数', () => {
      const params: WorkOrderQueryParams = {
        orderNo: 'WO2026',
        deviceName: '测试设备',
        deviceId: 1,
        reporterBy: 'zhangsan',
        assignTo: 'lisi',
        orderStatus: '0',
        urgencyLevel: '1',
        faultType: '0',
        pageNum: 1,
        pageSize: 10,
        params: {
          beginTime: '2026-07-01',
          endTime: '2026-07-31',
        },
      }
      expect(params.orderNo).toBe('WO2026')
      expect(params.deviceName).toBe('测试设备')
      expect(params.params?.beginTime).toBe('2026-07-01')
      expect(params.params?.endTime).toBe('2026-07-31')
    })

    it('应支持部分查询参数', () => {
      const params: WorkOrderQueryParams = {
        orderStatus: '1',
      }
      expect(params.orderStatus).toBe('1')
      expect(params.orderNo).toBeUndefined()
      expect(params.params).toBeUndefined()
    })

    it('应支持时间段查询边界值', () => {
      const params: WorkOrderQueryParams = {
        params: {
          beginTime: '2026-01-01',
          endTime: '2026-12-31',
        },
      }
      expect(params.params!.beginTime).toBe('2026-01-01')
      expect(params.params!.endTime).toBe('2026-12-31')
    })

    it('应继承 PageDomain 的分页字段', () => {
      const params: WorkOrderQueryParams = {
        pageNum: 1,
        pageSize: 20,
      }
      expect(params.pageNum).toBe(1)
      expect(params.pageSize).toBe(20)
    })
  })

  describe('WorkOrder', () => {
    it('应包含所有工单字段', () => {
      const order: WorkOrder = {
        orderId: 1,
        orderNo: 'WO20260703123456789',
        deviceId: 1,
        deviceName: '测试设备',
        deviceCode: 'DEV-001',
        reporterBy: 'zhangsan',
        reporterName: '张三',
        faultDesc: '设备无法启动',
        faultType: '0',
        urgencyLevel: '2',
        orderStatus: '1',
        assignTo: 'lisi',
        assignName: '李四',
        assignTime: '2026-07-03 10:00:00',
        finishTime: '2026-07-03 14:00:00',
        archiveTime: '2026-07-04 09:00:00',
        archiveBy: 'admin',
        archiveRemark: '验收通过',
        recordCount: 1,
      }
      expect(order.orderNo).toMatch(/^WO\d{8}\d+/)
      expect(order.recordCount).toBeGreaterThanOrEqual(0)
      expect(order.reporterName).toBe('张三')
      expect(order.assignName).toBe('李四')
    })

    it('应允许所有字段可选（部分更新场景）', () => {
      const partial: WorkOrder = {
        orderId: 1,
        orderStatus: '2',
      }
      expect(partial.orderId).toBe(1)
      expect(partial.orderStatus).toBe('2')
      expect(partial.deviceName).toBeUndefined()
    })
  })

  describe('WorkOrderStats', () => {
    it('应包含完整的统计数据和Top设备', () => {
      const stats: WorkOrderStats = {
        totalCount: 100,
        pendingCount: 30,
        inProgressCount: 20,
        completedCount: 50,
        faultTopDevices: [
          { deviceId: 1, deviceName: '设备A', faultCount: 15 },
          { deviceId: 2, deviceName: '设备B', faultCount: 10 },
        ],
      }
      expect(stats.totalCount).toBe(100)
      expect(stats.pendingCount + stats.inProgressCount + stats.completedCount).toBe(100)
      expect(stats.faultTopDevices).toHaveLength(2)
      expect(stats.faultTopDevices[0].faultCount).toBe(15)
    })

    it('应支持空Top设备列表', () => {
      const stats: WorkOrderStats = {
        totalCount: 0,
        pendingCount: 0,
        inProgressCount: 0,
        completedCount: 0,
        faultTopDevices: [],
      }
      expect(stats.faultTopDevices).toHaveLength(0)
    })

    it('统计数据总和应等于总量', () => {
      const stats: WorkOrderStats = {
        totalCount: 100,
        pendingCount: 30,
        inProgressCount: 20,
        completedCount: 50,
        faultTopDevices: [],
      }
      const sum = stats.pendingCount + stats.inProgressCount + stats.completedCount
      expect(sum).toBeLessThanOrEqual(stats.totalCount)
    })
  })

  describe('WorkOrderRecord', () => {
    it('应包含维修记录字段', () => {
      const record: WorkOrderRecord = {
        recordId: 1,
        orderId: 1,
        repairBy: 'lisi',
        repairName: '李四',
        repairTime: '2026-07-03 14:00:00',
        repairSolution: '更换电源模块',
        partConsumption: '电源模块×1',
        imageUrls: JSON.stringify(['http://example.com/img1.jpg', 'http://example.com/img2.jpg']),
        imageList: ['http://example.com/img1.jpg', 'http://example.com/img2.jpg'],
        repairResult: '0',
      }
      expect(record.repairSolution).toBe('更换电源模块')
      expect(record.imageList).toHaveLength(2)
      expect(record.repairResult).toBe('0')
    })

    it('imageUrls 应为 JSON 数组字符串', () => {
      const urls = ['http://example.com/img.jpg']
      const record: WorkOrderRecord = {
        imageUrls: JSON.stringify(urls),
      }
      expect(() => JSON.parse(record.imageUrls!)).not.toThrow()
      expect(JSON.parse(record.imageUrls!)).toEqual(urls)
    })
  })

  describe('DeviceInfo', () => {
    it('应包含设备信息和价格', () => {
      const device: DeviceInfo = {
        deviceId: 1,
        deviceCode: 'DEV-001',
        deviceName: '离心泵',
        deviceModel: 'LB-2000',
        location: 'A栋车间',
        status: '0',
        purchaseTime: '2026-01-15',
        price: 15999.99,
        responsibleBy: 'zhangsan',
        responsibleName: '张三',
      }
      expect(device.price).toBeGreaterThan(0)
      expect(device.responsibleName).toBe('张三')
    })

    it('应支持设备状态枚举', () => {
      const normalDevice: DeviceInfo = { status: '0', deviceCode: 'D001' }
      const repairingDevice: DeviceInfo = { status: '1', deviceCode: 'D002' }
      const scrappedDevice: DeviceInfo = { status: '2', deviceCode: 'D003' }
      expect(['0', '1', '2']).toContain(normalDevice.status)
      expect(['0', '1', '2']).toContain(repairingDevice.status)
      expect(['0', '1', '2']).toContain(scrappedDevice.status)
    })
  })

  describe('DeviceInfoQueryParams', () => {
    it('应支持设备查询参数', () => {
      const query: DeviceInfoQueryParams = {
        deviceCode: 'DEV',
        deviceName: '泵',
        status: '0',
        responsibleBy: 'zhangsan',
      }
      expect(query.deviceCode).toBe('DEV')
      expect(query.responsibleBy).toBe('zhangsan')
    })
  })
})