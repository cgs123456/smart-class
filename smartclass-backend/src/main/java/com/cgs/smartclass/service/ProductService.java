package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.dto.payment.ProductQueryRequest;
import com.cgs.smartclass.model.entity.Product;
import com.cgs.smartclass.model.vo.ProductVO;

/**
 * 商品服务
 */
public interface ProductService extends IService<Product> {

    /**
     * 商品列表
     *
     * @param request 查询请求
     * @return 商品分页
     */
    Page<ProductVO> listProducts(ProductQueryRequest request);

    /**
     * 获取商品详情
     *
     * @param id 商品ID
     * @return 商品VO
     */
    ProductVO getProductById(Long id);

    /**
     * 添加商品
     *
     * @param product 商品
     * @return 商品ID
     */
    Long addProduct(Product product);

    /**
     * 更新商品
     *
     * @param product 商品
     * @return 是否成功
     */
    boolean updateProduct(Product product);

    /**
     * 删除商品
     *
     * @param id 商品ID
     * @return 是否成功
     */
    boolean deleteProduct(Long id);
}
