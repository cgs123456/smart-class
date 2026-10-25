package com.smartclass.smartclass.model.vo;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 每日单词 CSV 批量导入结果视图
*/
@Data
public class DailyWordImportVO implements Serializable {

    /**
     * 总行数（不含表头）
     */
    private Integer total;

    /**
     * 成功导入数
     */
    private Integer success;

    /**
     * 失败数
     */
    private Integer fail;

    /**
     * 失败明细
     */
    private List<DailyWordImportFailItem> failList;

    private static final long serialVersionUID = 1L;
}
