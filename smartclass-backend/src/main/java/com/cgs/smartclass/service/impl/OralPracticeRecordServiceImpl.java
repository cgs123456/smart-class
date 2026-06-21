package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.PageRequest;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.mapper.OralPracticeRecordMapper;
import com.cgs.smartclass.model.entity.OralPracticeRecord;
import com.cgs.smartclass.service.OralPracticeRecordService;
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
