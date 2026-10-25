package com.smartclass.smartclass.model.vo;

import com.smartclass.smartclass.model.entity.UserWordBook;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户生词本视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserWordBookVO extends UserWordBook implements Serializable {

    /**
     * 单词内容
     */
    /**
     * 生词本记录 id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 单词 id
     */
    private Long wordId;

    /**
     * 学习状态（0 未掌握 / 1 已掌握）
     */
    private Integer learningStatus;

    /**
     * 是否收藏
     */
    private Integer isCollected;

    /**
     * 收藏时间
     */
    private Date collectedTime;

    /**
     * 难度
     */
    private Integer difficulty;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 单词内容
     */
    private String word;

    /**
     * 单词翻译
     */
    private String translation;

    /**
     * 单词音标
     */
    private String phonetic;

    /**
     * 发音URL
     */
    private String pronunciation;

    /**
     * 单词示例
     */
    private String example;

    private static final long serialVersionUID = 1L;
} 