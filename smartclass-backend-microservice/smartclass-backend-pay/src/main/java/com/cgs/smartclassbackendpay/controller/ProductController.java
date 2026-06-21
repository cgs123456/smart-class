package com.cgs.smartclassbackendpay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclassbackendcommon.annotation.AuthCheck;
import com.cgs.smartclassbackendcommon.common.BaseResponse;
import com.cgs.smartclassbackendcommon.common.ResultUtils;
import com.cgs.smartclassbackendcommon.constant.UserConstant;
import com.cgs.smartclassbackendmodel.model.dto.payment.ProductQueryRequest;
import com.cgs.smartclassbackendmodel.model.entity.Product;
import com.cgs.smartclassbackendmodel.model.vo.ProductVO;
import com.cgs.smartclassbackendpay.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/product")
@Slf4j
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 商品列表
     */
    @GetMapping("/list")
    public BaseResponse<Page<ProductVO>> listProducts(ProductQueryRequest request) {
        Page<ProductVO> page = productService.listProducts(request);
        return ResultUtils.success(page);
    }

    /**
     * 商品详情
     */
    @GetMapping("/{id}")
    public BaseResponse<ProductVO> getProductById(@PathVariable Long id) {
        ProductVO vo = productService.getProductById(id);
        return ResultUtils.success(vo);
    }

    /**
     * 添加商品
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addProduct(@RequestBody Product product) {
        Long id = productService.addProduct(product);
        return ResultUtils.success(id);
    }

    /**
     * 更新商品
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateProduct(@RequestBody Product product) {
        boolean result = productService.updateProduct(product);
        return ResultUtils.success(result);
    }

    /**
     * 删除商品
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteProduct(@RequestBody Product product) {
        boolean result = productService.deleteProduct(product.getId());
        return ResultUtils.success(result);
    }
}
