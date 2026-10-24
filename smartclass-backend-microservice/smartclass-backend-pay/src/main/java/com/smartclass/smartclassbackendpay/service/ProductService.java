package com.smartclass.smartclassbackendpay.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartclass.smartclassbackendmodel.model.dto.payment.ProductQueryRequest;
import com.smartclass.smartclassbackendmodel.model.entity.Product;
import com.smartclass.smartclassbackendmodel.model.vo.ProductVO;

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
