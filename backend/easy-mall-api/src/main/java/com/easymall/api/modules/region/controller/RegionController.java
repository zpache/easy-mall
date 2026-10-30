package com.easymall.api.modules.region.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.mapper.RegionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 行政区域（小程序省市区选择器数据，按 parentCode 逐级懒加载）。
 */
@Tag(name = "行政区域")
@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionMapper regionMapper;

    /** 区域节点 */
    public record RegionNode(String areaCode, String name, Integer level) {
    }

    @Operation(summary = "查询子区域（parentCode=0 查省级）")
    @GetMapping
    public Result<List<RegionNode>> children(@RequestParam(defaultValue = "0") String parentCode) {
        List<RegionNode> nodes = regionMapper.selectChildren(parentCode).stream()
                .map(row -> new RegionNode(
                        (String) row.get("area_code"),
                        (String) row.get("name"),
                        ((Number) row.get("level")).intValue()))
                .toList();
        return Result.ok(nodes);
    }
}
