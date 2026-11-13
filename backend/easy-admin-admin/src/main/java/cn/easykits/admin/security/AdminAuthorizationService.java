package cn.easykits.admin.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.admin.common.context.LoginUser;
import cn.easykits.admin.entity.SysRole;
import cn.easykits.admin.entity.SysUser;
import cn.easykits.admin.entity.SysUserRole;
import cn.easykits.admin.mapper.SysRoleMapper;
import cn.easykits.admin.mapper.SysUserMapper;
import cn.easykits.admin.mapper.SysUserRoleMapper;
import cn.easykits.admin.modules.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 请求级授权信息加载：校验用户是否仍启用，并以数据库中的最新角色和菜单授权构造 authority。
 * 不缓存，保证停用账号、调整角色或回收权限后旧 token 立即失效。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final MenuService menuService;

    /**
     * 用 token 中的用户 ID 回库刷新用户与角色；用户被删除或停用时返回 empty。
     */
    public Optional<LoginUser> resolveActiveUser(LoginUser tokenUser) {
        SysUser user = sysUserMapper.selectById(tokenUser.userId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            return Optional.empty();
        }
        List<String> roles = listActiveRoleCodes(user);
        return Optional.of(new LoginUser(user.getId(), user.getUsername(), user.getNickname(), roles));
    }

    /**
     * 角色码 + 菜单/按钮权限码；ADMIN 的菜单码已在 MenuService 中兜底为全部内置权限。
     */
    public List<GrantedAuthority> loadAuthorities(LoginUser loginUser) {
        Set<String> values = new LinkedHashSet<>();
        loginUser.roles().forEach(role -> values.add(ROLE_PREFIX + role));
        values.addAll(menuService.listAuthCodesByUser(loginUser.userId()));
        List<GrantedAuthority> authorities = new ArrayList<>(values.size());
        values.forEach(value -> authorities.add(new SimpleGrantedAuthority(value)));
        return List.copyOf(authorities);
    }

    private List<String> listActiveRoleCodes(SysUser user) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        if (!roleIds.isEmpty()) {
            return sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                            .in(SysRole::getId, roleIds)
                            .eq(SysRole::getStatus, 1)
                            .orderByAsc(SysRole::getSort))
                    .stream().map(SysRole::getCode).toList();
        }
        if (!StringUtils.hasText(user.getRole())) {
            return List.of();
        }
        SysRole legacyRole = sysRoleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, user.getRole())
                .eq(SysRole::getStatus, 1)
                .last("limit 1"));
        return legacyRole == null ? List.of() : List.of(legacyRole.getCode());
    }
}
