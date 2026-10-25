package com.smartclass.smartclass.job.once;

import com.smartclass.smartclass.esdao.PostEsDao;
import com.smartclass.smartclass.model.dto.post.PostEsDTO;
import com.smartclass.smartclass.service.PostService;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;

import com.smartclass.smartclass.model.entity.Post;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.collection.CollUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 全量同步帖子到 es
*/
@Component
@Slf4j
// 全量重建为运维动作：默认不在启动时执行，需显式传 --rag.full-sync-on-startup=true。
// 增量同步走业务写入路径，不受此开关影响。
public class FullSyncPostToEs implements CommandLineRunner {

    @Resource
    private PostService postService;

    @Autowired(required = false)
    private PostEsDao postEsDao;

    @Override
    public void run(String... args) {
        if (postEsDao == null) {
            log.info("Elasticsearch不可用，跳过全量同步帖子");
            return;
        }
        List<Post> postList = postService.list();
        if (CollUtil.isEmpty(postList)) {
            return;
        }
        List<PostEsDTO> postEsDTOList = postList.stream().map(PostEsDTO::objToDto).collect(Collectors.toList());
        final int pageSize = 500;
        int total = postEsDTOList.size();
        log.info("FullSyncPostToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
            postEsDao.saveAll(postEsDTOList.subList(i, end));
        }
        log.info("FullSyncPostToEs end, total {}", total);
    }
}
