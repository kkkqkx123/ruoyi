import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  listOrder,
  getOrder,
  addOrder,
  updateOrder,
  delOrder,
  batchAssignOrder,
  exportOrder,
  getWorkOrderStats,
} from '@/api/workorder/order'

// mock request 模块
vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => {
    // 返回模拟响应
    if (config.url?.includes('/stats')) {
      return Promise.resolve({
        code: 200,
        data: {
          totalCount: 100,
          pendingCount: 30,
          inProgressCount: 20,
          completedCount: 50,
          faultTopDevices: [
            { deviceId: 1, deviceName: '设备A', faultCount: 10 },
          ],
        },
      })
    }
    if (config.url?.includes('/batchAssign')) {
      return Promise.resolve({ code: 200, msg: '操作成功' })
    }
    if (config.url?.includes('/export')) {
      return Promise.resolve(new Blob(['csv content'], { type: 'application/vnd.ms-excel' }))
    }
    if (config.method === 'get' && config.url?.includes('/list')) {
      return Promise.resolve({
        code: 200,
        rows: [
          {
            orderId: 1,
            orderNo: 'WO20260703123456789',
            deviceName: '测试设备',
            orderStatus: '0',
          },
        ],
        total: 1,
      })
    }
    if (config.method === 'get' && config.url?.match(/\/workorder\/order\/\d+/)) {
      return Promise.resolve({
        code: 200,
        data: { orderId: 1, orderNo: 'WO20260703123456789' },
      })
    }
    if (config.method === 'post' || config.method === 'put') {
      return Promise.resolve({ code: 200, msg: '操作成功' })
    }
    if (config.method === 'delete') {
      return Promise.resolve({ code: 200, msg: '删除成功' })
    }
    return Promise.resolve({ code: 200 })
  }),
}))

describe('WorkOrder API', () => {
  describe('listOrder', () => {
    it('应返回分页工单列表', async () => {
      const result = await listOrder({ orderStatus: '0' })
      expect(result.code).toBe(200)
      expect(result.rows).toHaveLength(1)
      expect(result.rows[0].orderNo).toBe('WO20260703123456789')
      expect(result.total).toBe(1)
    })

    it('应传入查询参数', async () => {
      const query = { orderNo: 'WO2026', deviceName: '设备', orderStatus: '0' }
      const { default: request } = await import('@/utils/request')
      await listOrder(query)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/order/list',
          method: 'get',
          params: query,
        })
      )
    })

    it('应支持时间段筛选参数', async () => {
      const query = {
        params: { beginTime: '2026-07-01', endTime: '2026-07-31' },
      }
      const { default: request } = await import('@/utils/request')
      await listOrder(query)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          params: expect.objectContaining({
            params: { beginTime: '2026-07-01', endTime: '2026-07-31' },
          }),
        })
      )
    })
  })

  describe('getOrder', () => {
    it('应返回工单详情', async () => {
      const result = await getOrder(1)
      expect(result.code).toBe(200)
      expect(result.data.orderNo).toBe('WO20260703123456789')
    })

    it('应传入正确ID', async () => {
      const { default: request } = await import('@/utils/request')
      await getOrder(999)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/order/999',
          method: 'get',
        })
      )
    })
  })

  describe('addOrder', () => {
    it('应成功创建工单', async () => {
      const orderData = {
        deviceId: 1,
        faultDesc: '设备故障',
        urgencyLevel: '1',
      }
      const result = await addOrder(orderData)
      expect(result.code).toBe(200)
    })

    it('应传入工单数据到POST请求', async () => {
      const orderData = { deviceId: 1, faultDesc: '测试' }
      const { default: request } = await import('@/utils/request')
      await addOrder(orderData)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/order',
          method: 'post',
          data: orderData,
        })
      )
    })
  })

  describe('updateOrder', () => {
    it('应成功修改工单', async () => {
      const orderData = { orderId: 1, faultDesc: '已修复' }
      const result = await updateOrder(orderData)
      expect(result.code).toBe(200)
    })
  })

  describe('delOrder', () => {
    it('应删除单个工单', async () => {
      const result = await delOrder(1)
      expect(result.code).toBe(200)
    })

    it('应批量删除工单', async () => {
      const result = await delOrder([1, 2, 3])
      expect(result.code).toBe(200)
    })

    it('应传入正确URL', async () => {
      const { default: request } = await import('@/utils/request')
      await delOrder(5)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/order/5',
          method: 'delete',
        })
      )
    })
  })

  describe('batchAssignOrder', () => {
    it('应批量分配工单', async () => {
      const result = await batchAssignOrder({
        orderIds: [1, 2, 3],
        assignTo: 'lisi',
      })
      expect(result.code).toBe(200)
    })

    it('应传入分配参数', async () => {
      const data = { orderIds: [1, 2], assignTo: 'wangwu' }
      const { default: request } = await import('@/utils/request')
      await batchAssignOrder(data)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/order/batchAssign',
          method: 'put',
          data,
        })
      )
    })
  })

  describe('exportOrder', () => {
    it('应导出Excel Blob', async () => {
      const result = await exportOrder({ orderStatus: '0' })
      expect(result).toBeInstanceOf(Blob)
    })

    it('应设置responseType为blob', async () => {
      const { default: request } = await import('@/utils/request')
      await exportOrder({})
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/order/export',
          method: 'get',
          responseType: 'blob',
        })
      )
    })
  })

  describe('getWorkOrderStats', () => {
    it('应返回统计看板数据', async () => {
      const result = await getWorkOrderStats()
      expect(result.code).toBe(200)
      expect(result.data.totalCount).toBe(100)
      expect(result.data.faultTopDevices).toHaveLength(1)
    })

    it('应包含所有统计字段', async () => {
      const result = await getWorkOrderStats()
      const stats = result.data
      expect(stats).toHaveProperty('totalCount')
      expect(stats).toHaveProperty('pendingCount')
      expect(stats).toHaveProperty('inProgressCount')
      expect(stats).toHaveProperty('completedCount')
      expect(stats).toHaveProperty('faultTopDevices')
    })
  })
})