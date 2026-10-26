package com.easymall.admin.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.easymall.admin.common.context.LoginUser;
import com.easymall.admin.common.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：创建/更新时间、创建/修改人、乐观锁版本号。
 * <p>
 * 创建人取当前登录管理员；登录、注册这类还没有登录态的写入留空。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // 版本号必须从 0 开始，交给 @Version 之后自增
        strictInsertFill(metaObject, "version", Integer.class, 0);

        Long userId = currentUserId();
        if (userId != null) {
            strictInsertFill(metaObject, "createdBy", Long.class, userId);
            strictInsertFill(metaObject, "updatedBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 注意：这里不能用 strictUpdateFill —— 实体是从库里查出来的，
        // 字段已有值，strict 模式会直接跳过，导致修改人一直是创建人。
        setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
        Long userId = currentUserId();
        if (userId != null) {
            setFieldValByName("updatedBy", userId, metaObject);
        }
    }

    /**
     * 当前登录管理员 ID；无登录态（如登录接口本身）返回 null。
     */
    private Long currentUserId() {
        LoginUser loginUser = UserContext.get();
        return loginUser == null ? null : loginUser.userId();
    }
}
