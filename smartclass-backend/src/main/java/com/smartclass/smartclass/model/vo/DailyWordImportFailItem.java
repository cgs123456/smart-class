package com.smartclass.smartclass.model.vo;

import java.io.Serializable;
import lombok.Data;

/**
 * 每日单词导入失败项（行号、单词、失败原因）
*/
@Data
public class DailyWordImportFailItem implements Serializable {

    /**
     * CSV 行号（从 2 开始计，1 为表头）
     */
    private Integer row;

    /**
     * 单词
     */
    private String word;

    /**
     * 失败原因
     */
    private String reason;

    private static final long serialVersionUID = 1L;
}
