package com.ruoyi.common.enums;

/**
 * 防重复提交限流模式
 *
 * @author ruoyi
 */
public enum LimitMode
{
    /**
     * 标准模式：用户ID + 接口URL + 参数哈希
     */
    DEFAULT,

    /**
     * 业务锁模式：用户ID + 指定参数值（通过 SpEL 指定参数名）
     */
    PARAM
}