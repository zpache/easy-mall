package com.easymall.admin.modules.decorate.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.DecorateNav;
import com.easymall.admin.mapper.DecorateNavMapper;
import com.easymall.admin.modules.decorate.dto.NavSaveForm;
import com.easymall.admin.modules.decorate.vo.NavPreviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 导航装修：底部 TabBar、顶部导航、快捷入口的单例配置。
 * <p>
 * 保存即生效（无草稿/发布双份），靠乐观锁防并发覆盖；JSON 结构在服务端收口校验，
 * 避免前端换了实现后往库里写脏数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DecorateNavService extends ServiceImpl<DecorateNavMapper, DecorateNav> {

    /** 单例配置固定主键 */
    public static final long SINGLETON_ID = 1L;

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern HEX_COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    private static final Set<String> BORDER_STYLES = Set.of("black", "white", "none");
    /** home 跳商城首页 / page 指定装修页面 / path 自定义路径 / none 不跳转 */
    private static final Set<String> LINK_TYPES = Set.of("home", "none", "page", "path");
    private static final Set<String> NAVBAR_STYLES = Set.of("immersive", "normal");
    private static final Set<String> BACK_BEHAVIORS = Set.of("back", "home", "none");
    private static final Set<String> FLOAT_POSITIONS = Set.of("left", "right");

    private static final int MIN_TABS = 3;
    private static final int MAX_TABS = 5;
    private static final int MIN_TAB_NAME = 1;
    private static final int MAX_TAB_NAME = 8;
    private static final int MAX_TITLE = 20;
    private static final int MAX_URL = 500;
    private static final int MAX_FLOAT_ITEMS = 5;

    private static final String DEFAULT_TABBAR = """
            {"enabled":true,"bgColor":"#ffffff","color":"#909399","selectedColor":"#1677ff","borderStyle":"black",\
            "items":[\
            {"key":"home","name":"首页","icon":"","selectedIcon":"","linkType":"home","linkValue":"","visible":true},\
            {"key":"category","name":"分类","icon":"","selectedIcon":"","linkType":"path","linkValue":"/pages/category/index","visible":true},\
            {"key":"cart","name":"购物车","icon":"","selectedIcon":"","linkType":"path","linkValue":"/pages/cart/index","visible":true},\
            {"key":"mine","name":"我的","icon":"","selectedIcon":"","linkType":"path","linkValue":"/pages/mine/index","visible":true}]}""";

    private static final String DEFAULT_NAVBAR = """
            {"title":"易商城","style":"normal","bgColor":"#ffffff","textColor":"#333333","showBack":true,"backBehavior":"back"}""";

    private static final String DEFAULT_FLOAT = """
            {"enabled":true,"position":"right","itemList":[\
            {"key":"service","name":"客服","icon":"","value":"400-800-1234","enabled":true},\
            {"key":"cart","name":"购物车","icon":"","value":"","enabled":true},\
            {"key":"top","name":"回到顶部","icon":"","value":"","enabled":true}]}""";

    /**
     * 读取配置；首次访问时落库默认值，保证前端拿到的 version 可用。
     */
    public DecorateNav get() {
        DecorateNav nav = getById(SINGLETON_ID);
        if (nav != null) {
            return nav;
        }
        DecorateNav init = new DecorateNav();
        init.setId(SINGLETON_ID);
        init.setTabbarJson(DEFAULT_TABBAR);
        init.setNavbarJson(DEFAULT_NAVBAR);
        init.setFloatJson(DEFAULT_FLOAT);
        init.setRemark("默认导航配置");
        save(init);
        log.info("初始化导航装修默认配置");
        return getById(SINGLETON_ID);
    }

    /** 免登录预览（扫码预览页）用：只回三段渲染配置。 */
    public NavPreviewVO previewConfig() {
        DecorateNav nav = get();
        return new NavPreviewVO(nav.getTabbarJson(), nav.getNavbarJson(), nav.getFloatJson());
    }

    /** 保存配置：三段 JSON 校验通过后整体覆盖。 */
    public DecorateNav save(NavSaveForm form) {
        DecorateNav nav = get();
        validateTabbar(form.tabbarJson());
        validateNavbar(form.navbarJson());
        validateFloat(form.floatJson());

        nav.setTabbarJson(form.tabbarJson());
        nav.setNavbarJson(form.navbarJson());
        nav.setFloatJson(form.floatJson());
        if (form.version() != null) {
            nav.setVersion(form.version());
        }
        if (!updateById(nav)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "导航配置已在其他地方被修改，请刷新后重试");
        }
        return getById(SINGLETON_ID);
    }

    /** 恢复默认配置。 */
    public DecorateNav reset() {
        DecorateNav nav = get();
        nav.setTabbarJson(DEFAULT_TABBAR);
        nav.setNavbarJson(DEFAULT_NAVBAR);
        nav.setFloatJson(DEFAULT_FLOAT);
        if (!updateById(nav)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "导航配置已在其他地方被修改，请刷新后重试");
        }
        return getById(SINGLETON_ID);
    }

    /* ---------------- 校验 ---------------- */

    private void validateTabbar(String json) {
        JsonNode root = readObject(json, "底部 TabBar");
        checkColor(root, "bgColor", "底部 TabBar 背景色");
        checkColor(root, "color", "底部 TabBar 未选中文字色");
        checkColor(root, "selectedColor", "底部 TabBar 选中色");
        checkEnum(root, "borderStyle", BORDER_STYLES, "底部 TabBar 上边框样式");

        JsonNode items = root.get("items");
        if (items == null || !items.isArray()) {
            throw new BusinessException("底部 TabBar 缺少 items 数组");
        }
        if (items.size() < MIN_TABS || items.size() > MAX_TABS) {
            throw new BusinessException("底部 TabBar 需要 " + MIN_TABS + "~" + MAX_TABS + " 个标签");
        }
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String prefix = "第 " + (i + 1) + " 个标签";
            if (item == null || !item.isObject()) {
                throw new BusinessException(prefix + "格式不正确");
            }
            String key = requireText(item, "key", prefix + "缺少标识");
            if (!keys.add(key)) {
                throw new BusinessException("标签标识重复：" + key);
            }
            String name = requireText(item, "name", prefix + "缺少文字");
            if (name.length() < MIN_TAB_NAME || name.length() > MAX_TAB_NAME) {
                throw new BusinessException(prefix + "的文字需为 " + MIN_TAB_NAME + "~" + MAX_TAB_NAME + " 个字符");
            }
            checkUrl(item, "icon", prefix + "未选中图标");
            checkUrl(item, "selectedIcon", prefix + "选中图标");

            String linkType = requireText(item, "linkType", prefix + "缺少跳转类型");
            if (!LINK_TYPES.contains(linkType)) {
                throw new BusinessException(prefix + "的跳转类型只能是 home / page / path / none");
            }
            String linkValue = text(item, "linkValue");
            if ("page".equals(linkType) && !StringUtils.hasText(linkValue)) {
                throw new BusinessException(prefix + "选择了跳转自定义页面，但没有指定页面");
            }
            if ("path".equals(linkType)) {
                if (!StringUtils.hasText(linkValue) || !linkValue.startsWith("/")) {
                    throw new BusinessException(prefix + "的自定义路径需以 / 开头");
                }
                if (linkValue.length() > MAX_URL) {
                    throw new BusinessException(prefix + "的自定义路径过长");
                }
            }
        }
    }

    private void validateNavbar(String json) {
        JsonNode root = readObject(json, "顶部导航");
        String title = requireText(root, "title", "顶部导航标题不能为空");
        if (title.length() > MAX_TITLE) {
            throw new BusinessException("顶部导航标题不能超过 " + MAX_TITLE + " 个字符");
        }
        checkEnum(root, "style", NAVBAR_STYLES, "顶部导航样式");
        checkEnum(root, "backBehavior", BACK_BEHAVIORS, "返回按钮行为");
        checkColor(root, "bgColor", "顶部导航背景色");
        checkColor(root, "textColor", "顶部导航文字色");
    }

    private void validateFloat(String json) {
        JsonNode root = readObject(json, "快捷入口");
        checkEnum(root, "position", FLOAT_POSITIONS, "快捷入口位置");
        JsonNode items = root.get("itemList");
        if (items == null || !items.isArray()) {
            throw new BusinessException("快捷入口缺少 itemList 数组");
        }
        if (items.size() > MAX_FLOAT_ITEMS) {
            throw new BusinessException("快捷入口最多 " + MAX_FLOAT_ITEMS + " 项");
        }
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String prefix = "第 " + (i + 1) + " 个快捷入口";
            if (item == null || !item.isObject()) {
                throw new BusinessException(prefix + "格式不正确");
            }
            String key = requireText(item, "key", prefix + "缺少标识");
            if (!keys.add(key)) {
                throw new BusinessException("快捷入口标识重复：" + key);
            }
            String name = requireText(item, "name", prefix + "缺少名称");
            if (name.length() > MAX_TAB_NAME) {
                throw new BusinessException(prefix + "的名称不能超过 " + MAX_TAB_NAME + " 个字符");
            }
            checkUrl(item, "icon", prefix + "图标");
            String value = text(item, "value");
            if (value != null && value.length() > 50) {
                throw new BusinessException(prefix + "的配置值不能超过 50 个字符");
            }
        }
    }

    /* ---------------- 校验辅助 ---------------- */

    private JsonNode readObject(String json, String label) {
        JsonNode node;
        try {
            node = JSON.readTree(json);
        } catch (Exception e) {
            throw new BusinessException(label + "配置不是合法的 JSON");
        }
        if (node == null || !node.isObject()) {
            throw new BusinessException(label + "配置必须是 JSON 对象");
        }
        return node;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String requireText(JsonNode node, String field, String message) {
        String value = text(node, field);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private void checkColor(JsonNode node, String field, String label) {
        String value = text(node, field);
        if (StringUtils.hasText(value) && !HEX_COLOR.matcher(value).matches()) {
            throw new BusinessException(label + "需为 #RGB 或 #RRGGBB 格式");
        }
    }

    private void checkUrl(JsonNode node, String field, String label) {
        String value = text(node, field);
        if (value != null && value.length() > MAX_URL) {
            throw new BusinessException(label + "地址过长");
        }
    }

    private void checkEnum(JsonNode node, String field, Set<String> allowed, String label) {
        String value = text(node, field);
        if (StringUtils.hasText(value) && !allowed.contains(value)) {
            throw new BusinessException(label + "取值不合法");
        }
    }
}
