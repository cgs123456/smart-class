package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.mapper.ProductMapper;
import com.cgs.smartclass.model.dto.payment.ProductQueryRequest;
import com.cgs.smartclass.model.entity.Product;
import com.cgs.smartclass.model.vo.ProductVO;
import com.cgs.smartclass.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 */
@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements ProductService {

    @Override
    public Page<ProductVO> listProducts(ProductQueryRequest request) {
        long current = request.getCurrent();
        long size = request.getPageSize();
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(request.getType())) {
            queryWrapper.eq("type", request.getType());
        }
        if (ObjectUtils.isNotEmpty(request.getStatus())) {
            queryWrapper.eq("status", request.getStatus());
        }
        queryWrapper.orderByAsc("sortOrder");
        Page<Product> productPage = this.page(new Page<>(current, size), queryWrapper);
        Page<ProductVO> voPage = new Page<>(current, size, productPage.getTotal());
        List<ProductVO> voList = productPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ProductVO getProductById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Product product = this.getById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        return convertToVO(product);
    }

    @Override
    public Long addProduct(Product product) {
        if (product == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(product.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品名称不能为空");
        }
        if (product.getPrice() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品价格不能为空");
        }
        if (StringUtils.isBlank(product.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品类型不能为空");
        }
        boolean result = this.save(product);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加商品失败");
        }
        return product.getId();
    }

    @Override
    public boolean updateProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Product oldProduct = this.getById(product.getId());
        if (oldProduct == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        return this.updateById(product);
    }

    @Override
    public boolean deleteProduct(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Product product = this.getById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        return this.removeById(id);
    }

    /**
     * 转换为VO
     */
    private ProductVO convertToVO(Product product) {
        if (product == null) {
            return null;
        }
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }
}
