package cn.easykits.api.modules.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.easykits.api.common.api.PageResult;
import cn.easykits.api.common.api.ResultCode;
import cn.easykits.api.common.exception.BusinessException;
import cn.easykits.api.entity.Category;
import cn.easykits.api.entity.Goods;
import cn.easykits.api.entity.GoodsSku;
import cn.easykits.api.mapper.CategoryMapper;
import cn.easykits.api.mapper.GoodsMapper;
import cn.easykits.api.mapper.GoodsSkuMapper;
import cn.easykits.api.modules.goods.vo.HomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品浏览服务（只读，仅返回上架商品）。
 */
@Service
@RequiredArgsConstructor
public class GoodsService {

    /** 首页热销/新品展示数量 */
    private static final int HOME_LIMIT = 6;

    private final GoodsMapper goodsMapper;
    private final CategoryMapper categoryMapper;
    private final GoodsSkuMapper goodsSkuMapper;

    public PageResult<Goods> pageQuery(long pageNum, long pageSize, Long categoryId, String keyword, String sortBy) {
        LambdaQueryWrapper<Goods> wrapper = Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .eq(categoryId != null, Goods::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Goods::getName, keyword);
        applySort(wrapper, sortBy);
        Page<Goods> page = goodsMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        page.getRecords().forEach(this::mergeSales);
        return PageResult.of(page);
    }

    public Goods detail(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.GOODS_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(goods.getStatus())) {
            throw new BusinessException(ResultCode.GOODS_OFF_SHELF);
        }
        // 多规格商品：附带启用中的 SKU，前端据此渲染规格选择与价格/库存联动
        goods.setSkus(goodsSkuMapper.selectList(Wrappers.<GoodsSku>lambdaQuery()
                .eq(GoodsSku::getGoodsId, id)
                .eq(GoodsSku::getStatus, 1)
                .orderByAsc(GoodsSku::getId)));
        return mergeSales(goods);
    }

    public List<Category> categories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    public HomeVO home() {
        List<Category> categories = categories();
        List<Goods> hotGoods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .last("ORDER BY (sales + base_sales) DESC LIMIT " + HOME_LIMIT));
        List<Goods> newGoods = goodsMapper.selectList(Wrappers.<Goods>lambdaQuery()
                .eq(Goods::getStatus, 1)
                .orderByDesc(Goods::getId)
                .last("limit " + HOME_LIMIT));
        hotGoods.forEach(this::mergeSales);
        newGoods.forEach(this::mergeSales);
        return new HomeVO(categories, hotGoods, newGoods);
    }

    /**
     * 对外展示销量 = 真实销量 + 基础销量，副作用是把实体上的 sales 覆盖为合计值（仅用于响应序列化）。
     */
    private Goods mergeSales(Goods goods) {
        int sales = goods.getSales() == null ? 0 : goods.getSales();
        int base = goods.getBaseSales() == null ? 0 : goods.getBaseSales();
        goods.setSales(sales + base);
        return goods;
    }

    private void applySort(LambdaQueryWrapper<Goods> wrapper, String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            wrapper.orderByDesc(Goods::getSort).orderByDesc(Goods::getId);
            return;
        }
        switch (sortBy) {
            case "sales" -> wrapper.last("ORDER BY (sales + base_sales) DESC");
            case "priceAsc" -> wrapper.orderByAsc(Goods::getPrice);
            case "priceDesc" -> wrapper.orderByDesc(Goods::getPrice);
            default -> wrapper.orderByDesc(Goods::getId);
        }
    }
}
