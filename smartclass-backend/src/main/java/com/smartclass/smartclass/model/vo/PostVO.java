package com.smartclass.smartclass.model.vo;

import cn.hutool.json.JSONUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.smartclass.smartclass.model.entity.Post;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 帖子视图
*/
@Data
public class PostVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer thumbNum;

    /**
     * 收藏数
     */
    private Integer favourNum;

    /**
     * 评论数
     */
    private Integer commentNum;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 国家
     */
    private String country;

    /**
     * 城市
     */
    private String city;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 创建人信息
     */
    private UserVO user;

    /**
     * 是否已点赞
     */
    private Boolean hasThumb;

    /**
     * 是否已收藏
     */
    private Boolean hasFavour;

    /**
     * 包装类转对象
     *
     * @param postVO
     * @return
     */
    public static Post voToObj(PostVO postVO) {
        if (postVO == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postVO, post);
        List<String> tagList = postVO.getTagList();
        post.setTags(JSONUtil.toJsonStr(tagList));
        return post;
    }

    /**
     * 对象转包装类
     *
     * @param post
     * @return
     */
    public static PostVO objToVo(Post post) {
        if (post == null) {
            return null;
        }
        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);
        postVO.setTagList(parseTags(post.getTags()));
        return postVO;
    }

    /**
     * tags 列历史上存在两种存储格式：JSON 数组字符串与逗号分隔字符串，均需兼容。
     * 与 PostEsDTO.parseTags 保持同一语义，暂不抽象：两处字段语义独立演进。
     */
    private static List<String> parseTags(String tagsStr) {
        if (StringUtils.isBlank(tagsStr)) {
            return new ArrayList<>();
        }
        String trimmed = tagsStr.trim();
        if (trimmed.startsWith("[")) {
            try {
                return JSONUtil.toList(trimmed, String.class);
            } catch (Exception ignored) {
                // 非法 JSON 时退回逗号分隔解析
            }
        }
        List<String> result = new ArrayList<>();
        for (String part : trimmed.split("[，,]")) {
            if (StringUtils.isNotBlank(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }
}
