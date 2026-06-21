package com.cgs.smartclass.model.dto.ebook;

import com.cgs.smartclass.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询电子书请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EbookQueryRequest extends PageRequest implements Serializable {

    private String category;

    private String level;

    private String language;

    private Integer isVipOnly;

    private static final long serialVersionUID = 1L;
}
