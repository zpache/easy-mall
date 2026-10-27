package com.easymall.admin.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.SysDept;
import com.easymall.admin.entity.SysUser;
import com.easymall.admin.mapper.SysDeptMapper;
import com.easymall.admin.mapper.SysUserMapper;
import com.easymall.admin.modules.system.dto.DeptForm;
import com.easymall.admin.modules.system.vo.DeptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理：树形结构维护。删除前校验下级部门与部门下用户。
 */
@Service
@RequiredArgsConstructor
public class DeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    private final SysUserMapper sysUserMapper;

    /**
     * 部门树。一次性查出全部节点后在内存组树，避免逐层查库。
     */
    public List<DeptVO> listTree() {
        List<SysDept> all = list(Wrappers.<SysDept>lambdaQuery()
                .orderByAsc(SysDept::getPid)
                .orderByAsc(SysDept::getSort)
                .orderByAsc(SysDept::getId));
        Map<Long, List<SysDept>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(dept -> dept.getPid() == null ? 0L : dept.getPid()));
        return buildTree(childrenMap, 0L);
    }

    public SysDept create(DeptForm form) {
        SysDept dept = new SysDept();
        dept.setPid(form.pid() == null ? 0L : form.pid());
        dept.setName(form.name());
        dept.setSort(form.sort() == null ? 0 : form.sort());
        dept.setStatus(form.status() == null ? 1 : form.status());
        dept.setRemark(form.remark());
        validatePid(dept.getPid(), null);
        save(dept);
        return dept;
    }

    public SysDept update(Long id, DeptForm form) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }
        Long pid = form.pid() == null ? 0L : form.pid();
        validatePid(pid, id);
        dept.setPid(pid);
        dept.setName(form.name());
        dept.setSort(form.sort() == null ? 0 : form.sort());
        dept.setStatus(form.status() == null ? 1 : form.status());
        dept.setRemark(form.remark());
        if (!updateById(dept)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "数据已被其他人修改，请刷新后重试");
        }
        return dept;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "部门不存在");
        }
        long children = count(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getPid, id));
        if (children > 0) {
            throw new BusinessException("请先删除下级部门");
        }
        Long users = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, id));
        if (users != null && users > 0) {
            throw new BusinessException("该部门下还有用户，无法删除");
        }
        removeById(id);
    }

    /**
     * 部门下拉选项（含停用部门，前端自行按 status 过滤）。
     */
    public List<DeptVO> options() {
        return listTree();
    }

    private void validatePid(Long pid, Long selfId) {
        if (pid == null || pid == 0) {
            return;
        }
        if (pid.equals(selfId)) {
            throw new BusinessException("上级部门不能是自己");
        }
        if (getById(pid) == null) {
            throw new BusinessException("上级部门不存在");
        }
        if (selfId != null && isDescendant(pid, selfId)) {
            throw new BusinessException("上级部门不能是自己的下级");
        }
    }

    private boolean isDescendant(Long candidatePid, Long selfId) {
        Long current = candidatePid;
        while (current != null && current != 0) {
            if (current.equals(selfId)) {
                return true;
            }
            SysDept parent = getById(current);
            current = parent == null ? null : parent.getPid();
        }
        return false;
    }

    private List<DeptVO> buildTree(Map<Long, List<SysDept>> childrenMap, Long pid) {
        List<SysDept> children = childrenMap.get(pid);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<DeptVO> result = new ArrayList<>(children.size());
        for (SysDept dept : children) {
            result.add(toVO(dept, buildTree(childrenMap, dept.getId())));
        }
        return result;
    }

    private DeptVO toVO(SysDept dept, List<DeptVO> children) {
        return new DeptVO(dept.getId(), dept.getPid(), dept.getName(), dept.getSort(),
                dept.getStatus(), dept.getRemark(), dept.getCreatedAt(), children);
    }

    /**
     * 部门名是否重复（同层）。
     */
    public boolean nameExists(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            return false;
        }
        return count(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getName, name)
                .ne(excludeId != null, SysDept::getId, excludeId)) > 0;
    }

    /**
     * 分页查询（备用，部门管理默认用树）。
     */
    public Page<SysDept> pageQuery(long pageNum, long pageSize) {
        return page(Page.of(pageNum, pageSize), Wrappers.<SysDept>lambdaQuery().orderByAsc(SysDept::getSort));
    }
}
