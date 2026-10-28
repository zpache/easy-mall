package com.easymall.api.modules.decorate.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.entity.DecorateNav;
import com.easymall.api.entity.DecoratePage;
import com.easymall.api.mapper.DecorateNavMapper;
import com.easymall.api.mapper.DecoratePageMapper;
import com.easymall.api.modules.decorate.vo.DecoratePageVO;
import com.easymall.api.modules.decorate.vo.NavConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 装修只读服务：小程序端按已发布快照渲染首页 / 微页面，并下发全局导航配置。
 */
@Service
@RequiredArgsConstructor
public class DecorateService {

    /** 导航配置单例主键，与 admin 侧约定一致 */
    private static final long NAV_ID = 1L;

    private final DecoratePageMapper pageMapper;
    private final DecorateNavMapper navMapper;

    /** 商城首页：is_home=1 且已发布 */
    public DecoratePageVO home() {
        DecoratePage page = pageMapper.selectOne(Wrappers.<DecoratePage>lambdaQuery()
                .eq(DecoratePage::getIsHome, 1)
                .eq(DecoratePage::getStatus, 1)
                .last("limit 1"));
        if (page == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商城首页还未发布");
        }
        return toVO(page);
    }

    /** 微页面：仅已发布可访问 */
    public DecoratePageVO page(Long id) {
        DecoratePage page = pageMapper.selectById(id);
        if (page == null || page.getStatus() == null || page.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "页面不存在或未发布");
        }
        return toVO(page);
    }

    private DecoratePageVO toVO(DecoratePage page) {
        return new DecoratePageVO(page.getId(), page.getName(), page.getType(), content(page), nav());
    }

    /** 小程序只展示已发布内容；未发布（如首次上线前）回退草稿，避免空白 */
    private String content(DecoratePage page) {
        return StringUtils.hasText(page.getPublishedJson()) ? page.getPublishedJson() : page.getSchemaJson();
    }

    /** 导航配置单独下发，供非首页页面渲染底部 TabBar */
    public NavConfigVO navConfig() {
        return nav();
    }

    private NavConfigVO nav() {
        DecorateNav nav = navMapper.selectById(NAV_ID);
        if (nav == null) {
            return new NavConfigVO(null, null, null);
        }
        return new NavConfigVO(nav.getTabbarJson(), nav.getNavbarJson(), nav.getFloatJson());
    }
}
