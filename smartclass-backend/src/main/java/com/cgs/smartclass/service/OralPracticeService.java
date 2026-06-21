package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeCreateRequest;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeQueryRequest;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeSubmitRequest;
import com.cgs.smartclass.model.entity.OralPractice;
import com.cgs.smartclass.model.vo.OralPracticeRecordVO;
import com.cgs.smartclass.model.vo.OralPracticeVO;

/**
 * 口语练习题目服务
 */
public interface OralPracticeService extends IService<OralPractice> {

    /**
     * 分页查询口语练习题目
     *
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<OralPracticeVO> listPractices(OralPracticeQueryRequest queryRequest);

    /**
     * 根据ID获取口语练习题目
     *
     * @param id 题目ID
     * @return 题目视图
     */
    OralPracticeVO getPracticeById(Long id);

    /**
     * 添加口语练习题目（管理员）
     *
     * @param createRequest 创建请求
     * @return 新增ID
     */
    Long addPractice(OralPracticeCreateRequest createRequest);

    /**
     * 提交口语练习
     *
     * @param userId         用户ID
     * @param submitRequest  提交请求
     * @return 练习记录视图
     */
    OralPracticeRecordVO submitPractice(Long userId, OralPracticeSubmitRequest submitRequest);

    /**
     * 获取我的练习记录
     *
     * @param userId      用户ID
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    Page<OralPracticeRecordVO> getMyRecords(Long userId, com.cgs.smartclass.common.PageRequest pageRequest);
}
