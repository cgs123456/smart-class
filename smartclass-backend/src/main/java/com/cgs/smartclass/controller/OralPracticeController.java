package com.cgs.smartclass.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclass.annotation.AuthCheck;
import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.PageRequest;
import com.cgs.smartclass.common.ResultUtils;
import com.cgs.smartclass.constant.UserConstant;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeCreateRequest;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeQueryRequest;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeSubmitRequest;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.OralPracticeRecordVO;
import com.cgs.smartclass.model.vo.OralPracticeVO;
import com.cgs.smartclass.service.OralPracticeService;
import com.cgs.smartclass.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 口语练习接口
 */
@RestController
@RequestMapping("/api/oral-practice")
@Slf4j
public class OralPracticeController {

    @Resource
    private OralPracticeService oralPracticeService;

    @Resource
    private UserService userService;

    /**
     * 分页获取口语练习题目列表
     */
    @GetMapping("/list")
    public BaseResponse<Page<OralPracticeVO>> listPractices(OralPracticeQueryRequest queryRequest) {
        Page<OralPracticeVO> page = oralPracticeService.listPractices(queryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 根据ID获取口语练习题目
     */
    @GetMapping("/{id}")
    public BaseResponse<OralPracticeVO> getPracticeById(@PathVariable("id") Long id) {
        OralPracticeVO vo = oralPracticeService.getPracticeById(id);
        return ResultUtils.success(vo);
    }

    /**
     * 添加口语练习题目（仅管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addPractice(@RequestBody OralPracticeCreateRequest createRequest) {
        if (createRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = oralPracticeService.addPractice(createRequest);
        return ResultUtils.success(id);
    }

    /**
     * 提交口语练习
     */
    @PostMapping("/submit")
    public BaseResponse<OralPracticeRecordVO> submitPractice(@RequestBody OralPracticeSubmitRequest submitRequest,
                                                              HttpServletRequest request) {
        if (submitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        OralPracticeRecordVO vo = oralPracticeService.submitPractice(loginUser.getId(), submitRequest);
        return ResultUtils.success(vo);
    }

    /**
     * 获取我的练习记录
     */
    @GetMapping("/records/my")
    public BaseResponse<Page<OralPracticeRecordVO>> getMyRecords(PageRequest pageRequest,
                                                                  HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<OralPracticeRecordVO> page = oralPracticeService.getMyRecords(loginUser.getId(), pageRequest);
        return ResultUtils.success(page);
    }
}
