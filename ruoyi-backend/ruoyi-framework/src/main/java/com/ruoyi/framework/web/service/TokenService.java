package com.ruoyi.framework.web.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.UserAgentUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;

/**
 * token验证处理
 * 
 * @author ruoyi
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认30分钟）
    @Value("${token.expireTime}")
    private int expireTime;

    // refreshToken 有效期（天）
    @Value("${token.refreshExpireTime:7}")
    private int refreshExpireTime;

    // 双 Token 开关
    @Value("${token.dualTokenEnabled:true}")
    private boolean dualTokenEnabled;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    /**
     * 获取用户身份信息
     * 
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                LoginUser user = redisCache.getCacheObject(userKey);
                return user;
            }
            catch (Exception e)
            {
                log.debug("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    /**
     * 创建令牌
     * 
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        return createToken(claims);
    }

    /**
     * 创建双 Token
     * <p>
     * 返回 accessToken（JWT，短时效）+ refreshToken（UUID，长时效）
     * </p>
     *
     * @param loginUser 用户信息
     * @return token -> accessToken, refreshToken -> refreshToken
     */
    public Map<String, String> createDualToken(LoginUser loginUser)
    {
        // 1. 创建 accessToken（原有 createToken 逻辑不变）
        String accessToken = createToken(loginUser);

        if (!dualTokenEnabled)
        {
            Map<String, String> tokens = new HashMap<>();
            tokens.put(Constants.TOKEN, Constants.TOKEN_PREFIX + accessToken);
            return tokens;
        }

        // 2. 创建 refreshToken
        String refreshToken = IdUtils.fastUUID();
        loginUser.setRefreshToken(refreshToken);
        String refreshKey = getRefreshTokenKey(refreshToken);
        redisCache.setCacheObject(refreshKey, loginUser.getToken(),
                refreshExpireTime, TimeUnit.DAYS);

        // 3. 返回双 Token
        Map<String, String> tokens = new HashMap<>();
        tokens.put(Constants.TOKEN, Constants.TOKEN_PREFIX + accessToken);
        tokens.put("refreshToken", Constants.TOKEN_PREFIX + refreshToken);
        return tokens;
    }

    /**
     * 刷新 accessToken
     *
     * @param refreshTokenStr 请求头中携带的 refreshToken（含 Bearer 前缀）
     * @return 新的 accessToken（含 Bearer 前缀）
     */
    public String refreshAccessToken(String refreshTokenStr)
    {
        // 1. 校验 refreshToken
        String tokenValue = refreshTokenStr.replace(Constants.TOKEN_PREFIX, "");
        String refreshKey = getRefreshTokenKey(tokenValue);
        String accessTokenUuid = redisCache.getCacheObject(refreshKey);
        if (StringUtils.isEmpty(accessTokenUuid))
        {
            throw new ServiceException("refreshToken 已过期，请重新登录");
        }

        // 2. 获取用户信息
        String userKey = getTokenKey(accessTokenUuid);
        LoginUser loginUser = redisCache.getCacheObject(userKey);
        if (loginUser == null)
        {
            throw new ServiceException("用户会话已过期，请重新登录");
        }

        // 3. 生成新 accessToken
        String newAccessToken = createToken(loginUser);

        // 4. 删除旧 accessToken 的 Redis 缓存（使用旧 uuid）
        redisCache.deleteObject(userKey);

        // 5. 更新 refreshToken 指向新的 accessToken Uuid
        redisCache.setCacheObject(refreshKey, loginUser.getToken(),
                refreshExpireTime, TimeUnit.DAYS);

        return Constants.TOKEN_PREFIX + newAccessToken;
    }

    /**
     * 登出处理：加入黑名单 + 删除 Redis 缓存
     */
    public void logout(HttpServletRequest request)
    {
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                // 解析 JWT 获取过期时间
                Claims claims = parseToken(token);
                long expiration = claims.getExpiration().getTime();
                long now = System.currentTimeMillis();
                if (expiration > now)
                {
                    // 将 token 加入黑名单，有效期至 JWT 原本过期时间
                    String blacklistKey = CacheConstants.TOKEN_BLACKLIST_KEY + token;
                    redisCache.setCacheObject(blacklistKey, "1",
                            Long.valueOf((expiration - now) / 1000).intValue(), TimeUnit.SECONDS);
                }
                // 删除 Redis 中的 LoginUser
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                redisCache.deleteObject(getTokenKey(uuid));
            }
            catch (Exception e)
            {
                log.debug("登出解析 token 异常", e);
            }
        }
    }

    /**
     * 检查 token 是否在黑名单中
     */
    public boolean isBlacklisted(String token)
    {
        return redisCache.hasKey(CacheConstants.TOKEN_BLACKLIST_KEY + token);
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     * 
     * @param loginUser 登录信息
     * @return 令牌
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     * 
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 设置用户代理信息
     * 
     * @param loginUser 登录信息
     */
    public void setUserAgent(LoginUser loginUser)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getBrowser(userAgent));
        loginUser.setOs(UserAgentUtils.getOperatingSystem(userAgent));
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims)
    {
        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    public String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    private String getRefreshTokenKey(String refreshToken)
    {
        return CacheConstants.REFRESH_TOKEN_KEY + refreshToken;
    }

    /**
     * 角色权限变更后，刷新所有持有该角色的在线用户权限
     *
     * @param roleId            变更的角色ID
     * @param permissionService 权限服务
     */
    public void refreshPermissionByRoleId(Long roleId, SysPermissionService permissionService)
    {
        // 扫描所有在线 token
        String pattern = CacheConstants.LOGIN_TOKEN_KEY + "*";
        Collection<String> keys = redisCache.keys(pattern);
        if (keys == null || keys.isEmpty())
        {
            return;
        }
        for (String key : keys)
        {
            LoginUser loginUser = redisCache.getCacheObject(key);
            if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().isAdmin())
            {
                // 管理员拥有所有权限，跳过
                continue;
            }
            // 判断该用户是否拥有此角色
            boolean hasRole = loginUser.getUser().getRoles() != null
                    && loginUser.getUser().getRoles().stream().anyMatch(r -> roleId.equals(r.getRoleId()));
            if (!hasRole)
            {
                continue;
            }
            // 刷新权限缓存
            loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
            refreshToken(loginUser);
            log.info("角色[{}]权限变更，已刷新在线用户[{}]的权限缓存", roleId, loginUser.getUsername());
        }
    }
}