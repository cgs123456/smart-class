package com.cgs.smartclass.common;

import com.cgs.smartclass.constant.CommonConstant;
import lombok.Data;

/**
 * 分页请求
*/
@Data
public class PageRequest {

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    public void setPageSize(int pageSize) {
        this.pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}
