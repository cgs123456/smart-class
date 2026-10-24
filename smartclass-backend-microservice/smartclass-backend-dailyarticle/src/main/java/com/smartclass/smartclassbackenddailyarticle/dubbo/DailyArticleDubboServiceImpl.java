package com.smartclass.smartclassbackenddailyarticle.dubbo;

import com.smartclass.smartclassbackenddailyarticle.service.ArticleLearningProfileService;
import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.smartclass.smartclassbackendserviceclient.dubbo.DailyArticleDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 每日文章 Dubbo 服务实现
 *
 * 替代原 DailyArticleInnerController (Feign 内部接口)
 * 协议: Triple (HTTP/2 + Protobuf)
 * 注册中心: Nacos
 */
@DubboService(
    version = "1.0.0",
    protocol = "triple",
    interfaceClass = DailyArticleDubboService.class
)
public class DailyArticleDubboServiceImpl implements DailyArticleDubboService {

    @Resource
    private ArticleLearningProfileService articleLearningProfileService;

    @Override
    public LearningProfileDTO getArticleLearningSummary(Long userId) {
        return articleLearningProfileService.getArticleLearningSummary(userId);
    }
}
