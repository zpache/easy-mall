package com.easymall.admin.modules.decorate.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.DecorateTheme;
import com.easymall.admin.mapper.DecorateThemeMapper;
import com.easymall.admin.modules.decorate.dto.ThemeSaveForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 商城主题换肤：草稿 / 发布双快照 + 单启用，与页面装修同口径。
 * <p>
 * 保存只写草稿列；「启用」时把草稿快照到 published 列并全局唯一激活，
 * 小程序端只读已启用主题的 published 列——编辑与保存不影响线上，启用是唯一生效闸门。
 * JSON 结构在服务端收口校验，避免前端换了实现后往库里写脏数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DecorateThemeService extends ServiceImpl<DecorateThemeMapper, DecorateTheme> {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern HEX_COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    /** 主题配置必填键，全部为十六进制颜色 */
    private static final List<String> REQUIRED_KEYS =
            List.of("primaryColor", "pageBgColor", "priceColor", "textColor", "subTextColor");

    /** 全部主题（含草稿态），数量少不分页 */
    public List<DecorateTheme> listAll() {
        return list(Wrappers.<DecorateTheme>lambdaQuery().orderByDesc(DecorateTheme::getIsActive).orderByAsc(DecorateTheme::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public DecorateTheme create(ThemeSaveForm form) {
        validateConfig(form.configJson());
        DecorateTheme theme = new DecorateTheme();
        theme.setName(form.name().trim());
        theme.setConfigJson(form.configJson());
        theme.setRemark(form.remark());
        theme.setIsActive(0);
        save(theme);
        return getById(theme.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public DecorateTheme update(Long id, ThemeSaveForm form) {
        DecorateTheme theme = requireTheme(id);
        validateConfig(form.configJson());
        theme.setName(form.name().trim());
        theme.setConfigJson(form.configJson());
        theme.setRemark(form.remark());
        if (form.version() != null) {
            theme.setVersion(form.version());
        }
        if (!updateById(theme)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "主题已在其他地方被修改，请刷新后重试");
        }
        return getById(id);
    }

    /**
     * 启用即发布：草稿快照到 published 列，取消其他主题的启用态，全局唯一线上主题。
     */
    @Transactional(rollbackFor = Exception.class)
    public DecorateTheme activate(Long id) {
        DecorateTheme theme = requireTheme(id);
        validateConfig(theme.getConfigJson());
        // 发布前重新校验草稿，保证快照可用
        theme.setPublishedJson(theme.getConfigJson());
        theme.setIsActive(1);
        if (!updateById(theme)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "主题已在其他地方被修改，请刷新后重试");
        }
        // 其余主题全部下线
        update(Wrappers.<DecorateTheme>lambdaUpdate()
                .ne(DecorateTheme::getId, id)
                .eq(DecorateTheme::getIsActive, 1)
                .set(DecorateTheme::getIsActive, 0));
        log.info("主题启用: id={}, name={}", id, theme.getName());
        return getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DecorateTheme theme = requireTheme(id);
        if (theme.getIsActive() != null && theme.getIsActive() == 1) {
            throw BusinessException.of("线上主题不能删除，请先启用其他主题");
        }
        removeById(id);
    }

    private DecorateTheme requireTheme(Long id) {
        DecorateTheme theme = getById(id);
        if (theme == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return theme;
    }

    /** 主题配置结构收口：固定键 + 十六进制颜色，未知键忽略 */
    private void validateConfig(String configJson) {
        JsonNode node;
        try {
            node = JSON.readTree(configJson);
        } catch (Exception e) {
            throw BusinessException.of("主题配置不是合法 JSON");
        }
        if (node == null || !node.isObject()) {
            throw BusinessException.of("主题配置必须是 JSON 对象");
        }
        for (String key : REQUIRED_KEYS) {
            JsonNode value = node.get(key);
            if (value == null || !value.isString()) {
                throw BusinessException.of("主题配置缺少 %s".formatted(key));
            }
            if (!HEX_COLOR.matcher(value.asString()).matches()) {
                throw BusinessException.of("%s 必须是 #RGB 或 #RRGGBB 颜色值".formatted(key));
            }
        }
    }
}
