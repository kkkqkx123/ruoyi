package com.ruoyi.framework.aspectj;

import java.lang.reflect.Field;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.Desensitize;
import com.ruoyi.common.annotation.DesensitizeField;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.DesensitizedType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 数据脱敏切面
 * <p>
 * 拦截标注 {@link Desensitize} 的方法，对返回的 {@link AjaxResult} 中 data 字段
 * 按注解指定的字段路径和脱敏类型进行脱敏处理。
 * </p>
 *
 * @author ruoyi
 */
@Aspect
@Component
public class DesensitizeAspect
{
    private static final Logger log = LoggerFactory.getLogger(DesensitizeAspect.class);

    @Around("@annotation(desensitize)")
    public Object around(ProceedingJoinPoint joinPoint, Desensitize desensitize) throws Throwable
    {
        Object result = joinPoint.proceed();

        // 仅对 AjaxResult 类型的成功响应进行脱敏
        if (result instanceof AjaxResult ajax)
        {
            Object code = ajax.get(AjaxResult.CODE_TAG);
            if (code == null || !"200".equals(code.toString()))
            {
                return result;
            }
            Object data = ajax.get(AjaxResult.DATA_TAG);
            if (data == null)
            {
                return result;
            }

            // 管理员豁免
            try
            {
                if (SecurityUtils.isAdmin())
                {
                    return result;
                }
            }
            catch (Exception e)
            {
                // 未登录等场景不豁免
            }

            try
            {
                Object sensitiveData = doDesensitize(data, desensitize.fields());
                ajax.put(AjaxResult.DATA_TAG, sensitiveData);
            }
            catch (Exception e)
            {
                log.error("数据脱敏异常", e);
            }
        }

        return result;
    }

    /**
     * 执行脱敏处理
     *
     * @param data   原始数据对象
     * @param fields 脱敏字段数组
     * @return 脱敏后的数据对象
     */
    @SuppressWarnings("unchecked")
    private Object doDesensitize(Object data, DesensitizeField[] fields)
    {
        if (data == null)
        {
            return null;
        }

        // 处理 List 类型
        if (data instanceof List<?> list)
        {
            for (Object item : list)
            {
                if (item != null)
                {
                    desensitizeObject(item, fields);
                }
            }
            return data;
        }

        // 处理普通对象
        desensitizeObject(data, fields);
        return data;
    }

    /**
     * 对单个对象按指定字段列表进行脱敏
     */
    private void desensitizeObject(Object obj, DesensitizeField[] fields)
    {
        for (DesensitizeField field : fields)
        {
            String fieldPath = field.name();
            DesensitizedType type = field.type();

            // 支持嵌套路径，如 "user.phone"
            int dotIndex = fieldPath.indexOf('.');
            if (dotIndex > 0)
            {
                String targetField = fieldPath.substring(0, dotIndex);
                String remainPath = fieldPath.substring(dotIndex + 1);
                Object nestedObj = getFieldValue(obj, targetField);
                if (nestedObj != null)
                {
                    // 递归脱敏嵌套对象
                    if (nestedObj instanceof List<?> nestedList)
                    {
                        for (Object item : nestedList)
                        {
                            if (item != null)
                            {
                                processFieldRecursively(item, remainPath, type);
                            }
                        }
                    }
                    else
                    {
                        processFieldRecursively(nestedObj, remainPath, type);
                    }
                }
                continue;
            }

            // 直接字段脱敏
            setDesensitizedValue(obj, fieldPath, type);
        }
    }

    /**
     * 递归处理嵌套字段路径
     */
    private void processFieldRecursively(Object obj, String fieldPath, DesensitizedType type)
    {
        int dotIndex = fieldPath.indexOf('.');
        if (dotIndex > 0)
        {
            String currentField = fieldPath.substring(0, dotIndex);
            String remainPath = fieldPath.substring(dotIndex + 1);
            Object nestedObj = getFieldValue(obj, currentField);
            if (nestedObj != null)
            {
                processFieldRecursively(nestedObj, remainPath, type);
            }
        }
        else
        {
            setDesensitizedValue(obj, fieldPath, type);
        }
    }

    /**
     * 获取对象的字段值（支持 public getter 和直接字段访问）
     */
    private Object getFieldValue(Object obj, String fieldName)
    {
        try
        {
            // 优先尝试 getter 方法
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try
            {
                var method = obj.getClass().getMethod(getterName);
                return method.invoke(obj);
            }
            catch (NoSuchMethodException e)
            {
                // 回退到直接字段访问
                Field field = getField(obj.getClass(), fieldName);
                if (field != null)
                {
                    field.setAccessible(true);
                    return field.get(obj);
                }
            }
        }
        catch (Exception e)
        {
            log.debug("获取字段值失败: {}#{}", obj.getClass().getSimpleName(), fieldName, e);
        }
        return null;
    }

    /**
     * 获取字段（包含父类字段）
     */
    private Field getField(Class<?> clazz, String fieldName)
    {
        Class<?> current = clazz;
        while (current != null && current != Object.class)
        {
            try
            {
                return current.getDeclaredField(fieldName);
            }
            catch (NoSuchFieldException e)
            {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 对字段进行脱敏并写回
     */
    private void setDesensitizedValue(Object obj, String fieldName, DesensitizedType type)
    {
        try
        {
            Object rawValue = getFieldValue(obj, fieldName);
            if (rawValue == null || !(rawValue instanceof String str))
            {
                return;
            }

            String desensitized = type.desensitizer().apply(str);
            if (desensitized == null || desensitized.equals(str))
            {
                return;
            }

            // 优先尝试 setter 方法
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try
            {
                var method = obj.getClass().getMethod(setterName, String.class);
                method.invoke(obj, desensitized);
            }
            catch (NoSuchMethodException e)
            {
                // 回退到直接字段访问
                Field field = getField(obj.getClass(), fieldName);
                if (field != null)
                {
                    field.setAccessible(true);
                    field.set(obj, desensitized);
                }
            }
        }
        catch (Exception e)
        {
            log.debug("设置脱敏值失败: {}#{}", obj.getClass().getSimpleName(), fieldName, e);
        }
    }
}