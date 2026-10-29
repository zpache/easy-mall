package com.easymall.admin.modules.file.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.file.service.MaterialService;
import com.easymall.admin.modules.file.vo.MaterialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 素材中心：管理上传到 COS / 本地的素材文件。
 */
@Tag(name = "文件-素材中心")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @Operation(summary = "分页查询素材")
    @GetMapping
    public Result<PageResult<MaterialVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "20") long pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String mediaType,
                                               @RequestParam(required = false) String scene) {
        return Result.ok(materialService.pageQuery(pageNum, pageSize, keyword, mediaType, scene));
    }

    @Operation(summary = "重命名素材")
    @PutMapping("/{id}")
    public Result<MaterialVO> rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(materialService.rename(id, body.get("name")));
    }

    @Operation(summary = "删除素材（仅移除素材库记录）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.ok();
    }
}
