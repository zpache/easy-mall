package com.easymall.admin.modules.goods.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.StockLog;
import com.easymall.admin.modules.goods.dto.StockAdjustForm;
import com.easymall.admin.modules.goods.service.StockService;
import com.easymall.admin.modules.goods.vo.StockLogVO;
import com.easymall.admin.modules.goods.vo.StockVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存管理。
 */
@Tag(name = "商品-库存")
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @Operation(summary = "库存分页列表")
    @GetMapping
    public Result<PageResult<StockVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Boolean lowStock) {
        return Result.ok(stockService.pageQuery(pageNum, pageSize, keyword, lowStock));
    }

    @Operation(summary = "库存调整（入库/出库/盘点）")
    @PostMapping("/adjust")
    public Result<StockLog> adjust(@Valid @RequestBody StockAdjustForm form) {
        return Result.ok(stockService.adjust(form));
    }

    @Operation(summary = "库存流水分页")
    @GetMapping("/logs")
    public Result<PageResult<StockLogVO>> logs(@RequestParam(required = false) Long goodsId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(stockService.pageLogs(goodsId, pageNum, pageSize));
    }
}
