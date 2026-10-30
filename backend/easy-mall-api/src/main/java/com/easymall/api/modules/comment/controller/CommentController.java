package com.easymall.api.modules.comment.controller;

import com.easymall.api.common.api.Result;
import com.easymall.api.common.context.LoginUser;
import com.easymall.api.common.context.UserContext;
import com.easymall.api.modules.comment.dto.CommentCreateForm;
import com.easymall.api.modules.comment.service.CommentService;
import com.easymall.api.modules.comment.vo.GoodsCommentVO;
import com.easymall.api.security.LoginSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
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
 * <p>POST 需要登录；GET /comments/goods/** 与商品详情一样免登录（游客也能看评价）。
 * 免登录接口不在拦截器范围内，因此这里按需自行解析 token：已登录用户能看到自己的待审核评价。</p>
 */
@Tag(name = "评价")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final CommentService commentService;
    private final LoginSessionService sessionService;

    @Operation(summary = "提交评价（已完成订单，可一次提交多个商品，提交后进入待审核）")
    @PostMapping
    public Result<Integer> submit(@Valid @RequestBody CommentCreateForm form) {
        return Result.ok(commentService.submit(UserContext.userId(), form));
    }

    @Operation(summary = "商品评价列表与评分概览（含本人待审核评价）")
    @GetMapping("/goods/{goodsId}")
    public Result<GoodsCommentVO> goodsComments(@PathVariable Long goodsId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               HttpServletRequest request) {
        return Result.ok(commentService.goodsComments(goodsId, pageNum, pageSize, optionalUserId(request)));
    }

    /**
     * 可选登录态：带了有效 token 就按登录用户视角返回（多看自己的待审核评价），
     * 没带或已过期则按游客处理，不抛 401。
     */
    private Long optionalUserId(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return sessionService.resolve(token).map(LoginUser::userId).orElse(null);
    }
}
