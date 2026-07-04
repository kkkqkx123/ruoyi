package com.ruoyi.common.utils.file;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.MinIoConfig;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

/**
 * MinIO 对象存储工具类
 * 
 * @author ruoyi
 */
@Component
public class MinIoUtils
{
    private static final Logger log = LoggerFactory.getLogger(MinIoUtils.class);

    @Autowired
    private MinIoConfig minIoConfig;

    private MinioClient minioClient;

    @PostConstruct
    public void init()
    {
        if (StringUtils.isEmpty(minIoConfig.getEndpoint()))
        {
            log.warn("MinIO endpoint 未配置，MinIO 存储功能不可用");
            return;
        }
        minioClient = MinioClient.builder()
                .endpoint(minIoConfig.getEndpoint())
                .credentials(minIoConfig.getAccessKey(), minIoConfig.getSecretKey())
                .build();
        log.info("MinIO 客户端初始化完成，endpoint: {}", minIoConfig.getEndpoint());
    }

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 文件可访问 URL
     * @throws Exception 上传异常
     */
    public String upload(MultipartFile file) throws Exception
    {
        // 生成文件名：日期目录/UUID.扩展名
        String extension = FileUploadUtils.getExtension(file);
        String fileName = DateUtils.datePath() + "/" + IdUtils.fastSimpleUUID() + "." + extension;

        // 检查 bucket 是否存在，不存在则创建
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minIoConfig.getBucketName()).build());
        if (!found)
        {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(minIoConfig.getBucketName()).build());
            log.info("MinIO 存储桶 [{}] 已创建", minIoConfig.getBucketName());
        }

        // 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minIoConfig.getBucketName())
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                        .build());

        // 返回可访问 URL
        String url = minIoConfig.getPublicDomain() + "/" + minIoConfig.getBucketName() + "/" + fileName;
        log.info("MinIO 文件上传成功: {}", url);
        return url;
    }

    /**
     * 删除文件
     *
     * @param fileName 文件路径（含目录）
     * @throws Exception 删除异常
     */
    public void delete(String fileName) throws Exception
    {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(minIoConfig.getBucketName())
                        .object(fileName)
                        .build());
        log.info("MinIO 文件删除成功: {}", fileName);
    }

    /**
     * 判断 MinIO 是否可用
     */
    public boolean isAvailable()
    {
        return minioClient != null && StringUtils.isNotEmpty(minIoConfig.getEndpoint());
    }
}