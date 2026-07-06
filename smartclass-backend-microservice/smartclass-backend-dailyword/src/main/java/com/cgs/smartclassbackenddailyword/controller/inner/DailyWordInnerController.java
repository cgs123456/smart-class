package com.cgs.smartclassbackenddailyword.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cgs.smartclassbackenddailyword.service.UserDailyWordService;
import com.cgs.smartclassbackenddailyword.service.UserWordBookService;
import com.cgs.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.cgs.smartclassbackendmodel.model.entity.UserDailyWord;
import com.cgs.smartclassbackendmodel.model.entity.UserWordBook;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 每日单词服务内部接口
 * 供其他微服务通过 Feign 调用，/inner/** 路径已被网关拦截外部访问
 */
@RestController
@RequestMapping("/inner")
public class DailyWordInnerController {

    @Resource
    private UserDailyWordService userDailyWordService;

    @Resource
    private UserWordBookService userWordBookService;

    /**
     * 获取用户单词学习摘要
     * 统计 UserDailyWord 中各掌握程度的单词数 + UserWordBook 生词本统计
     *
     * @param userId 用户ID
     * @return 单词学习画像数据
     */
    @GetMapping("/word/summary")
    public LearningProfileDTO getWordLearningSummary(@RequestParam("userId") Long userId) {
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
