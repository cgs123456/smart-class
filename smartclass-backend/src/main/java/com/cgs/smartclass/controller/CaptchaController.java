package com.cgs.smartclass.controller;

import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ResultUtils;
import com.wf.captcha.SpecCaptcha;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${captcha.enable:true}")
    private boolean captchaEnable;

    @GetMapping
    public BaseResponse<Map<String, String>> getCaptcha() {
        if (!captchaEnable) {
            Map<String, String> result = new HashMap<>();
            result.put("captchaEnable", "false");
            return ResultUtils.success(result);
        }
        // 生成验证码
        SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
        String captchaCode = specCaptcha.text().toLowerCase();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis，5分钟过期
        String redisKey = "captcha:" + uuid;
        stringRedisTemplate.opsForValue().set(redisKey, captchaCode, 5, TimeUnit.MINUTES);

        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("captchaImage", specCaptcha.toBase64());
        return ResultUtils.success(result);
    }
}
