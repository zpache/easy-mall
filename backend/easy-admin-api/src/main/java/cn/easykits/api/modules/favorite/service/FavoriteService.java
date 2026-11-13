package cn.easykits.api.modules.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.easykits.api.common.api.PageResult;
import cn.easykits.api.common.exception.BusinessException;
import cn.easykits.api.common.api.ResultCode;
import cn.easykits.api.entity.Goods;
import cn.easykits.api.entity.UserFavorite;
import cn.easykits.api.mapper.GoodsMapper;
import cn.easykits.api.mapper.UserFavoriteMapper;
import cn.easykits.api.modules.favorite.vo.FavoriteItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品收藏：切换收藏 / 状态查询 / 分页列表。
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper favoriteMapper;
    private final GoodsMapper goodsMapper;

    /**
     * 切换收藏：未收藏则收藏，已收藏则取消。返回切换后是否已收藏。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggle(Long userId, Long goodsId) {
        requireGoods(goodsId);
        UserFavorite exists = favoriteMapper.selectOne(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getGoodsId, goodsId)
                .last("LIMIT 1"));
        if (exists != null) {
            favoriteMapper.deleteById(exists.getId());
            return false;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setGoodsId(goodsId);
        favoriteMapper.insert(favorite);
        return true;
    }

    /**
     * 商品是否已被当前用户收藏（商品详情页心形按钮初始态）。
     */
    public boolean isFavorited(Long userId, Long goodsId) {
        return favoriteMapper.selectCount(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getGoodsId, goodsId)) > 0;
    }

    /**
     * 收藏分页：按收藏时间倒序；商品已删除/下架的记录保留，goodsStatus 标记。
     */
    public PageResult<FavoriteItemVO> pageQuery(Long userId, long pageNum, long pageSize) {
        Page<UserFavorite> page = favoriteMapper.selectPage(Page.of(pageNum, pageSize),
                Wrappers.<UserFavorite>lambdaQuery()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getId));
        List<Long> goodsIds = page.getRecords().stream().map(UserFavorite::getGoodsId).toList();
        Map<Long, Goods> goodsMap = goodsIds.isEmpty()
                ? Map.of()
                : goodsMapper.selectBatchIds(goodsIds).stream()
                        .collect(Collectors.toMap(Goods::getId, Function.identity(), (a, b) -> a));
        List<FavoriteItemVO> records = page.getRecords().stream()
                .map(favorite -> {
                    Goods goods = goodsMap.get(favorite.getGoodsId());
                    return new FavoriteItemVO(
                            favorite.getGoodsId(),
                            goods == null ? "商品已删除" : goods.getName(),
                            goods == null ? "" : goods.getMainImage(),
                            goods == null ? null : goods.getPrice(),
                            goods == null ? null : goods.getOriginalPrice(),
                            goods == null ? 0 : goods.getStatus(),
                            favorite.getCreatedAt());
                })
                .toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    private void requireGoods(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || Objects.equals(goods.getStatus(), 0)) {
            throw new BusinessException(ResultCode.GOODS_OFF_SHELF, "商品已下架或不存在");
        }
    }
}
