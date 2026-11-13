package cn.easykits.admin.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.easykits.admin.entity.SysLoginLog;
import cn.easykits.admin.mapper.SysLoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 管理端登录日志：记录 + 分页查询。
 */
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    /**
     * 记录一次登录结果。任何异常静默吞掉，绝不影响登录主流程。
     */
    public void record(String username, Long userId, boolean success, String message) {
        try {
            SysLoginLog log = new SysLoginLog();
            log.setUsername(username);
            log.setUserId(userId);
            log.setSuccess(success ? 1 : 0);
            log.setMessage(message);
            fillRequest(log);
            log.setCreatedAt(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志失败不影响登录
        }
    }

    /**
     * 分页查询：可按账号、成功与否过滤。
     */
    public Page<SysLoginLog> pageQuery(long pageNum, long pageSize, String username, Boolean success) {
        LambdaQueryWrapper<SysLoginLog> wrapper = Wrappers.<SysLoginLog>lambdaQuery()
                .like(StringUtils.hasText(username), SysLoginLog::getUsername, username)
                .eq(success != null, SysLoginLog::getSuccess, success != null && success ? 1 : 0)
                .orderByDesc(SysLoginLog::getId);
        return loginLogMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
    }

    private void fillRequest(SysLoginLog log) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            log.setIp(ClientIpResolver.clientIp(request));
            log.setUserAgent(truncate(request.getHeader("User-Agent"), 255));
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
