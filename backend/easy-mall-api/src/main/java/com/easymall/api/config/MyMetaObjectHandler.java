package com.easymall.api.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.easymall.api.common.context.LoginUser;
import com.easymall.api.common.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：创建/更新时间、创建/修改人、乐观锁版本号。
 * <p>
 * 创建人取当前登录的小程序用户；微信首次登录自动注册时还没有登录态，留空。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "version", Integer.class, 0);

        Long userId = currentUserId();
        if (userId != null) {
            strictInsertFill(metaObject, "createdBy", Long.class, userId);
            strictInsertFill(metaObject, "updatedBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 不能用 strictUpdateFill：实体从库里查出时字段已有值，strict 模式会跳过
        setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
        Long userId = currentUserId();
        if (userId != null) {
            setFieldValByName("updatedBy", userId, metaObject);
        }
    }

    private Long currentUserId() {
        LoginUser loginUser = UserContext.get();
        return loginUser == null ? null : loginUser.userId();
    }
}
