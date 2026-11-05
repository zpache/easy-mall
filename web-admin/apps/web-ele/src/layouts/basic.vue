<script lang="ts" setup>
import type { NotificationItem } from '@vben/layouts';

import type { MessageApi } from '#/api/message';

import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import { AuthenticationLoginExpiredModal } from '@vben/common-ui';
import { useWatermark } from '@vben/hooks';
import {
  BasicLayout,
  LockScreen,
  Notification,
  UserDropdown,
} from '@vben/layouts';
import { preferences, usePreferences } from '@vben/preferences';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  getMessages,
  getUnreadCount,
  readAllMessages,
  readMessage,
} from '#/api/message';
import { $t } from '#/locales';
import { useAuthStore } from '#/store';
import LoginForm from '#/views/_core/authentication/login.vue';

/** 消息中心页面：铃铛里的「查看全部」落到这里 */
const MESSAGE_PAGE = '/message';
/** 轮询间隔：60s 拉一次未读数与最新未读列表 */
const POLL_INTERVAL = 60_000;

const notifications = ref<NotificationItem[]>([]);
/** 未读条数，铃铛角标。来源是真实的消息表，不是库存预警数量 */
const unread = ref(0);
let pollTimer: null | ReturnType<typeof setInterval> = null;

function toNotificationItem(message: MessageApi.Message): NotificationItem {
  return {
    id: message.id,
    avatar: preferences.app.defaultAvatar,
    date: (message.createdAt ?? '').replace('T', ' ').slice(0, 16),
    isRead: message.read,
    message: message.content ?? '',
    title: message.title,
    link: message.link,
  };
}

/**
 * 只拉未读消息：确认过的会从列表消失，角标同步归零。
 * 失败静默：保留上次数据，不弹错误打断后台操作。
 */
async function loadNotifications() {
  try {
    const [page, count] = await Promise.all([
      getMessages({ onlyUnread: true, pageNum: 1, pageSize: 10 }),
      getUnreadCount(),
    ]);
    notifications.value = page.records.map(toNotificationItem);
    unread.value = count.count;
  } catch {
    // 网络抖动时保持现状，下次轮询重试
  }
}

onMounted(() => {
  loadNotifications();
  pollTimer = setInterval(loadNotifications, POLL_INTERVAL);
});

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
});

const router = useRouter();
const userStore = useUserStore();
const authStore = useAuthStore();
const accessStore = useAccessStore();
const { destroyWatermark, updateWatermark } = useWatermark();
const { isDark } = usePreferences();

const menus = computed(() => [
  {
    handler: () => {
      router.push({ name: 'Profile' });
    },
    icon: 'lucide:user',
    text: $t('page.auth.profile'),
  },
]);

const avatar = computed(() => {
  return userStore.userInfo?.avatar ?? preferences.app.defaultAvatar;
});

async function handleLogout() {
  await authStore.logout(false);
}

/**
 * 单条已读：已读是全局的，谁确认了这条消息所有人都不会再看到，
 * 所以本地先移除 + 角标减一，接口失败再回滚重拉。
 */
async function markRead(id: number | string) {
  notifications.value = notifications.value.filter((item) => item.id !== id);
  unread.value = Math.max(0, unread.value - 1);
  try {
    await readMessage(Number(id));
  } catch {
    loadNotifications();
  }
}

/** 全部已读 */
async function markAllRead() {
  try {
    await readAllMessages();
  } finally {
    notifications.value = [];
    unread.value = 0;
  }
}

function viewAll() {
  router.push(MESSAGE_PAGE);
}

const handleClick = (item: NotificationItem) => {
  if (item.id !== undefined) {
    markRead(item.id);
  }
  if (item.link) {
    navigateTo(item.link, item.query, item.state);
  }
};

function navigateTo(
  link: string,
  query?: Record<string, any>,
  state?: Record<string, any>,
) {
  if (link.startsWith('http://') || link.startsWith('https://')) {
    // 外部链接，在新标签页打开
    window.open(link, '_blank');
  } else {
    // 内部路由链接，支持 query 参数和 state
    router.push({
      path: link,
      query: query || {},
      state,
    });
  }
}

watch(
  () => ({
    enable: preferences.app.watermark,
    content: preferences.app.watermarkContent,
    isDark: isDark.value,
  }),
  async ({ enable, content, isDark: isDarkValue }) => {
    if (enable) {
      const watermarkColor = isDarkValue
        ? 'rgba(255, 255, 255, 0.12)'
        : 'rgba(0, 0, 0, 0.12)';

      await updateWatermark({
        advancedStyle: {
          colorStops: [
            {
              color: watermarkColor,
              offset: 0,
            },
            {
              color: watermarkColor,
              offset: 1,
            },
          ],
          type: 'linear',
        },
        content:
          content ||
          `${userStore.userInfo?.username} - ${userStore.userInfo?.realName}`,
      });
    } else {
      destroyWatermark();
    }
  },
  {
    immediate: true,
  },
);
</script>

<template>
  <BasicLayout @clear-preferences-and-logout="handleLogout">
    <template #user-dropdown>
      <UserDropdown
        :avatar
        :menus
        :text="userStore.userInfo?.realName"
        description="ann.vben@gmail.com"
        tag-text="Pro"
        @logout="handleLogout"
        @clear-preferences-and-logout="handleLogout"
      />
    </template>
    <template #notification>
      <Notification
        :count="unread"
        :notifications="notifications"
        @clear="markAllRead"
        @make-all="markAllRead"
        @read="(item) => item.id !== undefined && markRead(item.id)"
        @remove="(item) => item.id !== undefined && markRead(item.id)"
        @on-click="handleClick"
        @view-all="viewAll"
      />
    </template>
    <template #extra>
      <AuthenticationLoginExpiredModal
        v-model:open="accessStore.loginExpired"
        :avatar
      >
        <LoginForm />
      </AuthenticationLoginExpiredModal>
    </template>
    <template #lock-screen>
      <LockScreen :avatar @to-login="handleLogout" />
    </template>
  </BasicLayout>
</template>
