package com.cgs.smartclassbackendpay.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclassbackendmodel.model.dto.payment.ProductQueryRequest;
import com.cgs.smartclassbackendmodel.model.entity.Product;
import com.cgs.smartclassbackendmodel.model.vo.ProductVO;

/**
 * 商品服务
 */
public interface ProductService extends IService<Product> {

    Page<ProductVO> listProducts(ProductQueryRequest request);

    ProductVO getProductById(Long id);

    Long addProduct(Product product);

    boolean updateProduct(Product product);

    boolean deleteProduct(Long id);
}
