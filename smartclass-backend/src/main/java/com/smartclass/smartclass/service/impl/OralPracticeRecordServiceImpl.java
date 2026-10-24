package com.smartclass.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclass.common.ErrorCode;
import com.smartclass.smartclass.common.PageRequest;
import com.smartclass.smartclass.exception.BusinessException;
import com.smartclass.smartclass.mapper.OralPracticeRecordMapper;
import com.smartclass.smartclass.model.entity.OralPracticeRecord;
import com.smartclass.smartclass.service.OralPracticeRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 口语练习记录服务实现
 */
@Service
@Slf4j
public class OralPracticeRecordServiceImpl extends ServiceImpl<OralPracticeRecordMapper, OralPracticeRecord>
        implements OralPracticeRecordService {

}
