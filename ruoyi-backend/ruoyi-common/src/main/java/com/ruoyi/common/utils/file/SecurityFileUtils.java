package com.ruoyi.common.utils.file;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.file.InvalidExtensionException;

/**
 * 文件安全校验工具类
 * <p>
 * 在原生 {@link FileUploadUtils#assertAllowed(MultipartFile, String[])} 基础上，
 * 增加 Magic Number 文件头校验和路径穿越防护。
 * </p>
 * 
 * @author ruoyi
 */
public class SecurityFileUtils
{
    /** 常见文件类型的 Magic Number（文件头标识） */
    private static final Map<String, byte[]> MAGIC_NUMBERS = new HashMap<>();

    static
    {
        MAGIC_NUMBERS.put("jpg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        MAGIC_NUMBERS.put("jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        MAGIC_NUMBERS.put("png", new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });
        MAGIC_NUMBERS.put("gif", new byte[] { 0x47, 0x49, 0x46 });
        MAGIC_NUMBERS.put("bmp", new byte[] { 0x42, 0x4D });
        MAGIC_NUMBERS.put("pdf", new byte[] { 0x25, 0x50, 0x44, 0x46 });
        MAGIC_NUMBERS.put("doc", new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0 });
        MAGIC_NUMBERS.put("docx", new byte[] { 0x50, 0x4B, 0x03, 0x04 });
        MAGIC_NUMBERS.put("xls", new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0 });
        MAGIC_NUMBERS.put("xlsx", new byte[] { 0x50, 0x4B, 0x03, 0x04 });
        MAGIC_NUMBERS.put("zip", new byte[] { 0x50, 0x4B, 0x03, 0x04 });
        MAGIC_NUMBERS.put("rar", new byte[] { 0x52, 0x61, 0x72, 0x21 });
    }

    /**
     * 对上传文件进行安全校验
     * <ul>
     * <li>文件名安全（路径穿越防护）</li>
     * <li>Magic Number 校验（防伪装扩展名的脚本文件）</li>
     * </ul>
     *
     * @param file 上传的文件
     * @throws InvalidExtensionException 校验不通过时抛出
     */
    public static void assertSecure(MultipartFile file) throws InvalidExtensionException
    {
        // 1. 文件名安全（路径穿越防护）
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")
                || filename.contains(";") || filename.contains("\0"))
        {
            throw new InvalidExtensionException(new String[0], "", filename != null ? filename : "");
        }

        // 2. Magic Number 校验（防脚本上传）
        String extension = FilenameUtils.getExtension(filename);
        if (extension == null)
        {
            throw new InvalidExtensionException(new String[0], "", filename);
        }

        byte[] expectedMagic = MAGIC_NUMBERS.get(extension.toLowerCase());
        if (expectedMagic != null)
        {
            byte[] fileHeader = new byte[expectedMagic.length];
            try (var in = file.getInputStream())
            {
                int bytesRead = in.read(fileHeader, 0, expectedMagic.length);
                if (bytesRead < expectedMagic.length || !Arrays.equals(fileHeader, expectedMagic))
                {
                    throw new InvalidExtensionException(new String[0], extension, filename);
                }
            }
            catch (IOException e)
            {
                throw new InvalidExtensionException(new String[0], extension, filename);
            }
        }
    }
}