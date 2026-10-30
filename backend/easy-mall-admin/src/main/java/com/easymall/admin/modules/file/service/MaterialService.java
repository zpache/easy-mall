package com.easymall.admin.modules.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysFile;
import com.easymall.admin.mapper.SysFileMapper;
import com.easymall.admin.modules.file.vo.MaterialVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 素材中心：sys_file 的管理端查询 / 重命名 / 删除。
 * <p>
 * 删除为软删除，仅移除素材库记录，不清理对象存储（避免装修快照里的历史引用失效）。
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final SysFileMapper sysFileMapper;

    public PageResult<MaterialVO> pageQuery(long pageNum, long pageSize, String keyword,
                                            String mediaType, String scene) {
        LambdaQueryWrapper<SysFile> wrapper = Wrappers.<SysFile>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysFile::getName, keyword)
                        .or().like(SysFile::getObjectKey, keyword))
                .eq(StringUtils.hasText(scene), SysFile::getScene, scene)
                .like(StringUtils.hasText(mediaType), SysFile::getMime, mediaType + "/")
                .orderByDesc(SysFile::getId);
        Page<SysFile> page = sysFileMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        List<MaterialVO> records = page.getRecords().stream().map(this::toVo).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public MaterialVO rename(Long id, String name) {
        SysFile file = requireFile(id);
        if (!StringUtils.hasText(name)) {
            throw BusinessException.of("素材名称不能为空");
        }
        file.setName(name.trim());
        sysFileMapper.updateById(file);
        return toVo(file);
    }

    public void delete(Long id) {
        requireFile(id);
        sysFileMapper.deleteById(id);
    }

    private SysFile requireFile(Long id) {
        SysFile file = sysFileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return file;
    }

    private MaterialVO toVo(SysFile file) {
        return new MaterialVO(file.getId(), file.getName(), absoluteUrl(file.getUrl()),
                file.getObjectKey(), file.getSize(), file.getMime(), file.getScene(), file.getCreatedAt());
    }

    /**
     * 本地上传模式的 url 存的是相对路径（如 /admin/uploads/2026/10/xx.jpg），
     * 下发时按当前请求来源补全为绝对地址，保证前端「复制链接」拿到即可直接使用；
     * COS 模式本身就是完整桶地址，原样返回。
     */
    private String absoluteUrl(String url) {
        if (!StringUtils.hasText(url) || url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return url;
        }
        HttpServletRequest request = attrs.getRequest();
        StringBuilder origin = new StringBuilder()
                .append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            origin.append(':').append(port);
        }
        return origin + url;
    }
}
