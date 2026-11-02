package com.easymall.admin.modules.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.context.UserContext;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.entity.StockLog;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.mapper.StockLogMapper;
import com.easymall.admin.modules.goods.dto.StockAdjustForm;
import com.easymall.admin.modules.goods.vo.BatchStockVO;
import com.easymall.admin.modules.goods.vo.GoodsVO;
import com.easymall.admin.modules.goods.vo.StockLogVO;
import com.easymall.admin.modules.goods.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存服务：库存查询、出入库 / 盘点调整、流水查询。
 */
@Service
@RequiredArgsConstructor
public class StockService {

    /** 入库 */
    private static final String TYPE_IN = "in";
    /** 出库 */
    private static final String TYPE_OUT = "out";
    /** 盘点（qty 为目标库存） */
    private static final String TYPE_CHECK = "check";
    private static final Set<String> TYPES = Set.of(TYPE_IN, TYPE_OUT, TYPE_CHECK);

    private final GoodsService goodsService;
    private final GoodsMapper goodsMapper;
    private final StockLogMapper stockLogMapper;

    /**
     * 库存分页：可按商品名搜索、只看低于预警值的商品。
     */
    public PageResult<StockVO> pageQuery(long pageNum, long pageSize, String keyword, Boolean lowStock) {
        LambdaQueryWrapper<Goods> wrapper = Wrappers.<Goods>lambdaQuery()
                // 关键词同时匹配商品名称与商品编码
                .and(StringUtils.hasText(keyword),
                        w -> w.like(Goods::getName, keyword).or().like(Goods::getGoodsCode, keyword))
                .orderByAsc(Goods::getStock)
                .orderByDesc(Goods::getId);
        if (Boolean.TRUE.equals(lowStock)) {
            wrapper.apply("stock <= stock_warn");
        }
        Page<Goods> page = goodsService.page(Page.of(pageNum, pageSize), wrapper);
        List<GoodsVO> voList = goodsService.toVOList(page.getRecords());
        List<StockVO> records = voList.stream()
                .map(vo -> new StockVO(
                        vo.id(),
                        vo.goodsCode(),
                        vo.name(),
                        vo.mainImage(),
                        vo.unit(),
                        vo.categoryName(),
                        vo.brandName(),
                        vo.stock(),
                        vo.stockWarn(),
                        vo.status(),
                        isLowStock(vo.stock(), vo.stockWarn()),
                        vo.updatedAt(),
                        vo.version()))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 库存调整。入库/出库按数量增减，盘点直接改成目标库存；每次调整写一条流水。
     */
    @Transactional(rollbackFor = Exception.class)
    public StockLog adjust(StockAdjustForm form) {
        Goods goods = goodsService.getById(form.goodsId());
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "商品不存在");
        }
        String type = form.type();
        if (!TYPES.contains(type)) {
            throw new BusinessException("调整类型只能为 in（入库）、out（出库）或 check（盘点）");
        }
        int current = goods.getStock() == null ? 0 : goods.getStock();
        int qty = form.qty() == null ? 0 : form.qty();
        int delta;
        switch (type) {
            case TYPE_IN -> {
                if (qty <= 0) {
                    throw new BusinessException("入库数量必须大于 0");
                }
                delta = qty;
            }
            case TYPE_OUT -> {
                if (qty <= 0) {
                    throw new BusinessException("出库数量必须大于 0");
                }
                delta = -qty;
            }
            default -> delta = qty - current;
        }
        if (delta == 0) {
            throw new BusinessException("库存没有变化，无需调整");
        }
        // 单条 SQL 内完成判断与更新，避免并发覆盖
        if (goodsMapper.adjustStock(form.goodsId(), delta) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT,
                    "库存不足，当前库存 " + current + "，无法出库 " + qty);
        }
        Goods latest = goodsService.getById(form.goodsId());
        int afterStock = latest == null ? current + delta : latest.getStock();

        StockLog log = new StockLog();
        log.setGoodsId(form.goodsId());
        log.setType(type);
        log.setChangeQty(delta);
        log.setBeforeStock(afterStock - delta);
        log.setAfterStock(afterStock);
        log.setRemark(form.remark());
        log.setCreatedBy(UserContext.get() == null ? null : UserContext.userId());
        stockLogMapper.insert(log);
        return log;
    }

    /**
     * 库存流水分页，可按商品过滤。
     */
    public PageResult<StockLogVO> pageLogs(Long goodsId, long pageNum, long pageSize) {
        LambdaQueryWrapper<StockLog> wrapper = Wrappers.<StockLog>lambdaQuery()
                .eq(goodsId != null, StockLog::getGoodsId, goodsId)
                .orderByDesc(StockLog::getId);
        Page<StockLog> page = stockLogMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<Long> goodsIds = page.getRecords().stream().map(StockLog::getGoodsId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nameMap = goodsIds.isEmpty()
                ? Map.of()
                : goodsService.listByIds(goodsIds).stream()
                .collect(Collectors.toMap(Goods::getId, Goods::getName, (a, b) -> a, () -> new java.util.LinkedHashMap<>()));
        List<StockLogVO> records = page.getRecords().stream()
                .map(log -> new StockLogVO(
                        log.getId(),
                        log.getGoodsId(),
                        nameMap.get(log.getGoodsId()),
                        log.getType(),
                        log.getChangeQty(),
                        log.getBeforeStock(),
                        log.getAfterStock(),
                        log.getRemark(),
                        log.getCreatedAt(),
                        log.getCreatedBy()))
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private Boolean isLowStock(Integer stock, Integer stockWarn) {
        int s = stock == null ? 0 : stock;
        int warn = stockWarn == null ? 0 : stockWarn;
        return s <= warn;
    }

    /* ---------------- 批量设置库存 ---------------- */

    /** 批量设置时写入流水的备注 */
    private static final String BATCH_REMARK = "批量设置库存";

    /**
     * 批量设置库存文件中的一行：商品编码 + 目标库存（Excel 读出的原始字符串）。
     */
    public record BatchStockRow(String goodsCode, String stock) {
    }

    /**
     * 批量设置库存（盘点语义）：按商品编码逐条把库存改为目标值。
     *
     * <p>编码大小写不敏感；文件内编码去重（后行忽略）；目标库存与当前库存一致视为成功但不写流水；
     * 单条失败不影响其余行，失败原因逐条返回。库存增减复用 {@link #adjust} 相同的
     * 单条原子 SQL 守卫（stock + delta &gt;= 0），并发安全。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchStockVO batchSetStock(List<BatchStockRow> rows) {
        int success = 0;
        List<BatchStockVO.Failure> failures = new java.util.ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (BatchStockRow row : rows) {
            String code = row.goodsCode() == null ? "" : row.goodsCode().trim();
            if (code.isEmpty()) {
                continue;
            }
            if (!seen.add(code.toUpperCase())) {
                failures.add(new BatchStockVO.Failure(code, null, "商品编码在文件中重复，仅处理首次出现"));
                continue;
            }
            Goods goods = goodsService.getOne(
                    Wrappers.<Goods>lambdaQuery().eq(Goods::getGoodsCode, code).last("LIMIT 1"));
            if (goods == null) {
                failures.add(new BatchStockVO.Failure(code, null, "商品编码不存在"));
                continue;
            }
            int target;
            try {
                target = Integer.parseInt(row.stock() == null ? "" : row.stock().trim());
            } catch (NumberFormatException e) {
                failures.add(new BatchStockVO.Failure(code, goods.getName(), "目标库存必须为非负整数"));
                continue;
            }
            if (target < 0) {
                failures.add(new BatchStockVO.Failure(code, goods.getName(), "目标库存不能小于 0"));
                continue;
            }
            int current = goods.getStock() == null ? 0 : goods.getStock();
            int delta = target - current;
            if (delta == 0) {
                success++;
                continue;
            }
            if (goodsMapper.adjustStock(goods.getId(), delta) == 0) {
                failures.add(new BatchStockVO.Failure(code, goods.getName(), "库存调整失败，请刷新后重试"));
                continue;
            }
            StockLog log = new StockLog();
            log.setGoodsId(goods.getId());
            log.setType(TYPE_CHECK);
            log.setChangeQty(delta);
            log.setBeforeStock(current);
            log.setAfterStock(target);
            log.setRemark(BATCH_REMARK);
            log.setCreatedBy(UserContext.get() == null ? null : UserContext.userId());
            stockLogMapper.insert(log);
            success++;
        }
        return new BatchStockVO(rows.size(), success, failures);
    }
}
