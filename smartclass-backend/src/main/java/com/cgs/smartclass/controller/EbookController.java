package com.cgs.smartclass.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclass.annotation.AuthCheck;
import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.ResultUtils;
import com.cgs.smartclass.constant.UserConstant;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.model.dto.ebook.EbookAddRequest;
import com.cgs.smartclass.model.dto.ebook.EbookQueryRequest;
import com.cgs.smartclass.model.entity.Ebook;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.EbookVO;
import com.cgs.smartclass.service.EbookService;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.service.UserVipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 电子书接口
 */
@RestController
@RequestMapping("/api/ebook")
@Slf4j
public class EbookController {

    @Resource
    private EbookService ebookService;

    @Resource
    private UserVipService userVipService;

    @Resource
    private UserService userService;

    /**
     * 分页获取电子书列表
     */
    @GetMapping("/list")
    public BaseResponse<Page<EbookVO>> listEbooks(EbookQueryRequest queryRequest) {
        Page<EbookVO> page = ebookService.listEbooks(queryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 根据ID获取电子书
     */
    @GetMapping("/{id}")
    public BaseResponse<EbookVO> getEbookById(@PathVariable("id") Long id, HttpServletRequest request) {
        EbookVO vo = ebookService.getEbookById(id);
        // VIP专属检查
        if (vo != null && vo.getIsVipOnly() != null && vo.getIsVipOnly() == 1) {
            try {
                User loginUser = userService.getLoginUserPermitNull(request);
                if (loginUser == null || !userVipService.checkPrivilege(loginUser.getId(), "ebook_vip")) {
                    // 非VIP用户隐藏文件URL
                    vo.setFileUrl(null);
                }
            } catch (Exception e) {
                // 未登录用户隐藏文件URL
                vo.setFileUrl(null);
            }
        }
        return ResultUtils.success(vo);
    }

    /**
     * 添加电子书（仅管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addEbook(@RequestBody EbookAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = ebookService.addEbook(addRequest);
        return ResultUtils.success(id);
    }

    /**
     * 更新电子书（仅管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateEbook(@RequestBody EbookAddRequest updateRequest) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Ebook ebook = new Ebook();
        BeanUtils.copyProperties(updateRequest, ebook);
        boolean result = ebookService.updateEbook(ebook);
        return ResultUtils.success(result);
    }

    /**
     * 删除电子书（仅管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteEbook(@RequestBody Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = ebookService.deleteEbook(id);
        return ResultUtils.success(result);
    }
}
