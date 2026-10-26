package com.easymall.admin.security;

import com.easymall.admin.common.api.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * 未认证（无 token / token 失效）时返回统一 JSON，而不是 Spring Security 默认的 HTML 页面。
 */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(jakarta.servlet.http.HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("未认证请求: {} {}", request.getMethod(), request.getRequestURI());
        write(response, HttpStatus.UNAUTHORIZED, ResultCode.UNAUTHORIZED);
    }

    static void write(HttpServletResponse response, HttpStatus status, ResultCode resultCode) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        PrintWriter writer = response.getWriter();
        writer.write("{\"code\":" + resultCode.getCode()
                + ",\"message\":\"" + resultCode.getMessage()
                + "\",\"data\":null}");
        writer.flush();
    }
}
