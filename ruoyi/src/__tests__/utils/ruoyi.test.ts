import { describe, it, expect } from 'vitest'
import {
  parseTime,
  addDateRange,
  selectDictLabel,
  selectDictLabels,
  blobValidate,
} from '@/utils/ruoyi'

describe('工具函数', () => {
  describe('parseTime', () => {
    it('应格式化 Date 对象', () => {
      const date = new Date(2026, 6, 3, 14, 30, 0) // 2026-07-03 14:30:00
      const result = parseTime(date, '{y}-{m}-{d} {h}:{i}:{s}')
      expect(result).toBe('2026-07-03 14:30:00')
    })

    it('应格式化时间戳', () => {
      const timestamp = new Date(2026, 0, 1).getTime() // 2026-01-01
      const result = parseTime(timestamp, '{y}-{m}-{d}')
      expect(result).toBe('2026-01-01')
    })

    it('应格式化日期字符串', () => {
      const result = parseTime('2026-07-03 14:30:00', '{y}-{m}-{d}')
      expect(result).toBe('2026-07-03')
    })

    it('应处理10位时间戳', () => {
      const timestamp = Math.floor(new Date(2026, 0, 1).getTime() / 1000)
      const result = parseTime(timestamp, '{y}-{m}-{d}')
      expect(result).toBe('2026-01-01')
    })

    it('应返回星期', () => {
      // 2026-07-03 是星期五
      const date = new Date(2026, 6, 3)
      const result = parseTime(date, '{a}')
      expect(result).toBe('五')
    })

    it('无参数应返回 null', () => {
      expect(parseTime('')).toBeNull()
      expect(parseTime(null as any)).toBeNull()
      expect(parseTime(undefined as any)).toBeNull()
    })
  })

  describe('addDateRange', () => {
    it('应添加时间范围到 params', () => {
      const params = { orderStatus: '0' }
      const dateRange = ['2026-07-01', '2026-07-31']
      const result = addDateRange(params, dateRange)
      expect(result.params.beginTime).toBe('2026-07-01')
      expect(result.params.endTime).toBe('2026-07-31')
      expect(result.orderStatus).toBe('0')
    })

    it('应保留已有 params', () => {
      const params = { params: { custom: 'value' } }
      const result = addDateRange(params, ['2026-07-01', '2026-07-31'])
      expect(result.params.beginTime).toBe('2026-07-01')
      expect(result.params.endTime).toBe('2026-07-31')
      expect(result.params.custom).toBe('value')
    })

    it('应使用自定义属性名', () => {
      const params = {}
      const result = addDateRange(params, ['2026-01-01', '2026-12-31'], 'CreateTime')
      expect(result.params.beginCreateTime).toBe('2026-01-01')
      expect(result.params.endCreateTime).toBe('2026-12-31')
    })

    it('空数组应不添加', () => {
      const params = {}
      const result = addDateRange(params, [])
      expect(result.params.beginTime).toBeUndefined()
      expect(result.params.endTime).toBeUndefined()
    })
  })

  describe('selectDictLabel', () => {
    const dictData = [
      { value: '0', label: '未派单' },
      { value: '1', label: '已派单' },
      { value: '2', label: '维修中' },
    ]

    it('应回显字典标签', () => {
      expect(selectDictLabel(dictData, '0')).toBe('未派单')
      expect(selectDictLabel(dictData, '1')).toBe('已派单')
      expect(selectDictLabel(dictData, '2')).toBe('维修中')
    })

    it('未定义值应返回空字符串', () => {
      expect(selectDictLabel(dictData, undefined)).toBe('')
    })

    it('无效值应返回原值', () => {
      expect(selectDictLabel(dictData, '99')).toBe('99')
    })
  })

  describe('selectDictLabels', () => {
    const dictData = [
      { value: '0', label: '机械故障' },
      { value: '1', label: '电气故障' },
      { value: '2', label: '软件故障' },
    ]

    it('应回显多选字典标签', () => {
      const result = selectDictLabels(dictData, '0,2')
      expect(result).toContain('机械故障')
      expect(result).toContain('软件故障')
    })

    it('应处理数组输入', () => {
      const result = selectDictLabels(dictData, ['0', '1'])
      expect(result).toContain('机械故障')
      expect(result).toContain('电气故障')
    })
  })

  describe('blobValidate', () => {
    it('Blob 类型不是 JSON 应返回 true', () => {
      const blob = new Blob(['test'], { type: 'application/vnd.ms-excel' })
      expect(blobValidate(blob)).toBe(true)
    })

    it('JSON Blob 应返回 false', () => {
      const blob = new Blob(['{}'], { type: 'application/json' })
      expect(blobValidate(blob)).toBe(false)
    })
  })
})