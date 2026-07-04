import { describe, it, expect, vi } from 'vitest'
import {
  listDevice,
  getDevice,
  addDevice,
  updateDevice,
  delDevice,
} from '@/api/device/info'

vi.mock('@/utils/request', () => ({
  default: vi.fn((config) => {
    if (config.method === 'get' && config.url?.includes('/list')) {
      return Promise.resolve({
        code: 200,
        rows: [
          {
            deviceId: 1,
            deviceCode: 'DEV-001',
            deviceName: '测试设备',
            status: '0',
            location: 'A栋3楼',
          },
        ],
        total: 1,
      })
    }
    if (config.method === 'get' && config.url?.match(/\/device\/info\/\d+/)) {
      return Promise.resolve({
        code: 200,
        data: { deviceId: 1, deviceCode: 'DEV-001', deviceName: '测试设备' },
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

describe('DeviceInfo API', () => {
  describe('listDevice', () => {
    it('应返回设备列表', async () => {
      const result = await listDevice({ status: '0' })
      expect(result.code).toBe(200)
      expect(result.rows).toHaveLength(1)
      expect(result.rows[0].deviceName).toBe('测试设备')
    })

    it('应传入查询参数', async () => {
      const query = { deviceName: '测试', status: '0' }
      const { default: request } = await import('@/utils/request')
      await listDevice(query)
      expect(request).toHaveBeenCalledWith(
        expect.objectContaining({
          url: '/device/info/list',
          method: 'get',
          params: query,
        })
      )
    })
  })

  describe('getDevice', () => {
    it('应返回设备详情', async () => {
      const result = await getDevice(1)
      expect(result.code).toBe(200)
      expect(result.data.deviceCode).toBe('DEV-001')
    })
  })

  describe('addDevice', () => {
    it('应创建设备', async () => {
      const deviceData = {
        deviceCode: 'DEV-002',
        deviceName: '新设备',
        status: '0',
      }
      const result = await addDevice(deviceData)
      expect(result.code).toBe(200)
    })
  })

  describe('updateDevice', () => {
    it('应更新设备', async () => {
      const result = await updateDevice({ deviceId: 1, deviceName: '已更新' })
      expect(result.code).toBe(200)
    })
  })

  describe('delDevice', () => {
    it('应删除单个设备', async () => {
      const result = await delDevice(1)
      expect(result.code).toBe(200)
    })

    it('应批量删除设备', async () => {
      const result = await delDevice([1, 2])
      expect(result.code).toBe(200)
    })
  })
})