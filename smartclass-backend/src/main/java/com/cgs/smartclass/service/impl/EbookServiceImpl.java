package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.mapper.EbookMapper;
import com.cgs.smartclass.model.dto.ebook.EbookAddRequest;
import com.cgs.smartclass.model.dto.ebook.EbookQueryRequest;
import com.cgs.smartclass.model.entity.Ebook;
import com.cgs.smartclass.model.vo.EbookVO;
import com.cgs.smartclass.service.EbookService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 电子书服务实现
 */
@Service
@Slf4j
public class EbookServiceImpl extends ServiceImpl<EbookMapper, Ebook>
        implements EbookService {

    @Override
    public Page<EbookVO> listEbooks(EbookQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<Ebook> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.eq(StringUtils.isNotBlank(queryRequest.getCategory()), "category", queryRequest.getCategory());
        queryWrapper.eq(StringUtils.isNotBlank(queryRequest.getLevel()), "level", queryRequest.getLevel());
        queryWrapper.eq(StringUtils.isNotBlank(queryRequest.getLanguage()), "language", queryRequest.getLanguage());
        queryWrapper.eq(queryRequest.getIsVipOnly() != null, "isVipOnly", queryRequest.getIsVipOnly());
        queryWrapper.orderByDesc("createTime");

        Page<Ebook> page = this.page(new Page<>(current, size), queryWrapper);
        Page<EbookVO> voPage = new Page<>(current, size, page.getTotal());
        List<EbookVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public EbookVO getEbookById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Ebook ebook = this.getById(id);
        if (ebook == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return convertToVO(ebook);
    }

    @Override
    public Long addEbook(EbookAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(addRequest.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "书名不能为空");
        }
        if (StringUtils.isBlank(addRequest.getFileUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件URL不能为空");
        }
        Ebook ebook = new Ebook();
        BeanUtils.copyProperties(addRequest, ebook);
        ebook.setDownloadCount(0);
        ebook.setStatus(1);
        boolean result = this.save(ebook);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ebook.getId();
    }

    @Override
    public boolean updateEbook(Ebook ebook) {
        if (ebook == null || ebook.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Ebook oldEbook = this.getById(ebook.getId());
        ThrowUtils.throwIf(oldEbook == null, ErrorCode.NOT_FOUND_ERROR);
        return this.updateById(ebook);
    }

    @Override
    public boolean deleteEbook(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Ebook ebook = this.getById(id);
        ThrowUtils.throwIf(ebook == null, ErrorCode.NOT_FOUND_ERROR);
        return this.removeById(id);
    }

    /**
     * 实体转VO
     */
    private EbookVO convertToVO(Ebook ebook) {
        if (ebook == null) {
            return null;
        }
        EbookVO vo = new EbookVO();
        BeanUtils.copyProperties(ebook, vo);
        return vo;
    }
}
