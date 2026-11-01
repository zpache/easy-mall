package com.easymall.admin.common.log;

import com.easymall.admin.common.context.LoginUser;
import com.easymall.admin.common.context.UserContext;
import com.easymall.admin.entity.SysOperLog;
import com.easymall.admin.mapper.SysOperLogMapper;
import com.easymall.admin.modules.log.service.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 操作日志切面：自动采集管理端所有写操作（POST / PUT / DELETE）。
 *
 * <p>模块与操作名直接取控制器上的 Swagger 注解（@Tag name / @Operation summary），
 * 无需业务代码打注标；登录接口（auth 包）已由登录日志单独记录，这里跳过。
 * 日志写入失败只打 warn，绝不影响业务。</p>
 */
@Slf4j
@Aspect
@Service
@RequiredArgsConstructor
public class OperLogAspect {

    /** 参数序列化上限（字符），超长截断 */
    private static final int PARAMS_MAX = 2000;

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @AfterReturning(pointcut = """
            @within(org.springframework.web.bind.annotation.RestController) && (
            @annotation(org.springframework.web.bind.annotation.PostMapping)
            || @annotation(org.springframework.web.bind.annotation.PutMapping)
            || @annotation(org.springframework.web.bind.annotation.DeleteMapping))
            """, returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        record(joinPoint, 0, null);
    }

    @AfterThrowing(pointcut = """
            @within(org.springframework.web.bind.annotation.RestController) && (
            @annotation(org.springframework.web.bind.annotation.PostMapping)
            || @annotation(org.springframework.web.bind.annotation.PutMapping)
            || @annotation(org.springframework.web.bind.annotation.DeleteMapping))
            """, throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Throwable ex) {
        record(joinPoint, 500, ex.getMessage());
    }

    private void record(JoinPoint joinPoint, int resultCode, String errorMsg) {
        try {
            Class<?> controllerClass = joinPoint.getTarget().getClass();
            // 登录/认证已单独记登录日志，避免重复
            if (controllerClass.getPackageName().contains(".auth")) {
                return;
            }
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            SysOperLog logEntry = new SysOperLog();
            LoginUser user = UserContext.get();
            if (user != null) {
                logEntry.setUserId(user.userId());
                logEntry.setUsername(user.username());
            }
            Tag tag = controllerClass.getAnnotation(Tag.class);
            logEntry.setModule(tag == null ? controllerClass.getSimpleName() : tag.name());
            Operation operation = method.getAnnotation(Operation.class);
            logEntry.setAction(operation == null ? method.getName() : operation.summary());

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logEntry.setMethod(truncate(request.getMethod(), 10));
                logEntry.setUri(truncate(request.getRequestURI(), 255));
                logEntry.setIp(ClientIpResolver.clientIp(request));
            }
            logEntry.setParams(truncate(serializeArgs(signature, joinPoint.getArgs()), PARAMS_MAX));
            logEntry.setResultCode(resultCode);
            logEntry.setErrorMsg(truncate(errorMsg, 500));
            logEntry.setCreatedAt(LocalDateTime.now());
            operLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("操作日志记录失败", e);
        }
    }

    /** 参数按方法形参名序列化：文件参数只记文件名与大小，请求对象跳过 */
    private String serializeArgs(MethodSignature signature, Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        String[] names = signature.getParameterNames();
        Map<String, Object> named = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof HttpServletRequest || arg instanceof MultipartFile) {
                if (arg instanceof MultipartFile file) {
                    named.put(paramName(names, i, "arg" + i),
                            "file:" + file.getOriginalFilename() + "(" + file.getSize() + "B)");
                }
                continue;
            }
            named.put(paramName(names, i, "arg" + i), arg);
        }
        if (named.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(named);
        } catch (Exception e) {
            return "<unserializable>";
        }
    }

    private String paramName(String[] names, int index, String fallback) {
        return names != null && names.length > index && StringUtils.hasText(names[index])
                ? names[index] : fallback;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
