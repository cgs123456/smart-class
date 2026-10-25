package com.smartclass.smartclass.filter;

import com.smartclass.smartclass.config.JwtConfig;
import com.smartclass.smartclass.constant.UserConstant;
import com.smartclass.smartclass.model.entity.User;
import com.smartclass.smartclass.service.UserService;
import com.smartclass.smartclass.utils.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>从 Authorization Header 解析 JWT，校验有效性</li>
 *   <li>JWT 有效时把用户同步到 Session（兼容旧业务逻辑）</li>
 *   <li>把用户身份与角色写入 Spring SecurityContext，使 SecurityConfig 的 authenticated() 生效</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private JwtConfig jwtConfig;

    @Resource
    private UserService userService;

    /**
     * Spring Security 6 默认 requireExplicitSave=true：过滤器里 setAuthentication 不会落库，
     * SessionManagementFilter 会把每次请求都当作「本次新认证」而触发会话固定防护
     * （ChangeSessionIdAuthenticationStrategy → changeSessionId → Redis 会话 key 被逐请求 RENAME），
     * 并行请求互踩旧 id。显式经 repository 持久化 SPRING_SECURITY_CONTEXT 后，
     * 后续请求 containsContext=true，策略不再触发，会话 id 全程稳定。
     */
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. 优先尝试 Session 中的登录态（兼容已有 Session 登录流程）
        User sessionUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);

        // 2. 如果 Session 中没有登录态，尝试从 Authorization header 解析 JWT
        if (sessionUser == null) {
            String authHeader = request.getHeader(jwtConfig.getHeader());
            if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(jwtConfig.getTokenPrefix())) {
                String token = authHeader.substring(jwtConfig.getTokenPrefix().length());
                try {
                    if (jwtUtil.validateToken(token)) {
                        Long userId = jwtUtil.getUserIdFromToken(token);
                        User user = userService.getById(userId);
                        if (user != null) {
                            // 同步到 Session，保持与原业务逻辑一致（不存储密码哈希）
                            user.setUserPassword(null);
                            request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
                            sessionUser = user;
                        }
                    }
                } catch (Exception e) {
                    log.warn("JWT token 处理失败: {}", e.getMessage());
                }
            }
        }

        // 3. 把登录用户写入 Spring SecurityContext，让 SecurityConfig 的 authenticated() 能识别
        if (sessionUser != null && sessionUser.getId() != null) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            String role = sessionUser.getUserRole();
            if (StringUtils.isNotBlank(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(sessionUser, null, authorities);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // 显式持久化，杜绝 SessionManagementFilter 的逐请求会话 id 轮换（见字段注释）
            this.securityContextRepository.saveContext(context, request, response);
        }

        // 4. 继续过滤器链，未登录请求由 SecurityConfig 决定是否放行
        filterChain.doFilter(request, response);
    }
}
