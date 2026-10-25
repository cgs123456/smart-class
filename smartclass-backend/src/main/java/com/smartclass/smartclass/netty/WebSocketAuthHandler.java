package com.smartclass.smartclass.netty;

import com.alibaba.fastjson2.JSON;
import com.smartclass.smartclass.constant.UserConstant;
import com.smartclass.smartclass.constant.WebSocketMessageType;
import com.smartclass.smartclass.model.dto.websocket.WebSocketMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisCallback;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket认证处理器
 * 支持两种认证方式：
 * 1. URL参数token认证，适用于Postman测试
 * 2. 消息认证，客户端发送type=auth的消息进行认证
 */
@Slf4j
public class WebSocketAuthHandler extends ChannelInboundHandlerAdapter {
    
    public static final AttributeKey<Long> USER_ID_KEY = AttributeKey.valueOf("userId");
    public static final AttributeKey<Boolean> AUTH_KEY = AttributeKey.valueOf("authenticated");
    public static final AttributeKey<String> TOKEN_KEY = AttributeKey.valueOf("token");
    
    private final StringRedisTemplate redisTemplate;
    private final ChannelManager channelManager;
    // 使用 ConcurrentHashMap 保证多线程下的 token 缓存线程安全
    // 注意：当前未做缓存过期清理，后续可改用 Caffeine 替代以支持过期与容量限制
    private final Map<String, Long> tokenUserMap = new ConcurrentHashMap<>();
    private final int authTimeoutSeconds;
    
    public WebSocketAuthHandler(int authTimeoutSeconds, StringRedisTemplate redisTemplate, ChannelManager channelManager) {
        this.authTimeoutSeconds = authTimeoutSeconds;
        this.redisTemplate = redisTemplate;
        this.channelManager = channelManager;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 处理HTTP握手请求，从URL中提取token参数
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            
            // 解析URL参数
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            Map<String, List<String>> parameters = decoder.parameters();
            
            // 检查是否有token参数
            if (parameters.containsKey("token")) {
                String token = parameters.get("token").get(0);
                if (StringUtils.isNotBlank(token)) {
                    log.debug("从URL参数中获取到token: {}", token);
                    ctx.channel().attr(TOKEN_KEY).set(token);
                    
                    // 尝试从token验证用户身份
                    Long userId = getUserIdFromToken(token);
                    if (userId != null) {
                        // 设置认证状态
                        authenticateUser(ctx, userId);
                        log.debug("URL参数token认证成功，用户ID: {}", userId);
                    } else {
                        log.warn("URL参数token认证失败: {}", token);
                        // 继续握手，但标记为未认证，让后续消息认证处理
                    }
                }
            }
            
            // 修改URI，移除查询参数（否则会影响WebSocket协议握手）
            if (request.uri().contains("?")) {
                String newUri = request.uri().split("\\?")[0];
                request.setUri(newUri);
            }
        }
        
        // 处理WebSocket消息，包括认证消息
        else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            String text = frame.text();
            
            // 已认证的连接，直接传递消息
            if (isAuthenticated(ctx)) {
                ctx.fireChannelRead(msg);
                return;
            }
            
            // 处理心跳消息，即使未认证也允许
            if ("PING".equals(text)) {
                ctx.fireChannelRead(msg);
                return;
            }
            
            try {
                // 尝试解析为WebSocketMessage格式
                WebSocketMessage message = JSON.parseObject(text, WebSocketMessage.class);
                
                // 认证消息
                if (WebSocketMessageType.AUTH.equals(message.getType())) {
                    handleAuthMessage(ctx, message);
                    return;
                }

                // 如果未认证，拒绝处理消息
                if (!isAuthenticated(ctx)) {
                    WebSocketMessage authError = new WebSocketMessage();
                    authError.setType(WebSocketMessageType.AUTH_ERROR);
                    authError.setContent("请先进行认证");
                    ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(authError)));
                    return;
                }
            } catch (Exception e) {
                log.error("处理认证消息失败", e);
                // 如果消息解析失败，且未认证，则拒绝处理
                if (!isAuthenticated(ctx)) {
                    WebSocketMessage authError = new WebSocketMessage();
                    authError.setType(WebSocketMessageType.ERROR);
                    authError.setContent("无效的消息格式，请先进行认证");
                    ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(authError)));
                    return;
                }
            }
        }
        
        // 传递给下一个处理器
        ctx.fireChannelRead(msg);
    }
    
    /**
     * 处理认证消息
     */
    private void handleAuthMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        // 仅接受 token 认证；senderId 由服务端从 token 解析，客户端上报值一律不采信
        String token = (String) message.getData();
        if (StringUtils.isBlank(token)) {
            sendAuthError(ctx, "认证失败，缺少token");
            return;
        }
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            sendAuthError(ctx, "无效的token");
            return;
        }
        
        // 认证用户
        authenticateUser(ctx, userId);
        
        // 发送认证成功响应
        WebSocketMessage authResponse = new WebSocketMessage();
        authResponse.setType(WebSocketMessageType.AUTH_SUCCESS);
        authResponse.setContent("认证成功");
        authResponse.setSenderId(userId);
        authResponse.setTimestamp(System.currentTimeMillis());
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(authResponse)));
        
        log.debug("用户认证成功: {}", userId);
    }
    
    /**
     * 从token获取用户ID
     */
    private Long getUserIdFromToken(String token) {
        // 首先从内存缓存中查找
        if (tokenUserMap.containsKey(token)) {
            return tokenUserMap.get(token);
        }
        
        // 从Redis中查找（Spring Session 分布式 session；未开启 spring.session 时此处必然 miss，认证失败关闭）
        String key = "spring:session:sessions:" + token;
        try {
            // Spring Session 的 hash 值是 JDK 序列化字节，必须按字节读回并反序列化；
            // 走 StringRedisTemplate 的字符串视图拿到的是乱码，永远解析不出用户
            byte[] raw = redisTemplate.execute((RedisCallback<byte[]>) conn -> conn.hashCommands().hGet(
                    key.getBytes(StandardCharsets.UTF_8),
                    ("sessionAttr:" + UserConstant.USER_LOGIN_STATE).getBytes(StandardCharsets.UTF_8)));
            Object loginState = deserialize(raw);
            Long userIdValue = extractUserId(loginState);
            if (userIdValue != null) {
                // 缓存到内存
                tokenUserMap.put(token, userIdValue);
                return userIdValue;
            }
        } catch (Exception e) {
            log.debug("从Redis获取用户ID失败", e);
        }

        return null;
    }

    /** JDK 反序列化 Spring Session 属性值；非该格式一律返回 null（fail-closed）。 */
    private Object deserialize(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(raw))) {
            return in.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 session 属性值中提取用户ID。登录态实际存的是 User 对象（可能被序列化为
     * Map/JSON 字符串），仅提取 id 字段且解析失败一律返回 null（fail-closed）。
     */
    @SuppressWarnings("unchecked")
    private Long extractUserId(Object loginState) {
        if (loginState == null) {
            return null;
        }
        // 登录态实际存的是 User 实体（JDK 反序列化后原对象）
        if (loginState instanceof com.smartclass.smartclass.model.entity.User) {
            return ((com.smartclass.smartclass.model.entity.User) loginState).getId();
        }
        if (loginState instanceof Number) {
            return ((Number) loginState).longValue();
        }
        if (loginState instanceof Map) {
            Object id = ((Map<?, ?>) loginState).get("id");
            return id instanceof Number ? ((Number) id).longValue() : null;
        }
        try {
            String raw = loginState.toString();
            if (raw.matches("\\d+")) {
                return Long.parseLong(raw);
            }
            // JSON 序列化的 User 对象：取 id 字段
            com.alibaba.fastjson2.JSONObject user = JSON.parseObject(raw);
            Object id = user == null ? null : user.get("id");
            return id instanceof Number ? ((Number) id).longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 设置用户认证状态
     */
    private void authenticateUser(ChannelHandlerContext ctx, Long userId) {
        // 设置用户ID和认证状态
        ctx.channel().attr(USER_ID_KEY).set(userId);
        ctx.channel().attr(AUTH_KEY).set(true);
        
        // 将通道添加到通道管理器
        channelManager.addChannel(userId, ctx.channel());
    }
    
    /**
     * 发送认证错误消息
     */
    private void sendAuthError(ChannelHandlerContext ctx, String errorMessage) {
        WebSocketMessage authError = new WebSocketMessage();
        authError.setType(WebSocketMessageType.AUTH_ERROR);
        authError.setContent(errorMessage);
        authError.setTimestamp(System.currentTimeMillis());
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(authError)));
    }
    
    /**
     * 检查是否已认证
     */
    private boolean isAuthenticated(ChannelHandlerContext ctx) {
        Boolean authenticated = ctx.channel().attr(AUTH_KEY).get();
        return authenticated != null && authenticated;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("WebSocket连接已建立，等待认证");
        
        // 设置认证超时
        if (authTimeoutSeconds > 0) {
            ctx.executor().schedule(() -> {
                if (!isAuthenticated(ctx)) {
                    // 发送超时消息
                    WebSocketMessage timeoutMsg = new WebSocketMessage();
                    timeoutMsg.setType(WebSocketMessageType.ERROR);
                    timeoutMsg.setContent("认证超时，连接将被关闭");
                    timeoutMsg.setTimestamp(System.currentTimeMillis());
                    ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(timeoutMsg)));
                    
                    // 关闭连接
                    log.debug("认证超时，关闭连接");
                    ctx.close();
                }
            }, authTimeoutSeconds, TimeUnit.SECONDS);
        }
        
        ctx.fireChannelActive();
    }
} 