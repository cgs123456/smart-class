package com.smartclass.smartclassbackenddailyword.dubbo;

import com.smartclass.smartclassbackenddailyword.service.WordLearningProfileService;
import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.smartclass.smartclassbackendserviceclient.dubbo.DailyWordDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 每日单词 Dubbo 服务实现
 *
 * 替代原 DailyWordInnerController (Feign 内部接口)
 * 协议: Triple (HTTP/2 + Protobuf)
 * 注册中心: Nacos
 */
@DubboService(
    version = "1.0.0",
    protocol = "triple",
    interfaceClass = DailyWordDubboService.class
)
public class DailyWordDubboServiceImpl implements DailyWordDubboService {

    @Resource
    private WordLearningProfileService wordLearningProfileService;

    @Override
    public LearningProfileDTO getWordLearningSummary(Long userId) {
        return wordLearningProfileService.getWordLearningSummary(userId);
    }
}
