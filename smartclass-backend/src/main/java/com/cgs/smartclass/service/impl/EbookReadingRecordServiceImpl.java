package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.PageRequest;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.mapper.EbookReadingRecordMapper;
import com.cgs.smartclass.model.dto.ebook.EbookReadingUpdateRequest;
import com.cgs.smartclass.model.entity.Ebook;
import com.cgs.smartclass.model.entity.EbookReadingRecord;
import com.cgs.smartclass.model.vo.EbookReadingRecordVO;
import com.cgs.smartclass.service.EbookReadingRecordService;
import com.cgs.smartclass.service.EbookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 电子书阅读记录服务实现
 */
@Service
@Slf4j
public class EbookReadingRecordServiceImpl extends ServiceImpl<EbookReadingRecordMapper, EbookReadingRecord>
        implements EbookReadingRecordService {

    @Resource
    private EbookService ebookService;

    @Override
    public Page<EbookReadingRecordVO> getMyBooks(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = pageRequest.getCurrent();
        long size = pageRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<EbookReadingRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("lastReadTime");

        Page<EbookReadingRecord> page = this.page(new Page<>(current, size), queryWrapper);
        Page<EbookReadingRecordVO> voPage = new Page<>(current, size, page.getTotal());
        List<EbookReadingRecordVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean updateProgress(Long userId, EbookReadingUpdateRequest updateRequest) {
        if (userId == null || updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (updateRequest.getEbookId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "电子书ID不能为空");
        }

        // 查找或创建阅读记录
        QueryWrapper<EbookReadingRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("ebookId", updateRequest.getEbookId());
        EbookReadingRecord record = this.getOne(queryWrapper);

        if (record == null) {
            // 新建阅读记录
            record = new EbookReadingRecord();
            record.setUserId(userId);
            record.setEbookId(updateRequest.getEbookId());
            record.setProgress(updateRequest.getProgress());
            record.setLastReadPage(updateRequest.getLastReadPage());
            record.setLastReadTime(new Date());
            record.setIsFavorite(0);
            return this.save(record);
        } else {
            // 更新阅读记录
            if (updateRequest.getProgress() != null) {
                record.setProgress(updateRequest.getProgress());
            }
            if (updateRequest.getLastReadPage() != null) {
                record.setLastReadPage(updateRequest.getLastReadPage());
            }
            record.setLastReadTime(new Date());
            return this.updateById(record);
        }
    }

    @Override
    public boolean toggleFavorite(Long userId, Long ebookId) {
        if (userId == null || ebookId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<EbookReadingRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("ebookId", ebookId);
        EbookReadingRecord record = this.getOne(queryWrapper);

        if (record == null) {
            // 新建阅读记录并收藏
            record = new EbookReadingRecord();
            record.setUserId(userId);
            record.setEbookId(ebookId);
            record.setProgress(java.math.BigDecimal.ZERO);
            record.setLastReadPage(0);
            record.setLastReadTime(new Date());
            record.setIsFavorite(1);
            return this.save(record);
        } else {
            // 切换收藏状态
            record.setIsFavorite(record.getIsFavorite() == 1 ? 0 : 1);
            return this.updateById(record);
        }
    }

    @Override
    public EbookReadingRecordVO getReadingRecord(Long userId, Long ebookId) {
        if (userId == null || ebookId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<EbookReadingRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("ebookId", ebookId);
        EbookReadingRecord record = this.getOne(queryWrapper);

        if (record == null) {
            return null;
        }
        return convertToVO(record);
    }

    /**
     * 实体转VO
     */
    private EbookReadingRecordVO convertToVO(EbookReadingRecord record) {
        if (record == null) {
            return null;
        }
        EbookReadingRecordVO vo = new EbookReadingRecordVO();
        BeanUtils.copyProperties(record, vo);

        // 填充电子书信息
        try {
            Ebook ebook = ebookService.getById(record.getEbookId());
            if (ebook != null) {
                vo.setEbookTitle(ebook.getTitle());
                vo.setEbookCoverUrl(ebook.getCoverUrl());
                vo.setEbookAuthor(ebook.getAuthor());
            }
        } catch (Exception e) {
            log.warn("获取电子书信息失败, ebookId={}", record.getEbookId(), e);
        }

        return vo;
    }
}
