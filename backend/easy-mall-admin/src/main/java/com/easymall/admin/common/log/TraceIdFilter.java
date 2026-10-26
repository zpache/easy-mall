package com.easymall.admin.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求级 traceId：写入 MDC，日志 pattern 通过 %X{traceId} 输出，
 * 同一次请求产生的所有日志行可用同一个 id 串起来。
 * <p>
 * 上游（网关、前端）若带了 X-Request-Id 则沿用，否则本地生成 16 位；
 * 同时把 traceId 回写到响应头，前端报错时可直接把该 id 给到后端排查。
 * <p>
 * 顺带兼容跨线程场景：异步线程需要手动把 MDC 传过去（MDC.getCopyOfContextMap()）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (!StringUtils.hasText(traceId) || traceId.length() > 64) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 线程池复用，必须清理，否则 traceId 会串到下一个请求
            MDC.remove(TRACE_ID);
        }
    }
}
