package com.smartclass.smartclass.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclass.mapper.UserArticleRecordMapper;
import com.smartclass.smartclass.model.dto.UserArticleRecord;
import com.smartclass.smartclass.service.UserArticleRecordService;
import org.springframework.stereotype.Service;

/**
 * 用户文章阅读记录服务实现。
 * <p>
 * 生成器只产出了接口和 Mapper，缺少该实现类，导致
 * LearningPathServiceImpl 注入 UserArticleRecordService 失败、整个容器无法启动。
 */
@Service
public class UserArticleRecordServiceImpl extends ServiceImpl<UserArticleRecordMapper, UserArticleRecord>
        implements UserArticleRecordService {

}
