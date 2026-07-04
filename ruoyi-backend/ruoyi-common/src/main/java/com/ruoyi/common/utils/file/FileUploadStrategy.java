package com.ruoyi.common.utils.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;

/**
 * 文件上传策略分发器
 * <p>
 * 根据配置的存储策略（local / minio），将文件上传到不同存储后端。
 * 支持安全校验（Magic Number + 路径穿越防护）。
 * </p>
 * 
 * @author ruoyi
 */
@Component
public class FileUploadStrategy
{
    private static final Logger log = LoggerFactory.getLogger(FileUploadStrategy.class);

    @Autowired(required = false)
    private MinIoUtils minIoUtils;

    @Value("${ruoyi.file.strategy:local}")
    private String strategy;

    /**
     * 上传文件，按策略分发
     *
     * @param file 上传的文件
     * @return 文件访问路径（本地为相对路径，MinIO 为完整 URL）
     * @throws Exception 上传异常
     */
    public String upload(MultipartFile file) throws Exception
    {
        // 1. 安全校验（增强）
        SecurityFileUtils.assertSecure(file);

        // 2. 按策略分发
        if ("minio".equals(strategy) && minIoUtils != null && minIoUtils.isAvailable())
        {
            log.debug("文件上传策略: MinIO");
            return minIoUtils.upload(file);
        }

        // 默认本地存储
        log.debug("文件上传策略: 本地存储");
        String filePath = RuoYiConfig.getUploadPath();
        return FileUploadUtils.upload(filePath, file);
    }

    /**
     * 获取当前存储策略
     */
    public String getStrategy()
    {
        return strategy;
    }
}