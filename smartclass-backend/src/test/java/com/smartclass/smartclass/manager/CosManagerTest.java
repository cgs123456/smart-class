package com.smartclass.smartclass.manager;

import com.smartclass.smartclass.config.CosClientConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Cos 操作测试（依赖真实对象存储：未配置凭证时跳过，避免误报失败）
 */
@SpringBootTest
class CosManagerTest {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Test
    void putObject() throws Exception {
        boolean configured = cosClientConfig.getAccessKey() != null && !cosClientConfig.getAccessKey().isBlank()
                && cosClientConfig.getSecretKey() != null && !cosClientConfig.getSecretKey().isBlank()
                && cosClientConfig.getBucket() != null && !cosClientConfig.getBucket().isBlank();
        Assumptions.assumeTrue(configured, "COS 凭证未配置，跳过上传集成测试");
        Path tmp = Files.createTempFile("cos-put-object", ".json");
        try {
            Files.write(tmp, "{\"hello\":\"world\"}".getBytes(StandardCharsets.UTF_8));
            cosManager.putObject("test", tmp.toString());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
