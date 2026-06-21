package com.cgs.smartclass.model.dto.postfavour;

import java.io.Serializable;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 帖子收藏添加请求
 */
@Data
public class PostFavourAddRequest implements Serializable {

    /**
     * 帖子id
     */
    @NotNull(message = "帖子id不能为空")
    private Long postId;

    private static final long serialVersionUID = 1L;
}