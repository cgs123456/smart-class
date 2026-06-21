package com.cgs.smartclass.job.once;

import cn.hutool.core.collection.CollUtil;
import com.cgs.smartclass.esdao.DailyWordEsDao;
import com.cgs.smartclass.model.dto.dailyword.DailyWordEsDTO;
import com.cgs.smartclass.model.entity.DailyWord;
import com.cgs.smartclass.service.DailyWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全量同步每日单词到 ES
 */
@Component
@Slf4j
public class FullSyncDailyWordToEs implements CommandLineRunner {

    @Resource
    private DailyWordService dailyWordService;

    @Autowired(required = false)
    private DailyWordEsDao dailyWordEsDao;

    @Override
    public void run(String... args) {
        if (dailyWordEsDao == null) {
            log.info("Elasticsearch不可用，跳过全量同步每日单词");
            return;
        }
        List<DailyWord> dailyWordList = dailyWordService.list();
        if (CollUtil.isEmpty(dailyWordList)) {
            return;
        }
        List<DailyWordEsDTO> dailyWordEsDTOList = dailyWordList.stream()
                .map(DailyWordEsDTO::objToDto)
                .collect(Collectors.toList());
        final int pageSize = 500;
        int total = dailyWordEsDTOList.size();
        log.info("FullSyncDailyWordToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            dailyWordEsDao.saveAll(dailyWordEsDTOList.subList(i, end));
        }
        log.info("FullSyncDailyWordToEs end, total {}", total);
    }
} 