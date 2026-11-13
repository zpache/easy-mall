package cn.easykits.admin.modules.system.controller;

import cn.easykits.admin.common.api.Result;
import cn.easykits.admin.common.context.UserContext;
import cn.easykits.admin.entity.SysMenu;
import cn.easykits.admin.modules.system.dto.MenuForm;
import cn.easykits.admin.modules.system.service.MenuService;
import cn.easykits.admin.modules.system.vo.MenuRouteVO;
import cn.easykits.admin.modules.system.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统管理 - 菜单 / 权限管理。
 */
@Tag(name = "系统-菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "菜单树")
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('system:menu:list', 'system:menu:create', "
            + "'system:menu:update', 'system:role:create', 'system:role:update')")
    public Result<List<MenuVO>> list() {
        return Result.ok(menuService.listTree());
    }

    @Operation(summary = "当前用户可见菜单树")
    @GetMapping("/list-by-user")
    public Result<List<MenuVO>> listByUser() {
        return Result.ok(menuService.listTreeByUser(UserContext.userId()));
    }

    @Operation(summary = "当前用户的动态路由树（前端登录后生成菜单用）")
    @GetMapping("/routes")
    public Result<List<MenuRouteVO>> routes() {
        return Result.ok(menuService.listRoutesByUser(UserContext.userId()));
    }

    @Operation(summary = "校验路由名称是否存在")
    @GetMapping("/name-exists")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<Boolean> nameExists(@RequestParam String name,
                                      @RequestParam(required = false) Long id) {
        return Result.ok(menuService.nameExists(name, id));
    }

    @Operation(summary = "校验路由地址是否存在")
    @GetMapping("/path-exists")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<Boolean> pathExists(@RequestParam String path,
                                      @RequestParam(required = false) Long id) {
        return Result.ok(menuService.pathExists(path, id));
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:create')")
    public Result<SysMenu> create(@Valid @RequestBody MenuForm form) {
        return Result.ok(menuService.create(form));
    }

    @Operation(summary = "编辑菜单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:update')")
    public Result<SysMenu> update(@PathVariable Long id, @Valid @RequestBody MenuForm form) {
        return Result.ok(menuService.update(id, form));
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
