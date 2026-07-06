package com.cgs.smartclassbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 错题本/AI习题
 * @TableName wrong_question
 */
@TableName(value = "wrong_question")
@Data
public class WrongQuestion implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 题目类型(choice/short_answer)
     */
    private String questionType;

    /**
     * 题目内容
     */
    private String questionContent;

    /**
     * 选择题选项，JSON数组
     */
    private String options;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 解析
     */
    private String analysis;

    /**
     * 来源类型(course/practice/exam)
     */
    private String sourceType;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 关联课程ID
     */
    private Long courseId;

    /**
     * 关联章节ID
     */
    private Long chapterId;

    /**
     * 关联小节ID
     */
    private Long sectionId;

    /**
     * 难度：1-简单，2-中等，3-困难
     */
    private Integer difficulty;

    /**
     * 是否AI生成：0-否，1-是
     */
    private Integer aiGenerated;

    /**
     * 掌握程度(0-未掌握 1-部分掌握 2-已掌握)
     */
    private Integer masteryLevel;

    /**
     * 复习次数
     */
    private Integer reviewCount;

    /**
     * 上次复习时间
     */
    private Date lastReviewTime;

    /**
     * 下次复习时间
     */
    private Date nextReviewTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
