package com.easymall.admin.modules.decorate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.DecoratePage;
import com.easymall.admin.mapper.DecoratePageMapper;
import com.easymall.admin.modules.decorate.dto.PageForm;
import com.easymall.admin.modules.decorate.dto.PageSaveForm;
import com.easymall.admin.modules.decorate.vo.PageListVO;
import com.easymall.admin.modules.decorate.vo.PagePreviewVO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 商城装修页面：草稿保存、发布、复制、首页设置与扫码预览。
 */
@Service
@RequiredArgsConstructor
public class DecoratePageService extends ServiceImpl<DecoratePageMapper, DecoratePage> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int MAX_SCHEMA_LENGTH = 1_000_000;
    private static final String EMPTY_SCHEMA =
            "{\"page\":{\"title\":\"\",\"bgColor\":\"#f5f6f7\",\"bgImage\":\"\",\"padding\":0},\"components\":[]}";


    public PageResult<PageListVO> pageQuery(long pageNum, long pageSize, String keyword, Integer type, Integer status) {
        LambdaQueryWrapper<DecoratePage> wrapper = Wrappers.<DecoratePage>lambdaQuery()
                .select(DecoratePage::getId, DecoratePage::getName, DecoratePage::getType,
                        DecoratePage::getStatus, DecoratePage::getIsHome, DecoratePage::getPreviewCode,
                        DecoratePage::getRemark, DecoratePage::getPublishedAt, DecoratePage::getUpdatedAt,
                        DecoratePage::getVersion)
                .like(StringUtils.hasText(keyword), DecoratePage::getName, keyword)
                .eq(type != null, DecoratePage::getType, type)
                .eq(status != null, DecoratePage::getStatus, status)
                .orderByDesc(DecoratePage::getIsHome)
                .orderByDesc(DecoratePage::getId);
        Page<DecoratePage> page = page(Page.of(pageNum, pageSize), wrapper);
        List<PageListVO> rows = page.getRecords().stream()
                .map(item -> new PageListVO(
                        item.getId(), item.getName(), item.getType(), item.getStatus(), item.getIsHome(),
                        item.getPreviewCode(), item.getRemark(),
                        format(item.getPublishedAt()), format(item.getUpdatedAt()), item.getVersion()))
                .toList();
        return new PageResult<>(rows, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public DecoratePage detail(Long id) {
        DecoratePage page = getById(id);
        if (page == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return page;
    }

    public DecoratePage create(PageForm form) {
        if (form.type() != null && form.type() == 1 && existsHomePage()) {
            throw new BusinessException("商城首页只能有一个，不能再新建首页类型页面");
        }
        DecoratePage page = new DecoratePage();
        page.setName(form.name());
        page.setType(form.type());
        page.setRemark(form.remark());
        page.setStatus(0);
        page.setIsHome(0);
        page.setPreviewCode(newPreviewCode());
        page.setSchemaJson(EMPTY_SCHEMA);
        save(page);
        return page;
    }

    /** 保存草稿：整页 JSON 覆盖，带乐观锁。 */
    public DecoratePage saveSchema(Long id, PageSaveForm form) {
        DecoratePage page = getById(id);
        if (page == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        validateSchema(form.schemaJson());
        page.setSchemaJson(form.schemaJson());
        if (StringUtils.hasText(form.name())) {
            page.setName(form.name());
        }
        if (form.version() != null) {
            page.setVersion(form.version());
        }
        if (!updateById(page)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "页面已在其他地方被修改，请刷新后重试");
        }
        return page;
    }

    /** 发布：把当前草稿快照成已发布内容。 */
    public DecoratePage publish(Long id) {
        DecoratePage page = getById(id);
        if (page == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        JsonNode schema = validateSchema(page.getSchemaJson());
        JsonNode components = schema.get("components");
        if (components == null || !components.isArray() || components.isEmpty()) {
            throw new BusinessException("画布还是空的，至少添加一个组件后再发布");
        }
        page.setPublishedJson(page.getSchemaJson());
        page.setStatus(1);
        page.setPublishedAt(LocalDateTime.now());
        if (!updateById(page)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "页面已在其他地方被修改，请刷新后重试");
        }
        return page;
    }

    /** 复制为草稿，不继承首页身份与发布状态；首页复制出的副本降级为微页面。 */
    public DecoratePage copy(Long id) {
        DecoratePage source = getById(id);
        if (source == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        DecoratePage copy = new DecoratePage();
        copy.setName(source.getName() + " 副本");
        // 首页全局唯一，副本只能以微页面形态存在
        copy.setType(source.getType() != null && source.getType() == 1 ? 3 : source.getType());
        copy.setRemark(source.getRemark());
        copy.setStatus(0);
        copy.setIsHome(0);
        copy.setPreviewCode(newPreviewCode());
        copy.setSchemaJson(source.getSchemaJson());
        save(copy);
        return copy;
    }

    /** 设为商城首页：仅首页类型页面可设，且全局只保留一个首页标记。 */
    @Transactional
    public DecoratePage setHome(Long id) {
        DecoratePage page = getById(id);
        if (page == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (page.getType() == null || page.getType() != 1) {
            throw new BusinessException("只有首页类型的页面才能设为商城首页");
        }
        if (page.getStatus() == null || page.getStatus() != 1) {
            throw new BusinessException("请先发布该页面，再设为首页");
        }
        update(Wrappers.<DecoratePage>lambdaUpdate()
                .set(DecoratePage::getIsHome, 0)
                .eq(DecoratePage::getIsHome, 1));
        page.setIsHome(1);
        if (!updateById(page)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "页面已在其他地方被修改，请刷新后重试");
        }
        return page;
    }

    public void delete(Long id) {
        DecoratePage page = getById(id);
        if (page == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (page.getIsHome() != null && page.getIsHome() == 1) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "商城首页不能删除，请先把其他页面设为首页");
        }
        removeById(id);
    }

    /** 真机扫码预览：免登录，返回当前编辑内容。 */
    public PagePreviewVO preview(String code) {
        DecoratePage page = getOne(Wrappers.<DecoratePage>lambdaQuery()
                .eq(DecoratePage::getPreviewCode, code), false);
        if (page == null) {
            throw new BusinessException("预览码不存在或页面已删除");
        }
        return new PagePreviewVO(page.getId(), page.getName(), page.getSchemaJson(),
                page.getPreviewCode(), page.getStatus());
    }

    /** 重新生成预览码（旧码立即失效）。 */
    public DecoratePage resetPreviewCode(Long id) {
        DecoratePage page = getById(id);
        if (page == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        page.setPreviewCode(newPreviewCode());
        if (!updateById(page)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "页面已在其他地方被修改，请刷新后重试");
        }
        return page;
    }

    /** 是否已存在首页类型页面（首页全局唯一，不允许再建）。 */
    private boolean existsHomePage() {
        return count(Wrappers.<DecoratePage>lambdaQuery().eq(DecoratePage::getType, 1)) > 0;
    }

    private JsonNode validateSchema(String schemaJson) {        if (!StringUtils.hasText(schemaJson)) {
            throw new BusinessException("页面结构不能为空");
        }
        if (schemaJson.length() > MAX_SCHEMA_LENGTH) {
            throw new BusinessException("页面结构过大，请减少组件或图片数量");
        }
        JsonNode node;
        try {
            node = JSON.readTree(schemaJson);
        } catch (Exception e) {
            throw new BusinessException("页面结构不是合法的 JSON");
        }
        if (node == null || !node.isObject()) {
            throw new BusinessException("页面结构必须是 JSON 对象");
        }
        JsonNode components = node.get("components");
        if (components == null || !components.isArray()) {
            throw new BusinessException("页面结构缺少 components 数组");
        }
        return node;
    }

    private String newPreviewCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        } while (count(Wrappers.<DecoratePage>lambdaQuery().eq(DecoratePage::getPreviewCode, code)) > 0);
        return code;
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }
}
