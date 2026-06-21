package com.cgs.smartclassbackendmodel.model.dto.payment;

import com.cgs.smartclassbackendcommon.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品查询请求
 */
@Data
public class ProductQueryRequest extends PageRequest implements Serializable {

    /**
     * 商品类型(vip/course/material)
     */
    private String type;

    /**
     * 状态(0-下架 1-上架)
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
