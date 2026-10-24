package com.smartclass.smartclassbackenddailyword.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartclass.smartclassbackenddailyword.service.UserDailyWordService;
import com.smartclass.smartclassbackenddailyword.service.UserWordBookService;
import com.smartclass.smartclassbackenddailyword.service.WordLearningProfileService;
import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.smartclass.smartclassbackendmodel.model.entity.UserDailyWord;
import com.smartclass.smartclassbackendmodel.model.entity.UserWordBook;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 单词学习画像服务实现
 * 逻辑自 DailyWordInnerController 迁移而来
 */
@Service
@Slf4j
public class WordLearningProfileServiceImpl implements WordLearningProfileService {

    @Resource
    private UserDailyWordService userDailyWordService;

    @Resource
    private UserWordBookService userWordBookService;

    @Override
    public LearningProfileDTO getWordLearningSummary(Long userId) {
        LearningProfileDTO profile = new LearningProfileDTO();

        try {
            // 查询用户所有每日单词学习记录
            List<UserDailyWord> dailyWordList = userDailyWordService.list(
                    new QueryWrapper<UserDailyWord>().eq("userId", userId)
            );
            if (dailyWordList != null) {
                int studied = 0;
                int mastered = 0;
                int familiar = 0;
                int newWord = 0;
                for (UserDailyWord udw : dailyWordList) {
                    // 仅统计已学习的
                    if (udw.getIsStudied() != null && udw.getIsStudied() == 1) {
                        studied++;
                        Integer mastery = udw.getMasteryLevel();
                        if (mastery == null) {
                            continue;
                        }
                        switch (mastery) {
                            case 3:
                                mastered++;
                                break;
                            case 2:
                                familiar++;
                                break;
                            case 1:
                                newWord++;
                                break;
                            default:
                                break;
                        }
                    }
                }
                profile.setWordStudied(studied);
                profile.setWordMastered(mastered);
                profile.setWordFamiliar(familiar);
                profile.setWordNew(newWord);
            }
        } catch (Exception e) {
            // 查询失败保持默认值 0
        }

        try {
            // 查询用户生词本（未删除的）
            List<UserWordBook> wordBookList = userWordBookService.list(
                    new QueryWrapper<UserWordBook>().eq("userId", userId).eq("isDeleted", 0)
            );
            if (wordBookList != null) {
                int total = wordBookList.size();
                int mastered = 0;
                for (UserWordBook uwb : wordBookList) {
                    // learningStatus：0-未学习，1-已学习，2-已掌握
                    if (uwb.getLearningStatus() != null && uwb.getLearningStatus() == 2) {
                        mastered++;
                    }
                }
                profile.setWordBookTotal(total);
                profile.setWordBookMastered(mastered);
            }
        } catch (Exception e) {
            // 查询失败保持默认值 0
        }

        return profile;
    }
}
