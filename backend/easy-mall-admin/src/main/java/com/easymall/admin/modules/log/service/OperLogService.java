package com.easymall.admin.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.admin.entity.SysOperLog;
import com.easymall.admin.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理端操作日志分页查询。
 */
@Service
@RequiredArgsConstructor
public class OperLogService {

    private final SysOperLogMapper operLogMapper;

    public Page<SysOperLog> pageQuery(long pageNum, long pageSize, String username,
                                      String module, Integer resultCode) {
        LambdaQueryWrapper<SysOperLog> wrapper = Wrappers.<SysOperLog>lambdaQuery()
                .like(StringUtils.hasText(username), SysOperLog::getUsername, username)
                .like(StringUtils.hasText(module), SysOperLog::getModule, module)
                .eq(resultCode != null, SysOperLog::getResultCode, resultCode)
                .orderByDesc(SysOperLog::getId);
        return operLogMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
    }
}
