package com.easymall.admin.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysMenu;
import com.easymall.admin.entity.SysRole;
import com.easymall.admin.entity.SysRoleMenu;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.entity.SysUserRole;
import com.easymall.admin.mapper.SysMenuMapper;
import com.easymall.admin.mapper.SysRoleMapper;
import com.easymall.admin.mapper.SysRoleMenuMapper;
import com.easymall.admin.mapper.SysUserMapper;
import com.easymall.admin.mapper.SysUserRoleMapper;
import com.easymall.admin.modules.system.dto.MenuForm;
import com.easymall.admin.modules.system.vo.MenuMeta;
import com.easymall.admin.modules.system.vo.MenuRouteVO;
import com.easymall.admin.modules.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    /** 外链 / 内嵌菜单统一使用 vben 的 IFrameView 组件渲染 */
    private static final String IFRAME_VIEW = "IFrameView";

    private static final String TYPE_BUTTON = "button";
    private static final String TYPE_CATALOG = "catalog";
    private static final String TYPE_LINK = "link";
    private static final String TYPE_EMBEDDED = "embedded";
    private static final String ROLE_ADMIN = "ADMIN";

    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;

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
        Set<Long> visibleIds = resolveVisibleMenuIds(userId);
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        List<SysMenu> all = listEnabled().stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .toList();
        return buildTree(all, 0L);
    }

    /**
     * 当前用户的权限码集合（菜单、按钮上的 auth_code）。
     */
    public List<String> listAuthCodesByUser(Long userId) {
        Set<Long> visibleIds = resolveVisibleMenuIds(userId);
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        return listEnabled().stream()
                .filter(menu -> visibleIds.contains(menu.getId()))
                .map(SysMenu::getAuthCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /**
     * 当前用户的路由树，字段与 vue-router 的路由记录一一对应，前端直接交给
     * vben 的 generateAccessible 生成路由和菜单（accessMode = backend）。
     * <p>
     * 与 {@link #listTreeByUser(Long)} 的区别：这里过滤掉按钮，并把外链 / 内嵌
     * 菜单的 component 统一映射为 IFrameView。
     */
    public List<MenuRouteVO> listRoutesByUser(Long userId) {
        Set<Long> visibleIds = resolveVisibleMenuIds(userId);
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysMenu> byId = listEnabled().stream()
                .collect(Collectors.toMap(SysMenu::getId, menu -> menu, (a, b) -> a, LinkedHashMap::new));
        return buildRouteChildren(0L, byId, visibleIds);
    }

    /**
     * 用户的默认首页：路由树中第一个可访问的页面路径，找不到时返回 null 由前端兜底。
     */
    public String resolveHomePath(Long userId) {
        return firstPagePath(listRoutesByUser(userId));
    }

    /**
     * 当前用户可见的菜单 ID 集合：角色已授权的菜单 + 其祖先链（保证子菜单不会因为
     * 父级未授权而在建树时丢失）。超级管理员（ADMIN）直接获得全部启用菜单。
     */
    public Set<Long> resolveVisibleMenuIds(Long userId) {
        List<SysMenu> all = listEnabled();
        if (all.isEmpty()) {
            return Set.of();
        }
        List<Long> roleIds = resolveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        if (hasAdminRole(roleIds)) {
            return all.stream().map(SysMenu::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        Set<Long> granted = sysRoleMenuMapper.selectList(
                        Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
        if (granted.isEmpty()) {
            return Set.of();
        }
        Map<Long, SysMenu> byId = all.stream()
                .collect(Collectors.toMap(SysMenu::getId, menu -> menu, (a, b) -> a));
        Set<Long> visible = new LinkedHashSet<>();
        for (Long menuId : granted) {
            SysMenu current = byId.get(menuId);
            // 沿 pid 向上补齐祖先链，父级停用时中断（父级不可见则子级也不可见）
            while (current != null && visible.add(current.getId())) {
                current = byId.get(current.getPid());
            }
        }
        return visible;
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

    /**
     * 全部启用菜单，按排序值、ID 升序，保证建树顺序稳定。
     */
    private List<SysMenu> listEnabled() {
        return list(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getOrderNo)
                .orderByAsc(SysMenu::getId));
    }

    /**
     * 用户的角色 ID：优先取 sys_user_role，账号未配置关联时回退到 sys_user.role 角色码，
     * 兼容早期只有单角色字段的数据。
     */
    private List<Long> resolveRoleIds(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        if (!roleIds.isEmpty()) {
            return roleIds;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getRole())) {
            return List.of();
        }
        SysRole role = sysRoleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, user.getRole()).last("limit 1"));
        return role == null ? List.of() : List.of(role.getId());
    }

    private boolean hasAdminRole(List<Long> roleIds) {
        return sysRoleMapper.selectList(
                        Wrappers.<SysRole>lambdaQuery().in(SysRole::getId, roleIds))
                .stream()
                .anyMatch(role -> ROLE_ADMIN.equalsIgnoreCase(role.getCode()));
    }

    private List<MenuRouteVO> buildRouteChildren(Long pid, Map<Long, SysMenu> byId, Set<Long> visibleIds) {
        List<MenuRouteVO> result = new ArrayList<>();
        for (SysMenu menu : byId.values()) {
            if (!Objects.equals(menu.getPid() == null ? 0L : menu.getPid(), pid)) {
                continue;
            }
            if (!visibleIds.contains(menu.getId()) || TYPE_BUTTON.equals(menu.getType())) {
                continue;
            }
            List<MenuRouteVO> children = buildRouteChildren(menu.getId(), byId, visibleIds);
            // 目录下没有任何可访问页面时不渲染，避免菜单树出现空节点
            if (TYPE_CATALOG.equals(menu.getType()) && children.isEmpty()) {
                continue;
            }
            result.add(new MenuRouteVO(
                    menu.getName(),
                    menu.getPath(),
                    resolveComponent(menu),
                    null,
                    toMeta(menu),
                    children));
        }
        return result;
    }

    /**
     * 外链 / 内嵌菜单统一交给 IFrameView 渲染，其余按库中配置的组件路径解析页面。
     */
    private String resolveComponent(SysMenu menu) {
        if (TYPE_LINK.equals(menu.getType()) || TYPE_EMBEDDED.equals(menu.getType())) {
            return IFRAME_VIEW;
        }
        return menu.getComponent();
    }

    private String firstPagePath(List<MenuRouteVO> routes) {
        for (MenuRouteVO route : routes) {
            if (StringUtils.hasText(route.component())
                    && StringUtils.hasText(route.path())
                    && !IFRAME_VIEW.equals(route.component())) {
                return route.path();
            }
            String child = firstPagePath(route.children());
            if (child != null) {
                return child;
            }
        }
        return null;
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
                    toMeta(menu),
                    buildChildren(childrenMap, menu.getId())));
        }
        return result;
    }

    private MenuMeta toMeta(SysMenu menu) {
        return new MenuMeta(
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
                menu.getIframeSrc());
    }
}
