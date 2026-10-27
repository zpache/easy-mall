package com.easymall.api.modules.address.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.entity.Address;
import com.easymall.api.modules.address.dto.AddressForm;
import com.easymall.api.modules.address.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址。
 */
@Tag(name = "收货地址")
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public Result<List<Address>> list() {
        return Result.ok(addressService.list());
    }

    @Operation(summary = "地址详情")
    @GetMapping("/{id}")
    public Result<Address> detail(@PathVariable Long id) {
        return Result.ok(addressService.detail(id));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Address> create(@Valid @RequestBody AddressForm form) {
        return Result.ok(addressService.create(form));
    }

    @Operation(summary = "编辑地址")
    @PutMapping("/{id}")
    public Result<Address> update(@PathVariable Long id, @Valid @RequestBody AddressForm form) {
        return Result.ok(addressService.update(id, form));
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.ok();
    }
}
