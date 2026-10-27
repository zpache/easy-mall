package com.easymall.admin.modules.order.controller;

import com.easymall.admin.common.api.PageResult;
import com.easymall.admin.common.api.Result;
import com.easymall.admin.modules.order.dto.CommentReplyForm;
import com.easymall.admin.modules.order.service.CommentService;
import com.easymall.admin.modules.order.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评价管理。
 */
@Tag(name = "订单-评价")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "分页查询评价")
    @GetMapping
    public Result<PageResult<CommentVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer score,
                                              @RequestParam(required = false) Integer isShow) {
        return Result.ok(commentService.pageQuery(pageNum, pageSize, keyword, score, isShow));
    }

    @Operation(summary = "显示 / 隐藏评价")
    @PutMapping("/{id}/visible")
    public Result<Void> updateVisible(@PathVariable Long id, @RequestParam Integer isShow) {
        commentService.updateVisible(id, isShow);
        return Result.ok();
    }

    @Operation(summary = "回复评价")
    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody CommentReplyForm form) {
        commentService.reply(id, form.reply());
        return Result.ok();
    }

    @Operation(summary = "删除评价")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }
}
