package cn.easykits.api.modules.cart.controller;

import cn.easykits.api.common.api.Result;
import cn.easykits.api.modules.cart.dto.CartAddRequest;
import cn.easykits.api.modules.cart.service.CartService;
import cn.easykits.api.modules.cart.vo.CartItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车。
 */
@Tag(name = "购物车")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "购物车列表")
    @GetMapping
    public Result<List<CartItemVO>> list() {
        return Result.ok(cartService.list());
    }

    @Operation(summary = "购物车商品件数")
    @GetMapping("/count")
    public Result<Long> count() {
        return Result.ok(cartService.count());
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CartAddRequest request) {
        cartService.add(request);
        return Result.ok();
    }

    @Operation(summary = "修改数量")
    @PutMapping("/{id}/quantity")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        cartService.updateQuantity(id, quantity);
        return Result.ok();
    }

    @Operation(summary = "勾选/取消勾选（不传 id 表示全选/全不选）")
    @PutMapping("/checked")
    public Result<Void> updateChecked(@RequestParam(required = false) Long id,
                                      @RequestParam Integer checked) {
        cartService.updateChecked(id, checked);
        return Result.ok();
    }

    @Operation(summary = "删除条目")
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(id);
        return Result.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear();
        return Result.ok();
    }
}
