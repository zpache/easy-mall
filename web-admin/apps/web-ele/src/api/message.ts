import { requestClient } from '#/api/request';

export namespace MessageApi {
  export interface Message {
    bizId?: number;
    bizType?: string;
    content?: string;
    createdAt: string;
    id: number;
    level: 'INFO' | 'URGENT' | 'WARN';
    link?: string;
    /** 全局已读：谁确认了这条，所有人都不会再看到 */
    read: boolean;
    readAt?: string;
    readBy?: number;
    title: string;
    type: string;
  }

  export interface PageResult<T> {
    pageNum: number;
    pageSize: number;
    pages: number;
    records: T[];
    total: number;
  }

  /** vxe-grid 需要 items + total，接口原始结构是 records + total，这里做一层转换 */
  export interface PagedResult<T> {
    items: T[];
    total: number;
  }

  export interface Query {
    onlyUnread?: boolean;
    pageNum?: number;
    pageSize?: number;
    type?: string;
  }
}

/**
 * 消息分页
 * <p>
 * 注意返回的是 { items, total }：列表页 vxe-grid 的 proxyConfig 全局按 items 取数据，
 * 直接返回后端的 records 会导致表格空白（铃铛弹层用的是同一份数据）。
 */
export async function getMessages(
  params: MessageApi.Query,
): Promise<MessageApi.PagedResult<MessageApi.Message>> {
  const result = await requestClient.get<
    MessageApi.PageResult<MessageApi.Message>
  >('/messages', { params });
  return { items: result.records, total: result.total };
}

/** 未读条数（铃铛角标） */
export async function getUnreadCount() {
  return requestClient.get<{ count: number }>('/messages/unread-count');
}

/** 单条已读（全局生效） */
export async function readMessage(id: number) {
  return requestClient.put<void>(`/messages/${id}/read`);
}

/** 全部已读，返回本次置为已读的条数 */
export async function readAllMessages() {
  return requestClient.put<number>('/messages/read-all');
}
