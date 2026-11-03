package com.easymall.api.modules.browse.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easymall.api.common.api.PageResult;
import com.easymall.api.entity.Goods;
import com.easymall.api.entity.UserBrowseLog;
import com.easymall.api.mapper.GoodsMapper;
import com.easymall.api.mapper.UserBrowseLogMapper;
import com.easymall.api.modules.browse.vo.BrowseItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 浏览足迹：进入商品详情即记录（upsert 刷新时间），每人保留最近 {@link #MAX_KEEP} 条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrowseService {

    /** 足迹保留上限：超出时裁剪最旧的 */
    private static final int MAX_KEEP = 200;

    private final UserBrowseLogMapper browseMapper;
    private final GoodsMapper goodsMapper;

    /**
     * 记录一次浏览：同商品 upsert 刷新时间；顺带按限量裁剪旧足迹。
     */
    public void record(Long userId, Long goodsId) {
        browseMapper.upsert(userId, goodsId);
        trim(userId);
    }

    /**
     * 足迹分页：按最近浏览时间倒序；商品已删除/下架的记录保留，goodsStatus 标记。
     */
    public PageResult<BrowseItemVO> pageQuery(Long userId, long pageNum, long pageSize) {
        Page<UserBrowseLog> page = browseMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<UserBrowseLog>lambdaQuery()
                        .eq(UserBrowseLog::getUserId, userId)
                        .orderByDesc(UserBrowseLog::getBrowsedAt));
        List<Long> goodsIds = page.getRecords().stream().map(UserBrowseLog::getGoodsId).toList();
        Map<Long, Goods> goodsMap = goodsIds.isEmpty()
                ? Map.of()
                : goodsMapper.selectBatchIds(goodsIds).stream()
                        .collect(Collectors.toMap(Goods::getId, Function.identity(), (a, b) -> a));
        List<BrowseItemVO> records = page.getRecords().stream()
                .map(logEntry -> {
                    Goods goods = goodsMap.get(logEntry.getGoodsId());
                    return new BrowseItemVO(
                            logEntry.getGoodsId(),
                            goods == null ? "商品已删除" : goods.getName(),
                            goods == null ? "" : goods.getMainImage(),
                            goods == null ? null : goods.getPrice(),
                            goods == null ? null : goods.getOriginalPrice(),
                            goods == null ? 0 : goods.getStatus(),
                            logEntry.getBrowsedAt());
                })
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /**
     * 足迹限量：超出 MAX_KEEP 的最旧记录物理删除。失败只记日志，不影响记录主流程。
     */
    private void trim(Long userId) {
        try {
            List<Long> staleIds = browseMapper.selectList(Wrappers.<UserBrowseLog>lambdaQuery()
                            .eq(UserBrowseLog::getUserId, userId)
                            .orderByDesc(UserBrowseLog::getBrowsedAt)
                            .last("LIMIT " + MAX_KEEP + ", " + MAX_KEEP))
                    .stream()
                    .map(UserBrowseLog::getId)
                    .toList();
            if (!staleIds.isEmpty()) {
                browseMapper.deleteByIds(staleIds);
            }
        } catch (Exception e) {
            log.warn("足迹裁剪失败: userId={}", userId, e);
        }
    }
}
