package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务缓存注解
 * <p>
 * 用于 Service 方法，自动缓存返回值或清除缓存。
 * 参照 {@link RateLimiter} 注解 + AOP 的设计模式。
 *
 * <pre>
 * // 查询方法：自动缓存返回值，TTL=1800s
 * &#64;RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceId", expire = 1800)
 * public DeviceInfo selectDeviceInfoById(Long deviceId) { ... }
 *
 * // 修改方法：执行后清除缓存
 * &#64;RedisCache(key = CacheConstants.DEVICE_INFO_KEY, keySuffix = "#deviceId", action = RedisCache.Action.EVICT)
 * public int updateDeviceInfo(DeviceInfo deviceInfo) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisCache {

    /**
     * 缓存 Key 前缀
     */
    String key() default "";

    /**
     * 缓存 Key 后缀（支持 SpEL：取方法参数）
     * <p>示例：#deviceId、#deviceInfo.deviceId、'dashboard'</p>
     */
    String keySuffix() default "";

    /**
     * 过期时间（秒），默认 10 分钟
     */
    int expire() default 600;

    /**
     * 操作类型：CACHE=缓存查询 EVICT=清除缓存
     */
    Action action() default Action.CACHE;

    enum Action {
        /** 缓存模式：方法执行后，将返回值写入 Redis */
        CACHE,
        /** 清除模式：方法执行后，删除指定 Key 的缓存 */
        EVICT
    }
}