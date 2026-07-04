package com.ruoyi.framework.interceptor.impl;

import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.enums.LimitMode;
import com.ruoyi.common.filter.RepeatedlyRequestWrapper;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpHelper;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.sign.Md5Utils;
import com.ruoyi.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 增强版防重复提交拦截器
 * <p>
 * 基于 Redis SETNX 实现，支持 {@link LimitMode#DEFAULT}（用户ID+URL+参数MD5）
 * 和 {@link LimitMode#PARAM}（用户ID+URL+指定参数值）两种模式。
 * </p>
 *
 * @author ruoyi
 */
@Component
public class EnhancedRepeatSubmitInterceptor extends RepeatSubmitInterceptor
{
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation)
    {
        String nowParams = "";
        if (request instanceof RepeatedlyRequestWrapper)
        {
            RepeatedlyRequestWrapper repeatedlyRequest = (RepeatedlyRequestWrapper) request;
            nowParams = HttpHelper.getBodyString(repeatedlyRequest);
        }

        // body参数为空，获取Parameter的数据
        if (StringUtils.isEmpty(nowParams))
        {
            nowParams = JSON.toJSONString(request.getParameterMap());
        }

        String url = request.getRequestURI();
        // 安全获取用户 ID：未认证时回退为 IP 地址
        String userId;
        try
        {
            userId = String.valueOf(SecurityUtils.getUserId());
        }
        catch (Exception e)
        {
            userId = IpUtils.getIpAddr();
        }
        int interval = annotation.interval();
        LimitMode mode = annotation.mode();

        String signKey;
        if (mode == LimitMode.PARAM)
        {
            // PARAM 模式：基于指定参数值构建业务锁
            String lockParam = annotation.lockParam();
            if (StringUtils.isEmpty(lockParam))
            {
                // 未指定参数名，回退为 DEFAULT 模式
                signKey = buildDefaultKey(url, userId, nowParams);
            }
            else
            {
                String paramValue = extractParamValue(nowParams, lockParam);
                if (StringUtils.isEmpty(paramValue))
                {
                    paramValue = nowParams;
                }
                signKey = CacheConstants.REPEAT_SUBMIT_KEY
                        + userId + ":"
                        + url + ":"
                        + mode.name() + ":"
                        + paramValue;
            }
        }
        else
        {
            // DEFAULT 模式：用户ID + URL + 参数MD5
            signKey = buildDefaultKey(url, userId, nowParams);
        }

        // SETNX 检查：key 已存在说明是重复提交
        Boolean isSet = redisTemplate.opsForValue().setIfAbsent(signKey, "1", interval, TimeUnit.MILLISECONDS);
        return Boolean.FALSE.equals(isSet);
    }

    /**
     * 构建 DEFAULT 模式下的缓存 key
     */
    private String buildDefaultKey(String url, String userId, String params)
    {
        String paramMd5 = Md5Utils.hash(params);
        return CacheConstants.REPEAT_SUBMIT_KEY
                + userId + ":"
                + url + ":"
                + paramMd5;
    }

    /**
     * 从 JSON 请求体中提取指定参数的值（支持 "." 分隔的嵌套路径）
     */
    private String extractParamValue(String jsonBody, String paramPath)
    {
        try
        {
            JSONObject json = JSON.parseObject(jsonBody);
            String[] keys = paramPath.split("\\.");
            Object current = json;
            for (String key : keys)
            {
                if (current instanceof JSONObject jo)
                {
                    current = jo.get(key);
                }
                else
                {
                    return null;
                }
            }
            return current != null ? current.toString() : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}