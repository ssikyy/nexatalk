package com.ttikss.nexatalk.util;

import com.ttikss.nexatalk.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectUnsupportedImageTypes() {
        FileUtils fileUtils = new FileUtils();
        ReflectionTestUtils.setField(fileUtils, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(fileUtils, "uploadPrefix", "/uploads");

        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "avatar.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes()
        );

        assertThrows(BusinessException.class, () -> fileUtils.uploadFile(svgFile, "avatar", 1024 * 1024));
    }

    @Test
    void shouldUploadAndDeleteAllowedImage() throws Exception {
        FileUtils fileUtils = new FileUtils();
        ReflectionTestUtils.setField(fileUtils, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(fileUtils, "uploadPrefix", "/uploads");

        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        String url = fileUtils.uploadFile(pngFile, "avatar", 1024 * 1024);
        Path storedFile = tempDir.resolve("avatar").resolve(Path.of(url).getFileName().toString());

        assertTrue(Files.exists(storedFile));
        assertDoesNotThrow(() -> fileUtils.deleteFile(url));
        assertTrue(Files.notExists(storedFile));
    }
}
