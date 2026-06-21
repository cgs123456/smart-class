package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.common.PageRequest;
import com.cgs.smartclass.model.dto.ebook.EbookReadingUpdateRequest;
import com.cgs.smartclass.model.entity.EbookReadingRecord;
import com.cgs.smartclass.model.vo.EbookReadingRecordVO;

/**
 * 电子书阅读记录服务
 */
public interface EbookReadingRecordService extends IService<EbookReadingRecord> {

    /**
     * 获取我的电子书阅读记录
     *
     * @param userId      用户ID
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    Page<EbookReadingRecordVO> getMyBooks(Long userId, PageRequest pageRequest);

    /**
     * 更新阅读进度
     *
     * @param userId        用户ID
     * @param updateRequest 更新请求
     * @return 是否成功
     */
    boolean updateProgress(Long userId, EbookReadingUpdateRequest updateRequest);

    /**
     * 切换收藏状态
     *
     * @param userId  用户ID
     * @param ebookId 电子书ID
     * @return 是否成功
     */
    boolean toggleFavorite(Long userId, Long ebookId);

    /**
     * 获取阅读记录
     *
     * @param userId  用户ID
     * @param ebookId 电子书ID
     * @return 阅读记录视图
     */
    EbookReadingRecordVO getReadingRecord(Long userId, Long ebookId);
}
