package cn.easykits.api.modules.decorate.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.easykits.api.common.api.ResultCode;
import cn.easykits.api.common.exception.BusinessException;
import cn.easykits.api.entity.DecorateNav;
import cn.easykits.api.entity.DecoratePage;
import cn.easykits.api.entity.DecorateTheme;
import cn.easykits.api.mapper.DecorateNavMapper;
import cn.easykits.api.mapper.DecoratePageMapper;
import cn.easykits.api.mapper.DecorateThemeMapper;
import cn.easykits.api.modules.decorate.vo.DecoratePageVO;
import cn.easykits.api.modules.decorate.vo.NavConfigVO;
import cn.easykits.api.modules.decorate.vo.ThemeConfigVO;
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
    private final DecorateThemeMapper themeMapper;

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
        if (page == null || page.getStatus() == null || page.getStatus() != 1
                || !StringUtils.hasText(page.getPublishedJson())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "页面不存在或未发布");
        }
        return toVO(page);
    }

    private DecoratePageVO toVO(DecoratePage page) {
        return new DecoratePageVO(page.getId(), page.getName(), page.getType(), content(page), nav());
    }

    /** 小程序只展示已发布快照：保存草稿 / 编辑画布不影响线上，发布是唯一生效闸门 */
    private String content(DecoratePage page) {
        return page.getPublishedJson();
    }

    /** 导航配置单独下发，供非首页页面渲染底部 TabBar */
    public NavConfigVO navConfig() {
        return nav();
    }

    /** 线上主题：已启用主题的发布快照；无启用主题时 themeJson 为 null，小程序端回退默认配色 */
    public ThemeConfigVO themeConfig() {
        DecorateTheme theme = themeMapper.selectOne(Wrappers.<DecorateTheme>lambdaQuery()
                .eq(DecorateTheme::getIsActive, 1)
                .last("limit 1"));
        if (theme == null || !StringUtils.hasText(theme.getPublishedJson())) {
            return new ThemeConfigVO(null, null);
        }
        return new ThemeConfigVO(theme.getName(), theme.getPublishedJson());
    }

    private NavConfigVO nav() {
        DecorateNav nav = navMapper.selectById(NAV_ID);
        if (nav == null) {
            return new NavConfigVO(null, null, null);
        }
        // 只下发已发布快照；未发布过返回 null，小程序端落到内置默认导航
        return new NavConfigVO(nav.getTabbarPublishedJson(), nav.getNavbarPublishedJson(),
                nav.getFloatPublishedJson());
    }
}
