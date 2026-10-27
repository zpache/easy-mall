package com.easymall.admin.modules.system.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门树节点。
 */
public record DeptVO(
        Long id,
        Long pid,
        String name,
        Integer sort,
        Integer status,
        String remark,
        LocalDateTime createdAt,
        List<DeptVO> children
) {
}
