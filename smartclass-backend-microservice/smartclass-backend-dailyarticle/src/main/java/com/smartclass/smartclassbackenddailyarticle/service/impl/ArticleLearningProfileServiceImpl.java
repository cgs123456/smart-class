package com.smartclass.smartclassbackenddailyarticle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartclass.smartclassbackenddailyarticle.service.ArticleLearningProfileService;
import com.smartclass.smartclassbackenddailyarticle.service.UserArticleRecordService;
import com.smartclass.smartclassbackendmodel.model.dto.UserArticleRecord;
import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章学习画像服务实现
 * 逻辑自 DailyArticleInnerController 迁移而来
 */
@Service
@Slf4j
public class ArticleLearningProfileServiceImpl implements ArticleLearningProfileService {

    @Resource
    private UserArticleRecordService userArticleRecordService;

    @Override
    public LearningProfileDTO getArticleLearningSummary(Long userId) {
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
