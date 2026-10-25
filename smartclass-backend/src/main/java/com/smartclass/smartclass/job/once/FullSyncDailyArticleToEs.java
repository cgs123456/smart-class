package com.smartclass.smartclass.job.once;

import cn.hutool.core.collection.CollUtil;
import com.smartclass.smartclass.esdao.DailyArticleEsDao;
import com.smartclass.smartclass.model.dto.dailyarticle.DailyArticleEsDTO;
import com.smartclass.smartclass.model.entity.DailyArticle;
import com.smartclass.smartclass.service.DailyArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全量同步每日美文到 ES
 */
@Component
@ConditionalOnProperty(name = "rag.full-sync-on-startup", havingValue = "true")
@Slf4j
// 全量重建为运维动作：默认不在启动时执行，需显式传 --rag.full-sync-on-startup=true。
// 增量同步走业务写入路径，不受此开关影响。
public class FullSyncDailyArticleToEs implements CommandLineRunner {

    @Resource
    private DailyArticleService dailyArticleService;

    @Autowired(required = false)
    private DailyArticleEsDao dailyArticleEsDao;

    @Override
    public void run(String... args) {
        if (dailyArticleEsDao == null) {
            log.info("Elasticsearch不可用，跳过全量同步每日美文");
            return;
        }
        List<DailyArticle> dailyArticleList = dailyArticleService.list();
        if (CollUtil.isEmpty(dailyArticleList)) {
            return;
        }
        List<DailyArticleEsDTO> dailyArticleEsDTOList = dailyArticleList.stream()
                .map(DailyArticleEsDTO::objToDto)
                .collect(Collectors.toList());
        final int pageSize = 500;
        int total = dailyArticleEsDTOList.size();
        log.info("FullSyncDailyArticleToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            dailyArticleEsDao.saveAll(dailyArticleEsDTOList.subList(i, end));
        }
        log.info("FullSyncDailyArticleToEs end, total {}", total);
    }
} 