package com.easymall.admin.modules.setting.controller;

import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.setting.dto.DeliveryCompanyForm;
import com.easymall.admin.modules.setting.service.DeliveryCompanyService;
import com.easymall.admin.modules.setting.vo.DeliveryCompanyVO;
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
 * 商城设置 - 快递公司。发货弹窗的下拉与物流轨迹查询的编码解析都读这里。
 */
@Tag(name = "商城设置-快递公司")
@RestController
@RequestMapping("/delivery-companies")
@RequiredArgsConstructor
public class DeliveryCompanyController {

    private final DeliveryCompanyService deliveryCompanyService;

    @Operation(summary = "快递公司列表（status=1 只取启用的，发货下拉用）")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('settings:delivery:list', 'order:order:ship')")
    public Result<List<DeliveryCompanyVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(deliveryCompanyService.list(status));
    }

    @Operation(summary = "新增快递公司")
    @PostMapping
    @PreAuthorize("hasAuthority('settings:delivery:create')")
    public Result<Long> create(@Valid @RequestBody DeliveryCompanyForm form) {
        return Result.ok(deliveryCompanyService.create(form));
    }

    @Operation(summary = "编辑快递公司")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings:delivery:update')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeliveryCompanyForm form) {
        deliveryCompanyService.update(id, form);
        return Result.ok();
    }

    @Operation(summary = "删除快递公司")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings:delivery:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        deliveryCompanyService.delete(id);
        return Result.ok();
    }
}
