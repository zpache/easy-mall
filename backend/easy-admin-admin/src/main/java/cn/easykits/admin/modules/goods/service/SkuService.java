package cn.easykits.admin.modules.goods.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.admin.common.api.ResultCode;
import cn.easykits.admin.common.exception.BusinessException;
import cn.easykits.admin.entity.Goods;
import cn.easykits.admin.entity.GoodsSku;
import cn.easykits.admin.mapper.GoodsMapper;
import cn.easykits.admin.mapper.GoodsSkuMapper;
import cn.easykits.admin.modules.goods.dto.SkuConfigForm;
import cn.easykits.admin.modules.goods.vo.SkuConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品规格与 SKU 配置。
 *
 * <p>数据模型：mall_goods.specs 存规格定义（JSON），每个规格组合在 mall_goods_sku 落一行。
 * 商品表的 price / stock 是展示聚合值（最低价 / 库存合计），由本服务在保存后重算，
 * 保证列表页、库存管理、小程序列表看到的都是 SKU 的真实汇总。</p>
 *
 * <p>校验口径：规格定义必须能被前端枚举成完整组合；提交的 skus 必须与组合一一对应，
 * 否则用户在小程序里会选不出某个规格。删除的 SKU 走逻辑删除，历史订单仍持有快照。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** 规格组数量上限（颜色 + 尺寸 + 版本 已经足够） */
    private static final int MAX_GROUPS = 3;
    /** 单个规格组的取值数量上限 */
    private static final int MAX_VALUES = 20;
    /** 组合总数上限，防止前端误操作生成海量 SKU */
    private static final int MAX_COMBINATIONS = 200;

    private final GoodsMapper goodsMapper;
    private final GoodsSkuMapper goodsSkuMapper;

    public SkuConfigVO get(Long goodsId) {
        Goods goods = requireGoods(goodsId);
        List<GoodsSku> skus = goodsSkuMapper.selectList(Wrappers.<GoodsSku>lambdaQuery()
                .eq(GoodsSku::getGoodsId, goodsId)
                .orderByAsc(GoodsSku::getId));
        return new SkuConfigVO(goods.getId(), goods.getSpecs(), skus.stream()
                .map(sku -> new SkuConfigVO.SkuItem(
                        sku.getId(),
                        sku.getSkuCode(),
                        sku.getSpecText(),
                        sku.getPrice(),
                        sku.getOriginalPrice(),
                        sku.getStock(),
                        sku.getSales(),
                        sku.getImage(),
                        sku.getStatus()))
                .toList());
    }

    /**
     * 保存规格与 SKU，并重算商品的价格 / 库存 / 销量汇总。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkuConfigVO save(Long goodsId, SkuConfigForm form) {
        Goods goods = requireGoods(goodsId);
        boolean single = !StringUtils.hasText(form.specs());
        if (single) {
            clearSkus(goodsId);
            updateGoodsAggregate(goods, null, null, null, null, null);
            return get(goodsId);
        }

        List<String> expected = parseSpecTexts(form.specs());
        List<SkuConfigForm.SkuItem> items = form.skus() == null ? List.of() : form.skus();
        validateItems(items, expected);

        Map<Long, GoodsSku> existing = goodsSkuMapper.selectList(Wrappers.<GoodsSku>lambdaQuery()
                        .eq(GoodsSku::getGoodsId, goodsId))
                .stream()
                .collect(Collectors.toMap(GoodsSku::getId, Function.identity()));

        Set<Long> kept = new HashSet<>();
        for (SkuConfigForm.SkuItem item : items) {
            GoodsSku sku = item.id() == null ? null : existing.get(item.id());
            if (sku == null) {
                sku = new GoodsSku();
                sku.setGoodsId(goodsId);
                sku.setSkuCode(nextSkuCode(goods));
                sku.setSales(0);
                sku.setStatus(item.status() == null ? 1 : item.status());
                applyItem(sku, item);
                goodsSkuMapper.insert(sku);
            } else {
                applyItem(sku, item);
                goodsSkuMapper.updateById(sku);
            }
            kept.add(sku.getId());
        }
        // 已下线/被移除的组合：逻辑删除，历史订单仍保留规格快照
        for (GoodsSku old : existing.values()) {
            if (!kept.contains(old.getId())) {
                goodsSkuMapper.deleteById(old.getId());
            }
        }

        List<GoodsSku> saved = goodsSkuMapper.selectList(Wrappers.<GoodsSku>lambdaQuery()
                .eq(GoodsSku::getGoodsId, goodsId));
        BigDecimal minPrice = null;
        BigDecimal minOriginal = null;
        int stock = 0;
        int sales = 0;
        for (GoodsSku sku : saved) {
            stock += nz(sku.getStock());
            sales += nz(sku.getSales());
            if (Integer.valueOf(1).equals(sku.getStatus()) && sku.getPrice() != null) {
                minPrice = minPrice == null || sku.getPrice().compareTo(minPrice) < 0 ? sku.getPrice() : minPrice;
                if (sku.getOriginalPrice() != null
                        && (minOriginal == null || sku.getOriginalPrice().compareTo(minOriginal) < 0)) {
                    minOriginal = sku.getOriginalPrice();
                }
            }
        }
        updateGoodsAggregate(goods, form.specs(), minPrice, minOriginal, stock, sales);
        return get(goodsId);
    }

    /** 商品汇总更新：specs 传 null 表示清空规格（改回单规格） */
    private void updateGoodsAggregate(Goods goods, String specs, BigDecimal price,
                                      BigDecimal originalPrice, Integer stock, Integer sales) {
        int finalStock = stock == null ? nz(goods.getStock()) : stock;
        int finalSales = sales == null ? nz(goods.getSales()) : sales;
        BigDecimal finalPrice = price == null ? goods.getPrice() : price;
        goodsMapper.update(null, Wrappers.<Goods>lambdaUpdate()
                .eq(Goods::getId, goods.getId())
                .set(Goods::getSpecs, specs)
                .set(Goods::getPrice, finalPrice)
                .set(Goods::getOriginalPrice, originalPrice)
                .set(Goods::getStock, finalStock)
                .set(Goods::getSales, finalSales));
    }

    private void clearSkus(Long goodsId) {
        List<GoodsSku> skus = goodsSkuMapper.selectList(Wrappers.<GoodsSku>lambdaQuery()
                .eq(GoodsSku::getGoodsId, goodsId));
        for (GoodsSku sku : skus) {
            goodsSkuMapper.deleteById(sku.getId());
        }
    }

    private void applyItem(GoodsSku sku, SkuConfigForm.SkuItem item) {
        sku.setSpecText(item.specText().trim());
        sku.setPrice(item.price());
        sku.setOriginalPrice(item.originalPrice());
        sku.setStock(item.stock() == null ? 0 : item.stock());
        sku.setImage(StringUtils.hasText(item.image()) ? item.image() : null);
        sku.setStatus(item.status() == null ? 1 : item.status());
    }

    private void validateItems(List<SkuConfigForm.SkuItem> items, List<String> expected) {
        if (items.size() != expected.size()) {
            throw new BusinessException("规格组合不完整：应生成 " + expected.size()
                    + " 个 SKU，实际提交 " + items.size() + " 个");
        }
        Set<String> expectedSet = new LinkedHashSet<>(expected);
        Set<String> seen = new HashSet<>();
        for (SkuConfigForm.SkuItem item : items) {
            if (!StringUtils.hasText(item.specText())) {
                throw new BusinessException("SKU 规格值不能为空");
            }
            String key = item.specText().trim();
            if (!expectedSet.contains(key)) {
                throw new BusinessException("SKU 规格值「" + key + "」不在规格定义里");
            }
            if (!seen.add(key)) {
                throw new BusinessException("SKU 规格值「" + key + "」重复");
            }
            if (item.price() == null || item.price().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("SKU「" + key + "」的销售价不能为空或小于 0");
            }
            if (item.stock() != null && item.stock() < 0) {
                throw new BusinessException("SKU「" + key + "」的库存不能小于 0");
            }
        }
    }

    /**
     * 解析规格定义并展开全部组合：每个组合按规格顺序用英文逗号拼接，
     * 与下单、购物车里的 spec_text 口径一致。
     */
    private List<String> parseSpecTexts(String specsJson) {
        JsonNode root;
        try {
            root = JSON.readTree(specsJson);
        } catch (Exception e) {
            throw new BusinessException("规格定义不是合法的 JSON");
        }
        if (root == null || !root.isArray() || root.size() == 0) {
            throw new BusinessException("规格定义至少需要一个规格组");
        }
        if (root.size() > MAX_GROUPS) {
            throw new BusinessException("规格组最多 " + MAX_GROUPS + " 个");
        }
        List<List<String>> valuesList = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (JsonNode group : root) {
            String name = group.path("name").asText("").trim();
            if (name.isEmpty()) {
                throw new BusinessException("规格名称不能为空");
            }
            if (!names.add(name)) {
                throw new BusinessException("规格名称「" + name + "」重复");
            }
            JsonNode values = group.get("values");
            if (values == null || !values.isArray() || values.isEmpty()) {
                throw new BusinessException("规格「" + name + "」至少需要一个规格值");
            }
            if (values.size() > MAX_VALUES) {
                throw new BusinessException("规格「" + name + "」最多 " + MAX_VALUES + " 个规格值");
            }
            List<String> list = new ArrayList<>(values.size());
            for (JsonNode value : values) {
                String text = value.asText("").trim();
                if (text.isEmpty()) {
                    throw new BusinessException("规格「" + name + "」存在空的规格值");
                }
                if (list.contains(text)) {
                    throw new BusinessException("规格「" + name + "」的规格值「" + text + "」重复");
                }
                // 逗号是组合分隔符，会破坏规格文案解析，直接拦住
                if (text.contains(",")) {
                    throw new BusinessException("规格值不能包含英文逗号");
                }
                list.add(text);
            }
            valuesList.add(list);
        }

        List<String> result = new ArrayList<>();
        collect(valuesList, 0, new ArrayList<>(), result);
        if (result.size() > MAX_COMBINATIONS) {
            throw new BusinessException("规格组合超过 " + MAX_COMBINATIONS + " 个，请精简规格值");
        }
        return result;
    }

    private void collect(List<List<String>> valuesList, int index, List<String> current, List<String> result) {
        if (index == valuesList.size()) {
            result.add(String.join(",", current));
            return;
        }
        for (String value : valuesList.get(index)) {
            current.add(value);
            collect(valuesList, index + 1, current, result);
            current.removeLast();
        }
    }

    /**
     * 生成 SKU 编码：商品编码 + 两位序号。商品编码为空时退化为 G{商品ID}。
     * 生成后会查重，冲突时继续递增。
     */
    private String nextSkuCode(Goods goods) {
        String prefix = StringUtils.hasText(goods.getGoodsCode()) ? goods.getGoodsCode() : "G" + goods.getId();
        for (int i = 1; i <= 999; i++) {
            String code = prefix + "-" + String.format("%02d", i);
            if (goodsSkuMapper.selectCount(Wrappers.<GoodsSku>lambdaQuery()
                    .eq(GoodsSku::getSkuCode, code)) == 0) {
                return code;
            }
        }
        throw new BusinessException("SKU 编码生成失败，请检查规格数量");
    }

    private Goods requireGoods(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "商品不存在");
        }
        return goods;
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 商品是否配置了规格（供库存服务判断走商品级还是 SKU 级库存）。
     */
    public boolean hasSpecs(Long goodsId) {
        return goodsSkuMapper.selectCount(Wrappers.<GoodsSku>lambdaQuery()
                .eq(GoodsSku::getGoodsId, goodsId)) > 0;
    }

    /** 取启用的 SKU，库存调整抽屉用 */
    public List<GoodsSku> listEnabled(Long goodsId) {
        return goodsSkuMapper.selectList(Wrappers.<GoodsSku>lambdaQuery()
                .eq(GoodsSku::getGoodsId, goodsId)
                .eq(GoodsSku::getStatus, 1)
                .orderByAsc(GoodsSku::getId));
    }

    /** 按 ID 取 SKU，校验归属 */
    public GoodsSku requireSku(Long goodsId, Long skuId) {
        GoodsSku sku = goodsSkuMapper.selectById(skuId);
        if (sku == null || !Objects.equals(sku.getGoodsId(), goodsId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "SKU 不存在");
        }
        return sku;
    }
}
