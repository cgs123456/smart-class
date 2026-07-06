package com.cgs.smartclassbackendserviceclient.service;

import com.cgs.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 每日单词服务 Feign 客户端
 *
 * <p>注意：name 必须与 dailyword 服务在 Nacos 注册的服务名一致，即 {@code smartclass-backend-dailyword}。
 * path 指向 dailyword 服务内部接口前缀 {@code /inner}。</p>
 */
@FeignClient(name = "smartclass-backend-dailyword", path = "/inner")
public interface DailyWordFeignClient {

    /**
     * 获取用户单词学习摘要
     *
     * @param userId 用户ID
     * @return 单词学习画像数据
     */
    @GetMapping("/word/summary")
    LearningProfileDTO getWordLearningSummary(@RequestParam("userId") Long userId);
}
