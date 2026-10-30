package com.easymall.api.modules.comment.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.comment.dto.CommentCreateForm;
import com.easymall.api.modules.comment.service.CommentService;
import com.easymall.api.modules.comment.vo.GoodsCommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价：小程序提交评价、商品详情查看评价。
 *
 * <p>POST 需要登录；GET /comments/goods/** 与商品详情一样免登录（游客也能看评价）。</p>
 */
@Tag(name = "评价")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "提交评价（已完成订单，可一次提交多个商品）")
    @PostMapping
    public Result<Integer> submit(@Valid @RequestBody CommentCreateForm form) {
        return Result.ok(commentService.submit(UserContext.userId(), form));
    }

    @Operation(summary = "商品评价列表与评分概览")
    @GetMapping("/goods/{goodsId}")
    public Result<GoodsCommentVO> goodsComments(@PathVariable Long goodsId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(commentService.goodsComments(goodsId, pageNum, pageSize));
    }
}
