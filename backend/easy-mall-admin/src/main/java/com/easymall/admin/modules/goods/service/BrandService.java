package com.easymall.admin.modules.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Brand;
import com.easymall.admin.entity.Goods;
import com.easymall.admin.mapper.BrandMapper;
import com.easymall.admin.mapper.GoodsMapper;
import com.easymall.admin.modules.goods.dto.BrandForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 品牌服务。
 */
@Service
@RequiredArgsConstructor
public class BrandService extends ServiceImpl<BrandMapper, Brand> {

    private final GoodsMapper goodsMapper;

    public PageResult<Brand> pageQuery(long pageNum, long pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<Brand> wrapper = Wrappers.<Brand>lambdaQuery()
                .like(StringUtils.hasText(keyword), Brand::getName, keyword)
                .eq(status != null, Brand::getStatus, status)
                .orderByAsc(Brand::getSort)
                .orderByDesc(Brand::getId);
        Page<Brand> page = page(Page.of(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /**
     * 启用中的品牌（下拉选项用）。
     */
    public List<Brand> listEnabled() {
        return list(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getStatus, 1)
                .orderByAsc(Brand::getSort)
                .orderByAsc(Brand::getId));
    }

    public Brand create(BrandForm form) {
        checkName(form.name(), null);
        Brand brand = new Brand();
        applyForm(brand, form);
        save(brand);
        return brand;
    }

    public Brand update(Long id, BrandForm form) {
        Brand brand = getById(id);
        if (brand == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        checkName(form.name(), id);
        applyForm(brand, form);
        if (form.version() != null) {
            brand.setVersion(form.version());
        }
        if (!updateById(brand)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return brand;
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0（停用）或 1（启用）");
        }
        Brand brand = getById(id);
        if (brand == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        brand.setStatus(status);
        if (!updateById(brand)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
    }

    public void delete(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        long used = goodsMapper.selectCount(Wrappers.<Goods>lambdaQuery().eq(Goods::getBrandId, id));
        if (used > 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT,
                    "该品牌下还有 " + used + " 个商品，请先调整商品的品牌");
        }
        removeById(id);
    }

    private void checkName(String name, Long excludeId) {
        long exists = count(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getName, name)
                .ne(excludeId != null, Brand::getId, excludeId));
        if (exists > 0) {
            throw new BusinessException("品牌名称已存在");
        }
    }

    private void applyForm(Brand brand, BrandForm form) {
        brand.setName(form.name());
        brand.setLogo(form.logo());
        brand.setSort(form.sort() == null ? 0 : form.sort());
        brand.setStatus(form.status() == null ? 1 : form.status());
        brand.setRemark(form.remark());
    }
}
