package cn.easykits.admin.modules.log.service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析：优先取反向代理头（X-Forwarded-For / X-Real-IP），取不到回退 remoteAddr。
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // 多级代理时第一个是客户端真实 IP
            int comma = ip.indexOf(',');
            return (comma > 0 ? ip.substring(0, comma) : ip).trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
