package com.easymall.admin.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.mapper.SysUserMapper;
import com.easymall.admin.modules.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理后台账号加载。角色取自 sys_user_role + sys_role，未配置时回退 sys_user.role。
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("账号不存在: " + username);
        }
        List<String> roles = userService.listRoleCodes(user.getId());
        return new AdminUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                roles,
                Integer.valueOf(1).equals(user.getStatus()));
    }
}
