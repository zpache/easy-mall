package cn.easykits.admin.modules.message.controller;

import cn.easykits.admin.common.api.PageResult;
import cn.easykits.admin.common.api.Result;
import cn.easykits.admin.common.context.UserContext;
import cn.easykits.admin.modules.message.service.MessageService;
import cn.easykits.admin.modules.message.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息中心。已读是全局状态：谁确认了这条消息，所有人都不会再看到它。
 */
@Tag(name = "消息中心")
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "消息分页（type 可按类型过滤，onlyUnread 只看未读）")
    @GetMapping
    @PreAuthorize("hasAuthority('system:message:list')")
    public Result<PageResult<MessageVO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) Boolean onlyUnread) {
        return Result.ok(messageService.pageQuery(pageNum, pageSize, type, onlyUnread));
    }

    @Operation(summary = "未读消息数（铃铛角标轮询用）")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('system:message:list')")
    public Result<UnreadCountVO> unreadCount() {
        return Result.ok(new UnreadCountVO(messageService.unreadCount()));
    }

    @Operation(summary = "单条已读（全局生效，记录确认人）")
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('system:message:read')")
    public Result<Void> read(@PathVariable Long id) {
        messageService.markRead(UserContext.userId(), id);
        return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    @PreAuthorize("hasAuthority('system:message:read-all')")
    public Result<Integer> readAll() {
        return Result.ok(messageService.markAllRead(UserContext.userId()));
    }

    public record UnreadCountVO(long count) {
    }
}
