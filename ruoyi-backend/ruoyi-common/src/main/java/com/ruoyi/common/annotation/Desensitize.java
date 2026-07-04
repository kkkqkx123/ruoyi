package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级数据脱敏注解
 * <p>
 * 通过 AOP 环绕通知，在接口返回数据后对指定字段进行脱敏处理。
 * 与 {@link com.ruoyi.common.annotation.Sensitive} 字段级注解互补使用。
 * </p>
 *
 * @author ruoyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Desensitize
{
    /**
     * 脱敏字段映射列表
     */
    DesensitizeField[] fields();
}