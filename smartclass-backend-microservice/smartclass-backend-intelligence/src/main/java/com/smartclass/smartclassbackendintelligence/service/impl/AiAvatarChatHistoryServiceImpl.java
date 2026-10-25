package com.smartclass.smartclassbackendintelligence.service.impl;

import org.apache.dubbo.config.annotation.DubboReference;

import com.smartclass.smartclassbackendserviceclient.dubbo.UserDubboService;

import com.smartclass.smartclassbackendserviceclient.utils.UserSessionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclassbackendintelligence.mapper.AiAvatarChatHistoryMapper;
import com.smartclass.smartclassbackendintelligence.service.AiAvatarChatHistoryService;
import com.smartclass.smartclassbackendintelligence.service.AiAvatarService;
import com.smartclass.smartclassbackendmodel.model.entity.AiAvatar;
import com.smartclass.smartclassbackendmodel.model.entity.AiAvatarChatHistory;
import com.smartclass.smartclassbackendmodel.model.entity.User;
import com.smartclass.smartclassbackendmodel.model.vo.ChatMessageVO;
import com.smartclass.smartclassbackendmodel.model.vo.ChatSessionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description 针对表【ai_avatar_chat_history(AI分身对话历史)】的数据库操作Service实现
 * @createDate 2025-03-24 21:35:44
 */
@Service
public class AiAvatarChatHistoryServiceImpl extends ServiceImpl<AiAvatarChatHistoryMapper, AiAvatarChatHistory>
    implements AiAvatarChatHistoryService {

    @DubboReference(interfaceClass = UserDubboService.class, version = "1.0.0", protocol = "triple")
    private UserDubboService userDubboService;
    
    private static final Logger log = LoggerFactory.getLogger(AiAvatarChatHistoryServiceImpl.class);
    
    @Resource
    private AiAvatarService aiAvatarService;
    
    
    @Resource
    private ApplicationContext applicationContext;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveMessage(Long userId, Long aiAvatarId, String sessionId, String messageType, String content) {
        if (userId == null || aiAvatarId == null || sessionId == null || messageType == null || content == null) {
            return false;
        }
        
        // 过滤"会话已创建"的系统消息
        if ("system".equals(messageType) && "会话已创建".equals(content)) {
            // 不保存这类消息到数据库
            return true;
        }
        
        AiAvatarChatHistory chatHistory = new AiAvatarChatHistory();
        chatHistory.setUserId(userId);
        chatHistory.setAiAvatarId(aiAvatarId);
        chatHistory.setSessionId(sessionId);
        chatHistory.setMessageType(messageType);
        chatHistory.setContent(content);
        chatHistory.setCreateTime(new Date());
        
        // 计算tokens (简单实现)
        int tokens = content.length() / 4; // 简单估算，4个字符约等于1个token
        chatHistory.setTokens(tokens);
        
        return this.save(chatHistory);
    }
    
    @Override
    public String createNewSession(Long userId, Long aiAvatarId) {
        // 生成标准UUID格式的会话ID
        String sessionId = UUID.randomUUID().toString();
        
        // 不再创建"会话已创建"的初始记录
        // 直接返回新生成的会话ID
        return sessionId;
    }
    
    @Override
    public List<ChatSessionVO> getUserSessions(Long userId, Long aiAvatarId) {
        // 查询用户与AI分身的所有会话，获取会话ID和名称
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        
        if (aiAvatarId != null) {
            queryWrapper.eq("aiAvatarId", aiAvatarId);
        }
        
        // GROUP BY 之后再 ORDER BY MAX(createTime) 才是合法 SQL（DISTINCT 与聚合排序不兼容）
        queryWrapper.select("sessionId", "sessionName", "aiAvatarId")
                .groupBy("sessionId", "sessionName", "aiAvatarId")
                .orderByDesc("MAX(createTime)");
        
        List<Map<String, Object>> sessionsList = this.listMaps(queryWrapper);
        
        List<String> sessionIds = sessionsList.stream()
                .map(session -> (String) session.get("sessionId"))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        return buildSessionVOs(userId, sessionIds);
    }
    
    @Override
    public List<AiAvatarChatHistory> getSessionHistory(String sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }
        
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sessionId", sessionId)
                .orderByAsc("createTime");
        
        return this.list(queryWrapper);
    }
    
    @Override
    public Page<AiAvatarChatHistory> getSessionHistoryPage(String sessionId, long current, long size) {
        if (sessionId == null) {
            return new Page<>(current, size);
        }
        
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sessionId", sessionId)
                .orderByAsc("createTime");
        
        return this.page(new Page<>(current, size), queryWrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSessionName(String sessionId, String sessionName) {
        if (sessionId == null || sessionName == null) {
            return false;
        }
        
        // 查询会话中的所有消息，并更新会话名称
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sessionId", sessionId);
        
        List<AiAvatarChatHistory> chatHistories = this.list(queryWrapper);
        
        if (chatHistories.isEmpty()) {
            return false;
        }
        
        // 更新所有消息的会话名称
        for (AiAvatarChatHistory history : chatHistories) {
            history.setSessionName(sessionName);
        }
        
        return this.updateBatchById(chatHistories);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(String sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            return false;
        }
        
        // 验证用户是否有权限删除该会话
        QueryWrapper<AiAvatarChatHistory> authQuery = new QueryWrapper<>();
        authQuery.eq("sessionId", sessionId)
                .eq("userId", userId)
                .last("LIMIT 1");
        
        AiAvatarChatHistory chatHistory = this.getOne(authQuery);
        
        if (chatHistory == null) {
            return false; // 会话不存在或用户无权限
        }
        
        // 删除会话中的所有消息
        QueryWrapper<AiAvatarChatHistory> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("sessionId", sessionId);
        
        return this.remove(deleteQuery);
    }
    
    @Override
    public Page<AiAvatarChatHistory> getUserHistoryPage(Long userId, Long aiAvatarId, long current, long size) {
        if (userId == null) {
            return new Page<>(current, size);
        }
        
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        
        if (aiAvatarId != null && aiAvatarId > 0) {
            queryWrapper.eq("aiAvatarId", aiAvatarId);
        }
        
        queryWrapper.orderByDesc("createTime");
        
        return this.page(new Page<>(current, size), queryWrapper);
    }
    
    @Override
    public Page<AiAvatarChatHistory> getUserLatestMessagesPage(Long userId, long current, long size) {
        if (userId == null) {
            return new Page<>(current, size);
        }
        
        // 获取用户的所有聊天历史记录中每个会话的最新消息
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .select("MAX(id) as id")
                .groupBy("sessionId")
                .orderByDesc("MAX(createTime)");
        
        // 创建分页对象
        Page<AiAvatarChatHistory> page = new Page<>(current, size);
        
        // 执行自定义SQL查询获取每个会话的最新消息的ID
        String sql = "SELECT MAX(id) as id FROM ai_avatar_chat_history " +
                "WHERE userId = " + userId + " GROUP BY sessionId ORDER BY MAX(createTime) DESC " +
                "LIMIT " + ((current - 1) * size) + ", " + size;
        
        List<Object> idList = this.baseMapper.selectObjs(
                new QueryWrapper<AiAvatarChatHistory>().select("MAX(id) as id")
                .eq("userId", userId)
                .groupBy("sessionId")
                .orderByDesc("MAX(createTime)")
                .last("LIMIT " + ((current - 1) * size) + ", " + size));
        
        if (idList.isEmpty()) {
            // 返回空结果
            return page;
        }
        
        // 获取这些ID对应的记录
        QueryWrapper<AiAvatarChatHistory> recordQuery = new QueryWrapper<>();
        recordQuery.in("id", idList).orderByDesc("createTime");
        List<AiAvatarChatHistory> records = this.list(recordQuery);
        
        // 设置记录和总数
        page.setRecords(records);
        
        // 获取总记录数（会话数）
        Long totalCount = 0L;
        try {
            // 查询不同会话的数量
            Map<String, Object> countResult = this.baseMapper.selectMaps(
                    new QueryWrapper<AiAvatarChatHistory>()
                    .select("COUNT(DISTINCT sessionId) as count")
                    .eq("userId", userId))
                    .get(0);
            
            if (countResult != null && countResult.get("count") != null) {
                // 根据数据库类型，count可能是Long、Integer或BigDecimal等
                Object countObj = countResult.get("count");
                if (countObj instanceof Number) {
                    totalCount = ((Number) countObj).longValue();
                }
            }
        } catch (Exception e) {
            log.error("获取会话总数失败", e);
        }
        
        page.setTotal(totalCount);
        
        return page;
    }
    
    @Override
    public List<ChatSessionVO> getRecentSessions(Long userId, int limit) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // 修复SQL查询，避免DISTINCT和ORDER BY不兼容的问题
        // 先获取用户的会话ID列表，然后单独查询每个会话的最后消息
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .select("MAX(id) as id, sessionId")
                .groupBy("sessionId")
                .orderByDesc("MAX(createTime)")
                .last("LIMIT " + limit);
        
        List<Map<String, Object>> results = this.listMaps(queryWrapper);
        
        List<String> sessionIds = results.stream()
                .map(item -> (String) item.get("sessionId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (sessionIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return buildSessionVOs(userId, sessionIds);
    }
    
    @Override
    public List<ChatMessageVO> getUserMessages(Long userId, Long aiAvatarId) {
        // 构建查询条件
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        
        if (aiAvatarId != null && aiAvatarId > 0) {
            queryWrapper.eq("aiAvatarId", aiAvatarId);
        }
        
        // 按时间倒序排列
        queryWrapper.orderByDesc("createTime");
        
        // 查询数据
        List<AiAvatarChatHistory> messageList = this.list(queryWrapper);
        
        // 转换为VO对象
        List<ChatMessageVO> result = new ArrayList<>();
        
        // AI 分身与用户信息批量化，避免逐消息 getById/getById（Dubbo RPC N+1）
        List<Long> avatarIdSet = messageList.stream()
                .map(AiAvatarChatHistory::getAiAvatarId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, AiAvatar> avatarById = avatarIdSet.isEmpty() ? Collections.emptyMap()
                : aiAvatarService.listByIds(avatarIdSet).stream()
                .collect(Collectors.toMap(AiAvatar::getId, avatar -> avatar));
        List<Long> userIdSet = messageList.stream()
                .map(AiAvatarChatHistory::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userById = userIdSet.isEmpty() ? Collections.emptyMap()
                : userDubboService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        for (AiAvatarChatHistory message : messageList) {
            ChatMessageVO chatMessageVO = new ChatMessageVO();
            BeanUtils.copyProperties(message, chatMessageVO);
            
            // 获取AI分身信息
            AiAvatar aiAvatar = avatarById.get(message.getAiAvatarId());
            if (aiAvatar != null) {
                chatMessageVO.setAiAvatarName(aiAvatar.getName());
                chatMessageVO.setAiAvatarImgUrl(aiAvatar.getAvatarImgUrl());
            }
            
            // 获取用户信息
            User user = userById.get(message.getUserId());
            if (user != null) {
                chatMessageVO.setUserName(user.getUserName());
                chatMessageVO.setUserAvatar(user.getUserAvatar());
            }
            
            result.add(chatMessageVO);
        }
        
        return result;
    }
    
    @Override
    public boolean updateSessionSummary(String sessionId, String summary) {
        if (StringUtils.hasLength(sessionId) == false || StringUtils.hasLength(summary) == false) {
            return false;
        }
        
        // 查询会话中的所有消息
        QueryWrapper<AiAvatarChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sessionId", sessionId);
        
        // 更新所有消息的会话名称
        AiAvatarChatHistory updateEntity = new AiAvatarChatHistory();
        updateEntity.setSessionName(summary);
        
        return this.update(updateEntity, queryWrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSessionCompletely(String sessionId, Long userId, String baseUrl, String avatarAuth) {
        if (!StringUtils.hasLength(sessionId) || userId == null) {
            return false;
        }
        
        // 验证用户是否有权限删除该会话
        QueryWrapper<AiAvatarChatHistory> authQuery = new QueryWrapper<>();
        authQuery.eq("sessionId", sessionId)
                .eq("userId", userId)
                .last("LIMIT 1");
        
        AiAvatarChatHistory chatHistory = this.getOne(authQuery);
        
        if (chatHistory == null) {
            log.warn("会话不存在或用户无权限删除: sessionId={}, userId={}", sessionId, userId);
            return false; // 会话不存在或用户无权限
        }
        
        // 先删除Dify远程会话，使用延迟加载的方式获取DifyService
        boolean difyDeleted = true;
        if (StringUtils.hasLength(baseUrl) && StringUtils.hasLength(avatarAuth)) {
            try {
                // 从ApplicationContext中获取DifyService，避免循环依赖
                Object difyServiceObj = applicationContext.getBean("difyServiceImpl");
                // 反射调用deleteConversation方法
                Class<?> clazz = difyServiceObj.getClass();
                boolean result = (boolean) clazz.getMethod("deleteConversation", Long.class, String.class, String.class, String.class)
                        .invoke(difyServiceObj, userId, sessionId, baseUrl, avatarAuth);
                difyDeleted = result;
                if (!difyDeleted) {
                    log.warn("删除Dify远程会话失败: sessionId={}", sessionId);
                    // 继续删除本地会话，不返回错误
                }
            } catch (Exception e) {
                log.error("调用DifyService.deleteConversation方法异常", e);
                // 继续删除本地会话，不返回错误
            }
        }
        
        // 删除本地会话记录
        QueryWrapper<AiAvatarChatHistory> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("sessionId", sessionId);
        
        boolean localDeleted = this.remove(deleteQuery);
        if (!localDeleted) {
            log.error("删除本地会话记录失败: sessionId={}", sessionId);
            return false;
        }
        
        return true;
    }
    
    @Override
    public List<ChatSessionVO> getHistoryDialogsList(Long userId, int limit, int offset) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        // 查询用户的会话列表，按最后更新时间降序排序
        String sql = "SELECT h.sessionId, h.sessionName, h.aiAvatarId, MAX(h.createTime) as lastTime, " +
                     "COUNT(*) as msgCount " +
                     "FROM ai_avatar_chat_history h " +
                     "WHERE h.userId = ? " +
                     "GROUP BY h.sessionId, h.sessionName, h.aiAvatarId " +
                     "ORDER BY lastTime DESC " +
                     "LIMIT ?, ?";
        
        // 使用自定义SQL查询
        try {
            List<ChatSessionVO> sessionList = new ArrayList<>();
            
            // 使用mybatis-plus的自定义SQL查询
            List<Map<String, Object>> resultMaps = this.baseMapper.selectMaps(
                    new QueryWrapper<AiAvatarChatHistory>()
                            .select("sessionId", "MAX(createTime) as lastTime")
                            .eq("userId", userId)
                            .groupBy("sessionId")
                            .orderByDesc("lastTime")
                            .last("LIMIT " + offset + ", " + limit)
            );
            
            List<String> sessionIds = resultMaps.stream()
                    .map(row -> (String) row.get("sessionId"))
                    .filter(StringUtils::hasLength)
                    .collect(Collectors.toList());
            
            return buildSessionVOs(userId, sessionIds);
        } catch (Exception e) {
            log.error("获取历史对话列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量组装会话 VO：固定 3 组查询（会话计数 / 末条消息 / AI 分身），
     * 消除逐会话 getOne + count + getById 造成的 3N 次查询。
     * 会话顺序沿用传入 sessionIds 的顺序（由调用方按最后活跃时间排好）。
     */
    private List<ChatSessionVO> buildSessionVOs(Long userId, List<String> sessionIds) {
        List<ChatSessionVO> sessions = new ArrayList<>();
        if (sessionIds == null || sessionIds.isEmpty()) {
            return sessions;
        }
        
        // 每个会话的消息数
        Map<String, Long> countBySession = new HashMap<>();
        for (Map<String, Object> row : this.listMaps(new QueryWrapper<AiAvatarChatHistory>()
                .select("sessionId", "COUNT(*) AS cnt")
                .eq(userId != null, "userId", userId)
                .in("sessionId", sessionIds)
                .groupBy("sessionId"))) {
            if (row.get("sessionId") != null) {
                countBySession.put(String.valueOf(row.get("sessionId")), ((Number) row.get("cnt")).longValue());
            }
        }
        
        // 每个会话的最后一条消息 = 会话内 id 最大的记录（createTime 并列时也无歧义）
        List<Long> lastMessageIds = this.listMaps(new QueryWrapper<AiAvatarChatHistory>()
                .select("MAX(id) AS mid")
                .eq(userId != null, "userId", userId)
                .in("sessionId", sessionIds)
                .groupBy("sessionId"))
                .stream()
                .map(row -> ((Number) row.get("mid")).longValue())
                .collect(Collectors.toList());
        Map<String, AiAvatarChatHistory> lastMessageBySession = new HashMap<>();
        if (!lastMessageIds.isEmpty()) {
            for (AiAvatarChatHistory msg : this.listByIds(lastMessageIds)) {
                if (msg.getSessionId() != null) {
                    lastMessageBySession.put(msg.getSessionId(), msg);
                }
            }
        }
        
        // 涉及的 AI 分身信息一次性取回
        Set<Long> avatarIds = lastMessageBySession.values().stream()
                .map(AiAvatarChatHistory::getAiAvatarId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AiAvatar> avatarById = avatarIds.isEmpty() ? Collections.emptyMap()
                : aiAvatarService.listByIds(avatarIds).stream()
                .collect(Collectors.toMap(AiAvatar::getId, avatar -> avatar));
        
        for (String sessionId : sessionIds) {
            AiAvatarChatHistory lastMessage = lastMessageBySession.get(sessionId);
            if (lastMessage == null) {
                continue;
            }
            ChatSessionVO sessionVO = new ChatSessionVO();
            sessionVO.setSessionId(sessionId);
            sessionVO.setSessionName(lastMessage.getSessionName());
            sessionVO.setAiAvatarId(lastMessage.getAiAvatarId());
            sessionVO.setLastMessage(lastMessage.getContent());
            sessionVO.setLastMessageTime(lastMessage.getCreateTime());
            sessionVO.setMessageCount(countBySession.getOrDefault(sessionId, 0L).intValue());
            AiAvatar aiAvatar = avatarById.get(lastMessage.getAiAvatarId());
            if (aiAvatar != null) {
                sessionVO.setAiAvatarName(aiAvatar.getName());
                sessionVO.setAiAvatarImgUrl(aiAvatar.getAvatarImgUrl());
            }
            sessions.add(sessionVO);
        }
        return sessions;
    }
}
