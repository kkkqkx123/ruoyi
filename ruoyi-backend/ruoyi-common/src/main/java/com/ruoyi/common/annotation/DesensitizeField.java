package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.ruoyi.common.enums.DesensitizedType;

/**
 * 脱敏字段定义（{@link Desensitize} 的内嵌注解）
 *
 * @author ruoyi
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DesensitizeField
{
    /**
     * 字段名（支持嵌套路径，如 "user.phone"）
     */
    String name();

    /**
     * 脱敏类型
     */
    DesensitizedType type();
}