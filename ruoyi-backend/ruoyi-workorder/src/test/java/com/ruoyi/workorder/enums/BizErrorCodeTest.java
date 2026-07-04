package com.ruoyi.workorder.enums;

import com.ruoyi.common.enums.BizErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BizErrorCode 枚举 单元测试
 * <p>
 * 覆盖范围：枚举完整性、错误码唯一性、消息非空
 *
 * @author ruoyi
 */
@DisplayName("BizErrorCode 枚举测试")
class BizErrorCodeTest {

    @Test
    @DisplayName("所有枚举项 - 错误码唯一且消息非空")
    void shouldHaveUniqueCodesAndNonEmptyMessages() {
        // Arrange
        BizErrorCode[] values = BizErrorCode.values();
        Set<Integer> codeSet = new HashSet<>();

        // Act & Assert
        for (BizErrorCode code : values) {
            assertTrue(codeSet.add(code.getCode()),
                    () -> "错误码重复: " + code.getCode() + " (" + code.name() + ")");
            assertNotNull(code.getMessage());
            assertFalse(code.getMessage().isEmpty(),
                    () -> "错误消息不能为空: " + code.name());
        }
    }

    @Test
    @DisplayName("枚举项数量 - 至少包含 10 个业务错误码")
    void shouldContainMinimumErrorCodes() {
        // Assert
        assertTrue(BizErrorCode.values().length >= 10,
                "BizErrorCode 应至少包含 10 个枚举项，当前: " + BizErrorCode.values().length);
    }

    @Test
    @DisplayName("错误码分段 - 1xxx 工单 / 2xxx 设备 / 4xxx 文件 / 5xxx 通用")
    void shouldFollowCodeSegmentRules() {
        // Arrange
        int[] allowedRanges = {1000, 2000, 3000, 4000, 5000};

        // Act & Assert
        for (BizErrorCode code : BizErrorCode.values()) {
            int codePrefix = code.getCode() / 1000 * 1000;
            boolean inRange = false;
            for (int range : allowedRanges) {
                if (codePrefix == range) {
                    inRange = true;
                    break;
                }
            }
            assertTrue(inRange,
                    () -> "错误码 " + code.getCode() + " (" + code.name() + ") 不在允许的分段范围内");
        }
    }

    @Test
    @DisplayName("核心错误码 - 工单不存在/状态无效/归档条件")
    void shouldProvideCoreWorkOrderCodes() {
        // Assert
        assertEquals("工单不存在", BizErrorCode.WORK_ORDER_NOT_FOUND.getMessage());
        assertEquals("工单当前状态不允许此操作", BizErrorCode.WORK_ORDER_STATUS_INVALID.getMessage());
        assertEquals("仅已完成的工单可以归档", BizErrorCode.WORK_ORDER_NOT_COMPLETED.getMessage());
        assertEquals("请填写维修方案", BizErrorCode.WORK_ORDER_REPAIR_SOLUTION_EMPTY.getMessage());
        assertEquals("请上传至少一张维修图片", BizErrorCode.WORK_ORDER_IMAGE_EMPTY.getMessage());
    }

    @Test
    @DisplayName("核心错误码 - 设备/文件/通用")
    void shouldProvideCoreDeviceAndFileCodes() {
        // Assert
        assertEquals("设备不存在", BizErrorCode.DEVICE_NOT_FOUND.getMessage());
        assertEquals("设备编号已存在", BizErrorCode.DEVICE_CODE_DUPLICATE.getMessage());
        assertEquals("文件上传失败", BizErrorCode.FILE_UPLOAD_FAILED.getMessage());
        assertEquals("文件安全校验未通过", BizErrorCode.FILE_SECURITY_CHECK_FAILED.getMessage());
        assertEquals("参数不合法", BizErrorCode.PARAMETER_INVALID.getMessage());
        assertEquals("操作被拒绝", BizErrorCode.OPERATION_DENIED.getMessage());
    }
}