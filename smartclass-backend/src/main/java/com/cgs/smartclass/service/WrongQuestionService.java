package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionAddRequest;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionQueryRequest;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionReviewRequest;
import com.cgs.smartclass.model.entity.WrongQuestion;
import com.cgs.smartclass.model.vo.WrongQuestionVO;

import java.util.List;

/**
 * 错题本服务
 */
public interface WrongQuestionService extends IService<WrongQuestion> {

    /**
     * 添加错题
     *
     * @param userId     用户ID
     * @param addRequest 添加请求
     * @return 新增ID
     */
    Long addWrongQuestion(Long userId, WrongQuestionAddRequest addRequest);

    /**
     * 分页查询我的错题
     *
     * @param userId       用户ID
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<WrongQuestionVO> listMyWrongQuestions(Long userId, WrongQuestionQueryRequest queryRequest);

    /**
     * 根据ID获取错题
     *
     * @param userId 用户ID
     * @param id     错题ID
     * @return 错题视图
     */
    WrongQuestionVO getWrongQuestionById(Long userId, Long id);

    /**
     * 复习错题
     *
     * @param userId         用户ID
     * @param reviewRequest  复习请求
     * @return 是否成功
     */
    boolean reviewWrongQuestion(Long userId, WrongQuestionReviewRequest reviewRequest);

    /**
     * 获取需要复习的错题列表
     *
     * @param userId 用户ID
     * @return 错题列表
     */
    List<WrongQuestionVO> getReviewList(Long userId);

    /**
     * 删除错题
     *
     * @param userId 用户ID
     * @param id     错题ID
     * @return 是否成功
     */
    boolean deleteWrongQuestion(Long userId, Long id);
}
