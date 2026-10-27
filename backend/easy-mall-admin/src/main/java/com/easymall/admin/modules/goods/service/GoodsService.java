package com.easymall.admin.modules.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Brand;
import com.easymall.admin.entity.Category;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.mapper.BrandMapper;
import com.easymall.admin.mapper.CategoryMapper;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.modules.goods.dto.GoodsForm;
import com.easymall.admin.modules.goods.vo.GoodsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品服务。
 */
@Service
@RequiredArgsConstructor
public class GoodsService extends ServiceImpl<GoodsMapper, Goods> {

    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;

    public PageResult<GoodsVO> pageQuery(long pageNum, long pageSize, String keyword,
                                         Long categoryId, Long brandId, Integer status) {
        LambdaQueryWrapper<Goods> wrapper = Wrappers.<Goods>lambdaQuery()
                .like(StringUtils.hasText(keyword), Goods::getName, keyword)
                .eq(categoryId != null, Goods::getCategoryId, categoryId)
                .eq(brandId != null, Goods::getBrandId, brandId)
                .eq(status != null, Goods::getStatus, status)
                .orderByDesc(Goods::getId);
        Page<Goods> page = page(Page.of(pageNum, pageSize), wrapper);
        List<GoodsVO> voList = toVOList(page.getRecords());
        return new PageResult<>(voList, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public GoodsVO detail(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return toVOList(List.of(goods)).getFirst();
    }

    public Goods create(GoodsForm form) {
        checkCategory(form.categoryId());
        checkBrand(form.brandId());
        Goods goods = new Goods();
        applyForm(goods, form);
        goods.setSales(0);
        save(goods);
        return goods;
    }

    public Goods update(Long id, GoodsForm form) {
        Goods goods = getById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        checkCategory(form.categoryId());
        checkBrand(form.brandId());
        applyForm(goods, form);
        if (form.version() != null) {
            goods.setVersion(form.version());
        }
        if (!updateById(goods)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return goods;
    }

    /**
     * 上架/下架。
     */
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（下架）或 1（上架）");
        }
        Goods goods = getById(id);
        if (goods == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        goods.setStatus(status);
        if (!updateById(goods)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 批量把商品转成 VO：分类、品牌各查一次，避免逐行查询。
     */
    public List<GoodsVO> toVOList(List<Goods> goodsList) {
        if (goodsList == null || goodsList.isEmpty()) {
            return List.of();
        }
        List<Long> categoryIds = goodsList.stream().map(Goods::getCategoryId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Category> categoryMap = categoryIds.isEmpty()
                ? Map.of()
                : categoryMapper.selectList(Wrappers.<Category>lambdaQuery().in(Category::getId, categoryIds))
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Long> brandIds = goodsList.stream().map(Goods::getBrandId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Brand> brandMap = brandIds.isEmpty()
                ? Map.of()
                : brandMapper.selectList(Wrappers.<Brand>lambdaQuery().in(Brand::getId, brandIds))
                .stream().collect(Collectors.toMap(Brand::getId, Function.identity()));

        return goodsList.stream().map(goods -> {
            Category category = categoryMap.get(goods.getCategoryId());
            Brand brand = goods.getBrandId() == null ? null : brandMap.get(goods.getBrandId());
            return new GoodsVO(
                    goods.getId(),
                    goods.getCategoryId(),
                    category == null ? null : category.getName(),
                    goods.getBrandId(),
                    brand == null ? null : brand.getName(),
                    goods.getName(),
                    goods.getSubtitle(),
                    goods.getMainImage(),
                    goods.getImages(),
                    goods.getPrice(),
                    goods.getOriginalPrice(),
                    goods.getStock(),
                    goods.getStockWarn(),
                    goods.getSales(),
                    goods.getUnit(),
                    goods.getDetail(),
                    goods.getStatus(),
                    goods.getSort(),
                    goods.getCreatedAt(),
                    goods.getUpdatedAt(),
                    goods.getVersion());
        }).toList();
    }

    private void checkCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }
    }

    private void checkBrand(Long brandId) {
        if (brandId == null) {
            return;
        }
        if (brandMapper.selectById(brandId) == null) {
            throw new BusinessException("品牌不存在");
        }
    }

    private void applyForm(Goods goods, GoodsForm form) {
        goods.setCategoryId(form.categoryId());
        goods.setBrandId(form.brandId());
        goods.setName(form.name());
        goods.setSubtitle(form.subtitle());
        goods.setMainImage(form.mainImage());
        goods.setImages(form.images());
        goods.setPrice(form.price());
        goods.setOriginalPrice(form.originalPrice());
        goods.setStock(form.stock());
        goods.setStockWarn(form.stockWarn() == null ? 0 : form.stockWarn());
        goods.setUnit(form.unit());
        goods.setDetail(form.detail());
        goods.setStatus(form.status());
        goods.setSort(form.sort() == null ? 0 : form.sort());
    }
}
