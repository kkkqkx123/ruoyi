package com.ruoyi.workorder.utils;

import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.utils.file.SecurityFileUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityFileUtils 文件安全校验 单元测试
 * <p>
 * 覆盖范围：Magic Number 校验、路径穿越防护
 *
 * @author ruoyi
 */
@ExtendWith(MockitoExtension.class)
class SecurityFileUtilsTest {

    @TempDir
    Path tempDir;

    // ==================== 路径穿越防护 ====================

    @Nested
    @DisplayName("路径穿越防护")
    class PathTraversalProtection {

        @Test
        @DisplayName("文件名含 ../ - 抛出异常")
        void shouldRejectParentDirectoryTraversal() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "../etc/passwd", "text/plain", "dummy".getBytes());

            // Act & Assert
            assertThrows(InvalidExtensionException.class, () -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("文件名含 ..\\ - 抛出异常")
        void shouldRejectWindowsDirectoryTraversal() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "..\\windows\\system32\\cmd.exe", "application/octet-stream", "dummy".getBytes());

            // Act & Assert
            assertThrows(InvalidExtensionException.class, () -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("文件名含 / 绝对路径 - 抛出异常")
        void shouldRejectAbsolutePath() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "/usr/bin/script.sh", "application/x-sh", "dummy".getBytes());

            // Act & Assert
            assertThrows(InvalidExtensionException.class, () -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("文件名含分号 - 抛出异常")
        void shouldRejectFilenameWithSemicolon() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malicious.jsp;.jpg", "text/plain", "dummy".getBytes());

            // Act & Assert
            assertThrows(InvalidExtensionException.class, () -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("文件名含 null 字符 - 抛出异常")
        void shouldRejectFilenameWithNullChar() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file", "malicious.php\0.jpg", "text/plain", "dummy".getBytes());

            // Act & Assert
            assertThrows(InvalidExtensionException.class, () -> SecurityFileUtils.assertSecure(file));
        }
    }

    // ==================== Magic Number 校验 ====================

    @Nested
    @DisplayName("Magic Number 校验")
    class MagicNumberValidation {

        @Test
        @DisplayName("JPG 文件 - 正确的 Magic Number 校验通过")
        void shouldAcceptValidJpg() {
            // Arrange
            byte[] validJpgHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", validJpgHeader);

            // Act & Assert (不抛异常即通过)
            assertDoesNotThrow(() -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("图片文件后缀但内容为文本 - Magic Number 不匹配抛出异常")
        void shouldRejectTextFileDisguisedAsJpg() {
            // Arrange
            byte[] textContent = "<?php system($_GET['cmd']); ?>".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "shell.jpg", "image/jpeg", textContent);

            // Act & Assert
            assertThrows(InvalidExtensionException.class, () -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("PNG 文件 - 正确的 Magic Number 校验通过")
        void shouldAcceptValidPng() {
            // Arrange
            byte[] validPngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.png", "image/png", validPngHeader);

            // Act & Assert
            assertDoesNotThrow(() -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("PDF 文件 - 正确的 Magic Number 校验通过")
        void shouldAcceptValidPdf() {
            // Arrange
            byte[] validPdfHeader = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", validPdfHeader);

            // Act & Assert
            assertDoesNotThrow(() -> SecurityFileUtils.assertSecure(file));
        }

        @Test
        @DisplayName("未知扩展名 - 无 Magic Number 映射时跳过校验")
        void shouldSkipMagicCheckForUnknownExtension() {
            // Arrange
            byte[] arbitraryContent = "arbitrary content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "readme.txt", "text/plain", arbitraryContent);

            // Act & Assert
            assertDoesNotThrow(() -> SecurityFileUtils.assertSecure(file));
        }
    }
}