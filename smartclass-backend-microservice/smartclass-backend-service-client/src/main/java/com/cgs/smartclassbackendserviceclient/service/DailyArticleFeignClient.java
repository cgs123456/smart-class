package com.cgs.smartclassbackendserviceclient.service;

import com.cgs.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 每日文章服务 Feign 客户端
 *
 * <p>注意：name 必须与 dailyarticle 服务在 Nacos 注册的服务名一致，即 {@code smartclass-backend-dailyarticle}。
 * path 指向 dailyarticle 服务内部接口前缀 {@code /inner}。</p>
 */
@FeignClient(name = "smartclass-backend-dailyarticle", path = "/inner")
public interface DailyArticleFeignClient {

    /**
     * 获取用户文章学习摘要
     *
     * @param userId 用户ID
     * @return 文章学习画像数据
     */
    @GetMapping("/article/summary")
    LearningProfileDTO getArticleLearningSummary(@RequestParam("userId") Long userId);
}
