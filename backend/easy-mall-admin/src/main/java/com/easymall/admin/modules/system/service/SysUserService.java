package com.easymall.admin.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.context.UserContext;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.modules.system.dto.SysUserForm;
import com.easymall.admin.modules.system.entity.SysUser;
import com.easymall.admin.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理后台账号服务。
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final PasswordEncoder passwordEncoder;

    public PageResult<SysUser> pageQuery(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.<SysUser>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword))
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId);
        Page<SysUser> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    public SysUser create(SysUserForm form) {
        if (!StringUtils.hasText(form.password())) {
            throw new BusinessException("新增账号必须设置密码");
        }
        long exists = count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, form.username()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(form.username());
        user.setPassword(passwordEncoder.encode(form.password()));
        user.setNickname(form.nickname());
        user.setAvatar(form.avatar());
        user.setPhone(form.phone());
        user.setRole(form.role());
        user.setStatus(form.status());
        save(user);
        return user;
    }

    public SysUser update(Long id, SysUserForm form) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        long exists = count(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, form.username())
                .ne(SysUser::getId, id));
        if (exists > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS);
        }
        user.setUsername(form.username());
        user.setNickname(form.nickname());
        user.setAvatar(form.avatar());
        user.setPhone(form.phone());
        user.setRole(form.role());
        user.setStatus(form.status());
        if (StringUtils.hasText(form.password())) {
            user.setPassword(passwordEncoder.encode(form.password()));
        }
        if (form.version() != null) {
            user.setVersion(form.version());
        }
        if (!updateById(user)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return user;
    }

    public void delete(Long id) {
        if (id.equals(UserContext.userId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    public void resetPassword(Long id, String rawPassword) {
        if (!StringUtils.hasText(rawPassword) || rawPassword.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (!updateById(user)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }
}
