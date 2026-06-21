package com.cgs.smartclass.model.dto.postfavour;

import com.cgs.smartclass.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 帖子收藏查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PostFavourQueryRequest extends PageRequest implements Serializable {
    
    /**
     * 帖子id
     */
    private Long postId;
    
    /**
     * 用户id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}