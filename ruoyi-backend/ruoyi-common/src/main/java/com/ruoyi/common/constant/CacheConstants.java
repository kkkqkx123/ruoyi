package com.ruoyi.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /** ===== 业务缓存（优化新增） ===== */

    /** 设备信息缓存 */
    public static final String DEVICE_INFO_KEY = "device_info:";

    /** 工单统计看板缓存 */
    public static final String WORKORDER_STATS_KEY = "workorder_stats:";

    /** 工单分类缓存 */
    public static final String WORKORDER_CATEGORY_KEY = "workorder_category:";

    /** ===== 双 Token 相关缓存 ===== */

    /** refreshToken 缓存 */
    public static final String REFRESH_TOKEN_KEY = "refresh_tokens:";

    /** Token 黑名单缓存 */
    public static final String TOKEN_BLACKLIST_KEY = "token_blacklist:";
}
