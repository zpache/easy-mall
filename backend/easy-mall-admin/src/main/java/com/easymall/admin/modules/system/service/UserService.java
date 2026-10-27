package com.easymall.admin.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.context.UserContext;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysDept;
import com.easymall.admin.entity.SysRole;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.entity.SysUserRole;
import com.easymall.admin.mapper.SysDeptMapper;
import com.easymall.admin.mapper.SysRoleMapper;
import com.easymall.admin.mapper.SysUserMapper;
import com.easymall.admin.mapper.SysUserRoleMapper;
import com.easymall.admin.modules.system.dto.UserForm;
import com.easymall.admin.modules.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台用户管理：分页、增删改、状态切换、角色分配、密码重置。
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {

    /** 超级管理员角色码，禁止删除或停用 */
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;

    public PageResult<UserVO> pageQuery(long pageNum, long pageSize, String keyword, Long deptId, Integer status) {
        List<Long> deptIds = collectDeptIds(deptId);
        Page<SysUser> page = page(Page.of(pageNum, pageSize), Wrappers.<SysUser>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword)
                        .or()
                        .like(SysUser::getPhone, keyword))
                .in(deptId != null && !deptIds.isEmpty(), SysUser::getDeptId, deptIds)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId));

        List<SysUser> records = page.getRecords();
        Map<Long, String> deptNameMap = records.isEmpty() ? Map.of()
                : sysDeptMapper.selectBatchIds(records.stream()
                        .map(SysUser::getDeptId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(SysDept::getId, SysDept::getName));

        Map<Long, List<SysUserRole>> userRoleMap = records.isEmpty() ? Map.of()
                : sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .in(SysUserRole::getUserId, records.stream().map(SysUser::getId).toList()))
                .stream().collect(Collectors.groupingBy(SysUserRole::getUserId));

        Set<Long> roleIds = userRoleMap.values().stream()
                .flatMap(List::stream).map(SysUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, SysRole> roleMap = roleIds.isEmpty() ? Map.of()
                : sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));

        return PageResult.of(page, user -> {
            List<SysUserRole> relations = userRoleMap.getOrDefault(user.getId(), List.of());
            List<Long> ids = relations.stream().map(SysUserRole::getRoleId).toList();
            List<String> names = ids.stream()
                    .map(roleMap::get).filter(Objects::nonNull).map(SysRole::getName).toList();
            return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(),
                    user.getEmail(), user.getDeptId(), deptNameMap.get(user.getDeptId()), user.getStatus(),
                    user.getCreatedAt(), user.getLastLoginAt(), ids, names, user.getVersion());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public SysUser create(UserForm form) {
        if (!StringUtils.hasText(form.password())) {
            throw new BusinessException("新增用户必须设置初始密码");
        }
        long exists = count(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, form.username()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(form.username());
        user.setPassword(passwordEncoder.encode(form.password()));
        user.setNickname(form.nickname());
        user.setPhone(form.phone());
        user.setEmail(form.email());
        user.setDeptId(form.deptId());
        user.setStatus(form.status() == null ? 1 : form.status());
        // 主角色字段兜底：取第一个角色码，保证 Spring Security 单角色场景可用
        user.setRole(resolvePrimaryRoleCode(form.roleIds()));
        save(user);
        replaceUserRoles(user.getId(), form.roleIds());
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysUser update(Long id, UserForm form) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        long exists = count(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, form.username())
                .ne(SysUser::getId, id));
        if (exists > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_EXISTS);
        }
        if (isAdminUser(id) && form.status() != null && form.status() == 0) {
            throw new BusinessException("超级管理员不可停用");
        }
        user.setUsername(form.username());
        user.setNickname(form.nickname());
        user.setPhone(form.phone());
        user.setEmail(form.email());
        user.setDeptId(form.deptId());
        user.setStatus(form.status() == null ? user.getStatus() : form.status());
        user.setRole(resolvePrimaryRoleCode(form.roleIds()));
        if (StringUtils.hasText(form.password())) {
            user.setPassword(passwordEncoder.encode(form.password()));
        }
        if (!updateById(user)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        replaceUserRoles(id, form.roleIds());
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id.equals(UserContext.userId())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (isAdminUser(id)) {
            throw new BusinessException("超级管理员不可删除");
        }
        removeById(id);
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
    }

    public void changeStatus(Long id, Integer status) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (status != null && status == 0 && isAdminUser(id)) {
            throw new BusinessException("超级管理员不可停用");
        }
        user.setStatus(status == null ? 1 : status);
        if (!updateById(user)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void resetPassword(Long id, String rawPassword) {
        if (!StringUtils.hasText(rawPassword) || rawPassword.length() < 6) {
            throw new BusinessException("密码长度不能少于 6 位");
        }
        SysUser user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (!updateById(user)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 用户拥有的角色码（登录鉴权使用，未配置角色时回退 sys_user.role）。
     */
    public List<String> listRoleCodes(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            SysUser user = getById(userId);
            return user == null || !StringUtils.hasText(user.getRole()) ? List.of() : List.of(user.getRole());
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> Integer.valueOf(1).equals(role.getStatus()))
                .map(SysRole::getCode)
                .toList();
    }

    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<Long> distinct = roleIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        for (Long roleId : distinct) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            sysUserRoleMapper.insert(relation);
        }
    }

    private String resolvePrimaryRoleCode(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return "OPERATOR";
        }
        SysRole role = sysRoleMapper.selectById(roleIds.getFirst());
        return role == null ? "OPERATOR" : role.getCode();
    }

    private boolean isAdminUser(Long userId) {
        return listRoleCodes(userId).contains(ADMIN_ROLE_CODE);
    }

    /**
     * 按部门过滤时包含其所有下级部门。
     */
    private List<Long> collectDeptIds(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        List<SysDept> all = sysDeptMapper.selectList(null);
        Map<Long, List<SysDept>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(dept -> dept.getPid() == null ? 0L : dept.getPid()));
        List<Long> result = new ArrayList<>();
        collect(deptId, childrenMap, result);
        return result;
    }

    private void collect(Long deptId, Map<Long, List<SysDept>> childrenMap, List<Long> result) {
        result.add(deptId);
        List<SysDept> children = childrenMap.get(deptId);
        if (children == null) {
            return;
        }
        children.forEach(child -> collect(child.getId(), childrenMap, result));
    }
}
