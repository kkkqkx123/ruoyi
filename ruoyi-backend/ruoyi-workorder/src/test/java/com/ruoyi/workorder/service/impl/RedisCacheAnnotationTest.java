package com.ruoyi.workorder.service.impl;

import com.ruoyi.common.annotation.RedisCache;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.workorder.domain.WorkOrder;
import com.ruoyi.workorder.domain.DeviceInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @RedisCache 注解元数据验证测试
 *
 * 验证缓存注解已正确添加在目标方法上
 * 注意：实际缓存命中/失效需在集成环境中验证（依赖 Redis 服务）
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheAnnotationTest {

    @Test
    @DisplayName("DeviceInfoServiceImpl 查询方法应标注 @RedisCache(CACHE)")
    void testDeviceInfoSelectCacheAnnotation() throws NoSuchMethodException {
        Method method = DeviceInfoServiceImpl.class.getMethod("selectDeviceInfoById", Long.class);
        RedisCache annotation = method.getAnnotation(RedisCache.class);
        assertNotNull(annotation, "selectDeviceInfoById 应标注 @RedisCache");
        assertEquals(CacheConstants.DEVICE_INFO_KEY, annotation.key());
        assertEquals("#deviceId", annotation.keySuffix());
        assertEquals(1800, annotation.expire());
        assertEquals(RedisCache.Action.CACHE, annotation.action());
    }

    @Test
    @DisplayName("DeviceInfoServiceImpl 更新方法应标注 @RedisCache(EVICT)")
    void testDeviceInfoUpdateCacheAnnotation() throws NoSuchMethodException {
        Method method = DeviceInfoServiceImpl.class.getMethod("updateDeviceInfo", DeviceInfo.class);
        RedisCache annotation = method.getAnnotation(RedisCache.class);
        assertNotNull(annotation, "updateDeviceInfo 应标注 @RedisCache");
        assertEquals(CacheConstants.DEVICE_INFO_KEY, annotation.key());
        assertEquals("#deviceInfo.deviceId", annotation.keySuffix());
        assertEquals(RedisCache.Action.EVICT, annotation.action());
    }

    @Test
    @DisplayName("DeviceInfoServiceImpl 删除方法应标注 @RedisCache(EVICT)")
    void testDeviceInfoDeleteCacheAnnotation() throws NoSuchMethodException {
        Method method = DeviceInfoServiceImpl.class.getMethod("deleteDeviceInfoByIds", Long[].class);
        RedisCache annotation = method.getAnnotation(RedisCache.class);
        assertNotNull(annotation, "deleteDeviceInfoByIds 应标注 @RedisCache");
        assertEquals(CacheConstants.DEVICE_INFO_KEY, annotation.key());
        assertEquals("#deviceIds", annotation.keySuffix());
        assertEquals(RedisCache.Action.EVICT, annotation.action());
    }

    @Test
    @DisplayName("WorkOrderServiceImpl 统计看板方法应标注 @RedisCache(CACHE)")
    void testWorkOrderStatsCacheAnnotation() throws NoSuchMethodException {
        Method method = WorkOrderServiceImpl.class.getMethod("selectWorkOrderStats");
        RedisCache annotation = method.getAnnotation(RedisCache.class);
        assertNotNull(annotation, "selectWorkOrderStats 应标注 @RedisCache");
        assertEquals(CacheConstants.WORKORDER_STATS_KEY, annotation.key());
        assertEquals("'dashboard'", annotation.keySuffix());
        assertEquals(600, annotation.expire());
        assertEquals(RedisCache.Action.CACHE, annotation.action());
    }

    @Test
    @DisplayName("WorkOrderServiceImpl 更新工单方法应标注 @RedisCache(EVICT)")
    void testWorkOrderUpdateCacheAnnotation() throws NoSuchMethodException {
        Method method = WorkOrderServiceImpl.class.getMethod("updateWorkOrder", WorkOrder.class);
        RedisCache annotation = method.getAnnotation(RedisCache.class);
        assertNotNull(annotation, "updateWorkOrder 应标注 @RedisCache");
        assertEquals(CacheConstants.WORKORDER_STATS_KEY, annotation.key());
        assertEquals("'dashboard'", annotation.keySuffix());
        assertEquals(RedisCache.Action.EVICT, annotation.action());
    }

    @Test
    @DisplayName("CacheConstants 应包含业务缓存 Key 常量")
    void testCacheConstantsExist() {
        assertNotNull(CacheConstants.DEVICE_INFO_KEY, "DEVICE_INFO_KEY 常量不应为空");
        assertNotNull(CacheConstants.WORKORDER_STATS_KEY, "WORKORDER_STATS_KEY 常量不应为空");
        assertNotNull(CacheConstants.WORKORDER_CATEGORY_KEY, "WORKORDER_CATEGORY_KEY 常量不应为空");
        assertEquals("device_info:", CacheConstants.DEVICE_INFO_KEY);
        assertEquals("workorder_stats:", CacheConstants.WORKORDER_STATS_KEY);
        assertEquals("workorder_category:", CacheConstants.WORKORDER_CATEGORY_KEY);
    }
}