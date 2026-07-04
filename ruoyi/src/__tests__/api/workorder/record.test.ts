import { describe, it, expect, vi } from 'vitest'
import { listRecord, addRecord, delRecord } from '@/api/workorder/record'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => {
    if (config.method === 'get') {
      return Promise.resolve({
        code: 200,
        rows: [
          {
            recordId: 1,
            orderId: 1,
            repairBy: 'lisi',
            repairSolution: '更换电源模块',
            repairResult: '0',
          },
        ],
        total: 1,
      })
    }
    if (config.method === 'post') {
      return Promise.resolve({ code: 200, msg: '操作成功' })
    }
    if (config.method === 'delete') {
      return Promise.resolve({ code: 200, msg: '删除成功' })
    }
    return Promise.resolve({ code: 200 })
  }),
}))

describe('WorkOrderRecord API', () => {
  describe('listRecord', () => {
    it('应返回维修记录列表', async () => {
      const result = await listRecord({ orderId: 1 })
      expect(result.code).toBe(200)
      expect(result.rows).toHaveLength(1)
      expect(result.rows[0].repairSolution).toBe('更换电源模块')
    })

    it('应按工单ID筛选', async () => {
      const { default: request } = await import('@/utils/request')
      await listRecord({ orderId: 1 })
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/record/list',
          method: 'get',
          params: { orderId: 1 },
        })
      )
    })

    it('应返回空记录列表', async () => {
      // 重新mock空结果
      const request = (await import('@/utils/request')).default
      request.mockResolvedValueOnce({
        code: 200,
        rows: [],
        total: 0,
      })
      const result = await listRecord({ orderId: 999 })
      expect(result.rows).toHaveLength(0)
      expect(result.total).toBe(0)
    })
  })

  describe('addRecord', () => {
    it('应新增维修记录并完成工单', async () => {
      const recordData = {
        orderId: 1,
        repairSolution: '更换零件',
        imageUrls: JSON.stringify(['http://example.com/img.jpg']),
        repairResult: '0',
      }
      const result = await addRecord(recordData)
      expect(result.code).toBe(200)
    })

    it('应传入维修数据到POST请求', async () => {
      const data = { orderId: 1, repairSolution: '修复完成' }
      const { default: request } = await import('@/utils/request')
      await addRecord(data)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/record',
          method: 'post',
          data,
        })
      )
    })
  })

  describe('delRecord', () => {
    it('应删除维修记录', async () => {
      const result = await delRecord(1)
      expect(result.code).toBe(200)
    })

    it('应传入正确记录ID', async () => {
      const { default: request } = await import('@/utils/request')
      await delRecord(5)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/workorder/record/5',
          method: 'delete',
        })
      )
    })
  })
})