package com.easymall.admin.modules.trade.vo;

import lombok.Data;

import java.util.List;

/**
 * 行政区域节点（省 / 市两级树）。
 */
@Data
public class RegionNodeVO {

    private String code;

    private String name;

    /** 1 省 2 市 */
    private Integer level;

    private List<RegionNodeVO> children;
}
