package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.constant.CommonConstant;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.mapper.PostFavourMapper;
import com.cgs.smartclass.mapper.PostMapper;
import com.cgs.smartclass.mapper.PostThumbMapper;
import com.cgs.smartclass.model.dto.post.PostEsDTO;
import com.cgs.smartclass.model.dto.post.PostQueryRequest;
import com.cgs.smartclass.model.entity.PostFavour;
import com.cgs.smartclass.model.entity.PostThumb;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.PostVO;
import com.cgs.smartclass.model.vo.UserVO;
import com.cgs.smartclass.service.PostService;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.utils.SqlUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import com.cgs.smartclass.model.entity.Post;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.collection.CollUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import com.cgs.smartclass.esdao.PostEsDao;

/**
 * 帖子服务实现
*/
@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Resource
    private UserService userService;

    @Resource
    private PostThumbMapper postThumbMapper;

    @Resource
    private PostFavourMapper postFavourMapper;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired(required = false)
    private PostEsDao postEsDao;

    @Override
    public void validPost(Post post, boolean add) {
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = post.getTitle();
        String content = post.getContent();
        String tags = post.getTags();
        String type = post.getType();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags, type), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    /**
     * 获取查询包装类
     *
     * @param postQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        if (postQueryRequest == null) {
            return queryWrapper;
        }
        String searchText = postQueryRequest.getSearchText();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        Long id = postQueryRequest.getId();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        List<String> tagList = postQueryRequest.getTags();
        Long userId = postQueryRequest.getUserId();
        Long notId = postQueryRequest.getNotId();
        // 拼接查询条件
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), "id", notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Page<Post> searchFromEs(PostQueryRequest postQueryRequest) {
        // ES不可用时回退到数据库查询
        if (elasticsearchOperations == null) {
            log.warn("Elasticsearch不可用，回退到数据库查询");
            QueryWrapper<Post> queryWrapper = getQueryWrapper(postQueryRequest);
            return this.page(new Page<>(postQueryRequest.getCurrent(), postQueryRequest.getPageSize()), queryWrapper);
        }
        String searchText = postQueryRequest.getSearchText();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        Long userId = postQueryRequest.getUserId();
        long current = postQueryRequest.getCurrent() - 1;
        long pageSize = postQueryRequest.getPageSize();
        
        // 使用 Criteria 构建查询
        Criteria criteria = new Criteria("isDelete").is(0);
        
        if (userId != null) {
            criteria = criteria.and(new Criteria("userId").is(userId));
        }
        if (StringUtils.isNotBlank(searchText)) {
            criteria = criteria.or(new Criteria("title").is(searchText))
                    .or(new Criteria("description").is(searchText))
                    .or(new Criteria("content").is(searchText));
        }
        if (StringUtils.isNotBlank(title)) {
            criteria = criteria.or(new Criteria("title").is(title));
        }
        if (StringUtils.isNotBlank(content)) {
            criteria = criteria.or(new Criteria("content").is(content));
        }
        
        Query searchQuery = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of((int) current, (int) pageSize));
        
        SearchHits<PostEsDTO> searchHits = elasticsearchOperations.search(searchQuery, PostEsDTO.class);
        
        Page<Post> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<Post> resourceList = new ArrayList<>();
        
        if (searchHits.hasSearchHits()) {
            List<Long> postIdList = searchHits.getSearchHits().stream()
                    .map(searchHit -> searchHit.getContent().getId())
                    .collect(Collectors.toList());
            List<Post> postList = baseMapper.selectBatchIds(postIdList);
            if (postList != null) {
                Map<Long, List<Post>> idPostMap = postList.stream().collect(Collectors.groupingBy(Post::getId));
                postIdList.forEach(postId -> {
                    if (idPostMap.containsKey(postId)) {
                        resourceList.add(idPostMap.get(postId).get(0));
                    } else {
                        String delete = elasticsearchOperations.delete(String.valueOf(postId), PostEsDTO.class);
                        log.info("delete post {}", delete);
                    }
                });
            }
        }
        page.setRecords(resourceList);
        return page;
    }

    @Override
    public Page<Post> searchFromEs(String searchText) {
        if (StringUtils.isBlank(searchText)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索词不能为空");
        }
        // ES不可用时回退到数据库查询
        if (elasticsearchOperations == null) {
            log.warn("Elasticsearch不可用，回退到数据库查询");
            QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
            return this.page(new Page<>(1, 10), queryWrapper);
        }
        
        long current = 0;
        long pageSize = 10;
        
        Criteria criteria = new Criteria("isDelete").is(0)
                .or(new Criteria("title").is(searchText))
                .or(new Criteria("description").is(searchText))
                .or(new Criteria("content").is(searchText));
        
        Query searchQuery = new CriteriaQuery(criteria)
                .setPageable(PageRequest.of((int) current, (int) pageSize));
        
        SearchHits<PostEsDTO> searchHits = elasticsearchOperations.search(searchQuery, PostEsDTO.class);
        
        Page<Post> page = new Page<>();
        page.setTotal(searchHits.getTotalHits());
        List<Post> resourceList = new ArrayList<>();
        
        if (searchHits.hasSearchHits()) {
            List<Long> postIdList = searchHits.getSearchHits().stream()
                    .map(searchHit -> searchHit.getContent().getId())
                    .collect(Collectors.toList());
            List<Post> postList = baseMapper.selectBatchIds(postIdList);
            if (postList != null) {
                Map<Long, List<Post>> idPostMap = postList.stream()
                        .collect(Collectors.groupingBy(Post::getId));
                postIdList.forEach(postId -> {
                    if (idPostMap.containsKey(postId)) {
                        resourceList.add(idPostMap.get(postId).get(0));
                    } else {
                        String delete = elasticsearchOperations.delete(String.valueOf(postId), PostEsDTO.class);
                        log.info("delete post {}", delete);
                    }
                });
            }
        }
        page.setRecords(resourceList);
        return page;
    }

    @Override
    public PostVO getPostVO(Post post, HttpServletRequest request) {
        PostVO postVO = PostVO.objToVo(post);
        long postId = post.getId();
        // 1. 关联查询用户信息
        Long userId = post.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        postVO.setUser(userVO);
        
        // 查询评论数量
        int commentCount = baseMapper.getCommentCount(postId);
        postVO.setCommentNum(commentCount);
        
        // 2. 已登录，获取用户点赞、收藏状态
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            // 获取点赞
            QueryWrapper<PostThumb> postThumbQueryWrapper = new QueryWrapper<>();
            postThumbQueryWrapper.in("postId", postId);
            postThumbQueryWrapper.eq("userId", loginUser.getId());
            PostThumb postThumb = postThumbMapper.selectOne(postThumbQueryWrapper);
            postVO.setHasThumb(postThumb != null);
            // 获取收藏
            QueryWrapper<PostFavour> postFavourQueryWrapper = new QueryWrapper<>();
            postFavourQueryWrapper.in("postId", postId);
            postFavourQueryWrapper.eq("userId", loginUser.getId());
            PostFavour postFavour = postFavourMapper.selectOne(postFavourQueryWrapper);
            postVO.setHasFavour(postFavour != null);
        }
        return postVO;
    }

    @Override
    public Page<PostVO> getPostVOPage(Page<Post> postPage, HttpServletRequest request) {
        List<Post> postList = postPage.getRecords();
        Page<PostVO> postVOPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        if (CollUtil.isEmpty(postList)) {
            return postVOPage;
        }
        
        // 1. 关联查询用户信息
        Set<Long> userIdSet = postList.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        
        // 2. 批量查询评论数量
        List<Long> postIdList = postList.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Integer> postIdCommentCountMap = new HashMap<>();
        List<Map<String, Object>> commentCountList = baseMapper.batchGetCommentCount(postIdList);
        for (Map<String, Object> map : commentCountList) {
            Long postId = (Long) map.get("postId");
            Integer count = ((Number) map.get("count")).intValue();
            postIdCommentCountMap.put(postId, count);
        }
        
        // 3. 已登录，获取用户点赞、收藏状态
        Map<Long, Boolean> postIdHasThumbMap = new HashMap<>();
        Map<Long, Boolean> postIdHasFavourMap = new HashMap<>();
        User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            loginUser = userService.getLoginUser(request);
            // 获取点赞
            QueryWrapper<PostThumb> postThumbQueryWrapper = new QueryWrapper<>();
            postThumbQueryWrapper.in("postId", postIdList);
            postThumbQueryWrapper.eq("userId", loginUser.getId());
            List<PostThumb> postPostThumbList = postThumbMapper.selectList(postThumbQueryWrapper);
            postPostThumbList.forEach(postPostThumb -> postIdHasThumbMap.put(postPostThumb.getPostId(), true));
            // 获取收藏
            QueryWrapper<PostFavour> postFavourQueryWrapper = new QueryWrapper<>();
            postFavourQueryWrapper.in("postId", postIdList);
            postFavourQueryWrapper.eq("userId", loginUser.getId());
            List<PostFavour> postFavourList = postFavourMapper.selectList(postFavourQueryWrapper);
            postFavourList.forEach(postFavour -> postIdHasFavourMap.put(postFavour.getPostId(), true));
        }
        
        // 填充信息
        List<PostVO> postVOList = postList.stream().map(post -> {
            PostVO postVO = PostVO.objToVo(post);
            Long userId = post.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            postVO.setUser(userService.getUserVO(user));
            postVO.setHasThumb(postIdHasThumbMap.getOrDefault(post.getId(), false));
            postVO.setHasFavour(postIdHasFavourMap.getOrDefault(post.getId(), false));
            // 设置评论数
            postVO.setCommentNum(postIdCommentCountMap.getOrDefault(post.getId(), 0));
            return postVO;
        }).collect(Collectors.toList());
        
        postVOPage.setRecords(postVOList);
        return postVOPage;
    }

    /**
     * 保存帖子到数据库，同时同步到ES
     * @param post 帖子
     * @return 结果
     */
    @Override
    public boolean savePost(Post post) {
        boolean result = this.save(post);
        if (result && postEsDao != null) {
            try {
                // 同步到ES
                PostEsDTO postEsDTO = PostEsDTO.objToDto(post);
                postEsDao.save(postEsDTO);
                log.info("同步新增帖子到ES成功, id={}", post.getId());
            } catch (Exception e) {
                log.error("同步新增帖子到ES失败", e);
            }
        }
        return result;
    }

    /**
     * 更新帖子，同时更新ES
     * @param post 帖子
     * @return 结果
     */
    @Override
    public boolean updatePost(Post post) {
        boolean result = this.updateById(post);
        if (result && postEsDao != null) {
            try {
                // 同步到ES
                PostEsDTO postEsDTO = PostEsDTO.objToDto(post);
                postEsDao.save(postEsDTO);
                log.info("同步更新帖子到ES成功, id={}", post.getId());
            } catch (Exception e) {
                log.error("同步更新帖子到ES失败", e);
            }
        }
        return result;
    }

    /**
     * 删除帖子，同时从ES删除
     * @param id 帖子id
     * @return 结果
     */
    @Override
    public boolean deletePost(Long id) {
        boolean result = this.removeById(id);
        if (result && postEsDao != null) {
            try {
                // 从ES中删除
                postEsDao.deleteById(id);
                log.info("同步删除帖子到ES成功, id={}", id);
            } catch (Exception e) {
                log.error("同步删除帖子到ES失败", e);
            }
        }
        return result;
    }

}




