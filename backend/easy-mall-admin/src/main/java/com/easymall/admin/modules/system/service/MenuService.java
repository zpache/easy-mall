package com.easymall.admin.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysMenu;
import com.easymall.admin.entity.SysRoleMenu;
import com.easymall.admin.entity.SysUserRole;
import com.easymall.admin.mapper.SysMenuMapper;
import com.easymall.admin.mapper.SysRoleMenuMapper;
import com.easymall.admin.mapper.SysUserRoleMapper;
import com.easymall.admin.modules.system.dto.MenuForm;
import com.easymall.admin.modules.system.vo.MenuMeta;
import com.easymall.admin.modules.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单 / 权限管理。树的每个节点对应 vben 的一条路由 + meta。
 */
@Service
@RequiredArgsConstructor
public class MenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 全量菜单树（菜单管理页使用）。
     */
    public List<MenuVO> listTree() {
        List<SysMenu> all = list(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getPid)
                .orderByAsc(SysMenu::getOrderNo)
                .orderByAsc(SysMenu::getId));
        return buildTree(all, 0L);
    }

    /**
     * 当前用户拥有的菜单树（按角色 → 菜单关联过滤，只保留启用的节点）。
     */
    public List<MenuVO> listTreeByUser(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = sysRoleMenuMapper.selectList(
                        Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        List<SysMenu> all = list(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getOrderNo));
        return buildTree(all, 0L);
    }

    /**
     * 当前用户的权限码集合（菜单、按钮上的 auth_code）。
     */
    public List<String> listAuthCodesByUser(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = sysRoleMenuMapper.selectList(
                        Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return list(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getId, menuIds)
                .eq(SysMenu::getStatus, 1))
                .stream()
                .map(SysMenu::getAuthCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    public SysMenu create(MenuForm form) {
        validateUnique(form.name(), form.path(), null);
        SysMenu menu = new SysMenu();
        applyForm(menu, form);
        save(menu);
        return menu;
    }

    public SysMenu update(Long id, MenuForm form) {
        SysMenu menu = getById(id);
        if (menu == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }
        if (Objects.equals(id, form.pid())) {
            throw new BusinessException("上级菜单不能是自己");
        }
        validateUnique(form.name(), form.path(), id);
        applyForm(menu, form);
        if (!updateById(menu)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return menu;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysMenu menu = getById(id);
        if (menu == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }
        long children = count(Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getPid, id));
        if (children > 0) {
            throw new BusinessException("请先删除下级菜单");
        }
        removeById(id);
        // 同步清理角色授权关系
        sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getMenuId, id));
    }

    public boolean nameExists(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return count(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getName, name)
                .ne(excludeId != null, SysMenu::getId, excludeId)) > 0;
    }

    public boolean pathExists(String path, Long excludeId) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return count(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getPath, path)
                .ne(excludeId != null, SysMenu::getId, excludeId)) > 0;
    }

    /**
     * 按角色查询已授权菜单 ID（授权弹窗回显用）。
     */
    public List<Long> listMenuIdsByRole(Long roleId) {
        return sysRoleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    /**
     * 覆盖式同步角色授权：先清空再写入。
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        Set<Long> distinct = menuIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long menuId : distinct) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            sysRoleMenuMapper.insert(relation);
        }
    }

    private void applyForm(SysMenu menu, MenuForm form) {
        menu.setPid(form.pid() == null ? 0L : form.pid());
        menu.setName(form.name());
        menu.setType(form.type());
        menu.setAuthCode(form.authCode());
        menu.setPath(form.path());
        menu.setComponent(form.component());
        menu.setStatus(form.status() == null ? 1 : form.status());

        MenuForm.MenuMetaForm meta = form.meta();
        if (meta == null) {
            menu.setTitle(form.name());
            menu.setOrderNo(0);
            return;
        }
        menu.setTitle(StringUtils.hasText(meta.title()) ? meta.title() : form.name());
        menu.setIcon(meta.icon());
        menu.setOrderNo(meta.order() == null ? 0 : meta.order());
        menu.setBadge(meta.badge());
        menu.setBadgeType(meta.badgeType());
        menu.setBadgeVariants(meta.badgeVariants());
        menu.setKeepAlive(toFlag(meta.keepAlive()));
        menu.setAffixTab(toFlag(meta.affixTab()));
        menu.setHideInMenu(toFlag(meta.hideInMenu()));
        menu.setHideChildrenInMenu(toFlag(meta.hideChildrenInMenu()));
        menu.setHideInBreadcrumb(toFlag(meta.hideInBreadcrumb()));
        menu.setHideInTab(toFlag(meta.hideInTab()));
        menu.setLink(meta.link());
        menu.setIframeSrc(meta.iframeSrc());
    }

    private void validateUnique(String name, String path, Long excludeId) {
        if (nameExists(name, excludeId)) {
            throw new BusinessException("菜单名称已存在");
        }
        if (pathExists(path, excludeId)) {
            throw new BusinessException("路由地址已存在");
        }
    }

    private int toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private boolean toBoolean(Integer value) {
        return value != null && value == 1;
    }

    private List<MenuVO> buildTree(List<SysMenu> all, Long pid) {
        Map<Long, List<SysMenu>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(menu -> menu.getPid() == null ? 0L : menu.getPid()));
        return buildChildren(childrenMap, pid);
    }

    private List<MenuVO> buildChildren(Map<Long, List<SysMenu>> childrenMap, Long pid) {
        List<SysMenu> children = childrenMap.get(pid);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<MenuVO> result = new ArrayList<>(children.size());
        for (SysMenu menu : children) {
            result.add(new MenuVO(
                    menu.getId(),
                    menu.getPid(),
                    menu.getName(),
                    menu.getType(),
                    menu.getAuthCode(),
                    menu.getPath(),
                    menu.getComponent(),
                    menu.getStatus(),
                    new MenuMeta(
                            menu.getTitle(),
                            menu.getIcon(),
                            menu.getOrderNo(),
                            menu.getBadge(),
                            menu.getBadgeType(),
                            menu.getBadgeVariants(),
                            toBoolean(menu.getKeepAlive()),
                            toBoolean(menu.getAffixTab()),
                            toBoolean(menu.getHideInMenu()),
                            toBoolean(menu.getHideChildrenInMenu()),
                            toBoolean(menu.getHideInBreadcrumb()),
                            toBoolean(menu.getHideInTab()),
                            menu.getLink(),
                            menu.getIframeSrc()),
                    buildChildren(childrenMap, menu.getId())));
        }
        return result;
    }
}
