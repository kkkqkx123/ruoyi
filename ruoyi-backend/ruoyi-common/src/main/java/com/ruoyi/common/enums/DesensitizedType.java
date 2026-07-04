package com.ruoyi.common.enums;

import java.util.function.Function;
import com.ruoyi.common.utils.DesensitizedUtil;

/**
 * 脱敏类型
 *
 * @author ruoyi
 */
public enum DesensitizedType
{
    /**
     * 姓名，第2位星号替换
     */
    USERNAME(s -> s.replaceAll("(\\S)\\S(\\S*)", "$1*$2")),

    /**
     * 密码，全部字符都用*代替
     */
    PASSWORD(DesensitizedUtil::password),

    /**
     * 身份证，中间10位星号替换
     */
    ID_CARD(s -> s.replaceAll("(\\d{4})\\d{10}(\\d{3}[Xx]|\\d{4})", "$1** **** ****$2")),

    /**
     * 手机号，中间4位星号替换
     */
    PHONE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")),

    /**
     * 电子邮箱，仅显示第一个字母和@后面的地址显示，其他星号替换
     */
    EMAIL(s -> s.replaceAll("(^.)[^@]*(@.*$)", "$1****$2")),

    /**
     * 银行卡号，保留最后4位，其他星号替换
     */
    BANK_CARD(s -> s.replaceAll("\\d{15}(\\d{3})", "**** **** **** **** $1")),

    /**
     * 车牌号码，包含普通车辆、新能源车辆
     */
    CAR_LICENSE(DesensitizedUtil::carLicense),

    /**
     * 设备涉密编号，保留前2位+后2位，中间星号替换
     */
    DEVICE_CODE(s -> {
        if (s == null || s.length() < 5) return s;
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2);
    }),

    /**
     * 地址，保留省份（前2～3位），其余星号替换
     */
    ADDRESS(s -> {
        if (s == null) return null;
        int provinceEnd = Math.min(s.length(), 3);
        return s.substring(0, provinceEnd) + "****";
    });

    private final Function<String, String> desensitizer;

    DesensitizedType(Function<String, String> desensitizer)
    {
        this.desensitizer = desensitizer;
    }

    public Function<String, String> desensitizer()
    {
        return desensitizer;
    }
}
