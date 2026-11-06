package com.easymall.admin.modules.goods.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.entity.StockLog;
import com.easymall.admin.modules.goods.dto.StockAdjustForm;
import com.easymall.admin.modules.goods.service.StockExcelService;
import com.easymall.admin.modules.goods.service.StockService;
import com.easymall.admin.modules.goods.vo.BatchStockVO;
import com.easymall.admin.modules.goods.vo.StockLogVO;
import com.easymall.admin.modules.goods.vo.StockVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 库存管理。
 */
@Tag(name = "商品-库存")
@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockExcelService stockExcelService;

    @Operation(summary = "库存分页列表")
    @GetMapping
    @PreAuthorize("hasAuthority('goods:stock:list')")
    public Result<PageResult<StockVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Boolean lowStock) {
        return Result.ok(stockService.pageQuery(pageNum, pageSize, keyword, lowStock));
    }

    @Operation(summary = "库存调整（入库/出库/盘点）")
    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('goods:stock:adjust')")
    public Result<StockLog> adjust(@Valid @RequestBody StockAdjustForm form) {
        return Result.ok(stockService.adjust(form));
    }

    @Operation(summary = "库存流水分页")
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('goods:stock:logs')")
    public Result<PageResult<StockLogVO>> logs(@RequestParam(required = false) Long goodsId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(stockService.pageLogs(goodsId, pageNum, pageSize));
    }

    @Operation(summary = "批量设置库存模板下载（xlsx）")
    @GetMapping("/batch-template")
    @PreAuthorize("hasAuthority('goods:stock:import')")
    public ResponseEntity<byte[]> batchTemplate() {
        byte[] bytes = stockExcelService.stockTemplate();
        String filename = URLEncoder.encode("批量设置库存模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @Operation(summary = "批量设置库存（上传按模板填写的 xlsx）")
    @PostMapping("/batch-set")
    @PreAuthorize("hasAuthority('goods:stock:import')")
    public Result<BatchStockVO> batchSet(@RequestParam("file") MultipartFile file) {
        return Result.ok(stockExcelService.parseAndBatchSet(file));
    }
}
