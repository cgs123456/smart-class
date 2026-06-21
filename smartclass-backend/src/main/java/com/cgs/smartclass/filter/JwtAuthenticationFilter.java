package com.cgs.smartclass.filter;

import com.cgs.smartclass.config.JwtConfig;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.utils.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.cgs.smartclass.constant.UserConstant.USER_LOGIN_STATE;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private JwtConfig jwtConfig;

    @Resource
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 从 Authorization header 提取 JWT token
        String authHeader = request.getHeader(jwtConfig.getHeader());
        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(jwtConfig.getTokenPrefix())) {
            String token = authHeader.substring(jwtConfig.getTokenPrefix().length());
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    // 如果 session 中没有登录用户，将用户信息存入 session
                    Object sessionUser = request.getSession().getAttribute(USER_LOGIN_STATE);
                    if (sessionUser == null) {
                        User user = userService.getById(userId);
                        if (user != null) {
                            request.getSession().setAttribute(USER_LOGIN_STATE, user);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("JWT token 处理失败", e);
            }
        }
        // 无论 JWT 是否有效，都继续执行后续过滤器
        filterChain.doFilter(request, response);
    }
}
