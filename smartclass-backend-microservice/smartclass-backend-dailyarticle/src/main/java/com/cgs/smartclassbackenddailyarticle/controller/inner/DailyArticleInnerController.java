package com.cgs.smartclassbackenddailyarticle.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cgs.smartclassbackenddailyarticle.service.UserArticleRecordService;
import com.cgs.smartclassbackendmodel.model.dto.UserArticleRecord;
import com.cgs.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 每日文章服务内部接口
 * 供其他微服务通过 Feign 调用，/inner/** 路径已被网关拦截外部访问
 */
@RestController
@RequestMapping("/inner")
public class DailyArticleInnerController {

    @Resource
    private UserArticleRecordService userArticleRecordService;

    /**
     * 获取用户文章学习摘要
     * 统计 UserArticleRecord 中各阅读状态的文章数
     * readStatus：0-未读，1-阅读中，2-已读完
     *
     * @param userId 用户ID
     * @return 文章学习画像数据
     */
    @GetMapping("/article/summary")
    public LearningProfileDTO getArticleLearningSummary(@RequestParam("userId") Long userId) {
        LearningProfileDTO profile = new LearningProfileDTO();

        try {
            List<UserArticleRecord> recordList = userArticleRecordService.list(
                    new QueryWrapper<UserArticleRecord>().eq("userId", userId)
            );
            if (recordList != null) {
                int total = recordList.size();
                int completed = 0;
                int inProgress = 0;
                for (UserArticleRecord record : recordList) {
                    Integer status = record.getReadStatus();
                    if (status == null) {
                        continue;
                    }
                    switch (status) {
                        case 2:
                            completed++;
                            break;
                        case 1:
                            inProgress++;
                            break;
                        default:
                            break;
                    }
                }
                profile.setArticleTotal(total);
                profile.setArticleCompleted(completed);
                profile.setArticleInProgress(inProgress);
            }
        } catch (Exception e) {
            // 查询失败保持默认值 0
        }

        return profile;
    }
}
