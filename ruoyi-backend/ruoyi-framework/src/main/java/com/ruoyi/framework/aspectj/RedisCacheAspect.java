package com.ruoyi.framework.aspectj;

import com.ruoyi.common.annotation.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 业务缓存切面
 * <p>
 * 拦截 {@link RedisCache} 注解的方法，实现自动缓存查询结果或清除缓存。
 * CACHE 模式：方法返回后将返回值写入 Redis
 * EVICT  模式：方法执行后删除指定 Key 的缓存（含模糊匹配清理）
 */
@Aspect
@Component
public class RedisCacheAspect {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheAspect.class);

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Autowired
    private com.ruoyi.common.core.redis.RedisCache redisCache;

    @Around("@annotation(cacheAnnotation)")
    public Object around(ProceedingJoinPoint point, RedisCache cacheAnnotation) throws Throwable {
        String key = buildCacheKey(cacheAnnotation, point);

        if (cacheAnnotation.action() == RedisCache.Action.EVICT) {
            // ========== 清除缓存 ==========
            Object result = point.proceed();
            // 删除精确 Key
            redisCache.deleteObject(key);
            // 删除该前缀下的所有缓存（模糊匹配）
            String pattern = key.substring(0, key.lastIndexOf(":") + 1) + "*";
            Collection<String> keys = redisCache.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisCache.deleteObject(keys);
            }
            log.debug("RedisCache EVICT key={}, pattern deleted count={}", key, keys != null ? keys.size() : 0);
            return result;
        }

        // ========== 查询缓存 ==========
        Object cachedValue = redisCache.getCacheObject(key);
        if (cachedValue != null) {
            log.debug("RedisCache HIT key={}", key);
            return cachedValue;
        }

        // 执行原方法
        Object result = point.proceed();
        if (result != null) {
            redisCache.setCacheObject(key, result, cacheAnnotation.expire(), TimeUnit.SECONDS);
            log.debug("RedisCache SET key={}, expire={}s", key, cacheAnnotation.expire());
        }
        return result;
    }

    /**
     * 解析 SpEL 表达式，构建完整 Redis Key
     */
    private String buildCacheKey(RedisCache annotation, ProceedingJoinPoint point) {
        String key = annotation.key();
        String suffix = annotation.keySuffix();
        if (StringUtils.isNotEmpty(suffix)) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            // 获取方法参数名（通过 LocalVariableTable 字节码调试信息）
            String[] paramNames = discoverer.getParameterNames(method);
            Object[] args = point.getArgs();
            EvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < Math.min(paramNames.length, args.length); i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            // 解析 SpEL 表达式，如 #deviceId、#deviceInfo.deviceId、'dashboard'
            try {
                suffix = parser.parseExpression(suffix).getValue(context, String.class);
            } catch (Exception e) {
                log.warn("SpEL解析失败 keySuffix={}, 使用原始字符串", suffix, e);
            }
            key = key + suffix;
        }
        return key;
    }
}