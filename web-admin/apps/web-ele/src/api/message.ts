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

  export interface Query {
    onlyUnread?: boolean;
    pageNum?: number;
    pageSize?: number;
    type?: string;
  }
}

/** 消息分页 */
export async function getMessages(params: MessageApi.Query) {
  return requestClient.get<MessageApi.PageResult<MessageApi.Message>>(
    '/messages',
    { params },
  );
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
