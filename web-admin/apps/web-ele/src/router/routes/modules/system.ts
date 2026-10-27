import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'lucide:settings',
      order: 90,
      title: $t('system.title'),
    },
    name: 'System',
    path: '/system',
    children: [
      {
        component: () => import('#/views/system/user/list.vue'),
        meta: {
          icon: 'lucide:users',
          title: $t('system.user.name'),
        },
        name: 'SystemUser',
        path: '/system/user',
      },
      {
        component: () => import('#/views/system/role/list.vue'),
        meta: {
          icon: 'lucide:shield-check',
          title: $t('system.role.name'),
        },
        name: 'SystemRole',
        path: '/system/role',
      },
      {
        component: () => import('#/views/system/menu/list.vue'),
        meta: {
          icon: 'lucide:menu',
          title: $t('system.menu.title'),
        },
        name: 'SystemMenu',
        path: '/system/menu',
      },
      {
        component: () => import('#/views/system/dept/list.vue'),
        meta: {
          icon: 'lucide:building-2',
          title: $t('system.dept.name'),
        },
        name: 'SystemDept',
        path: '/system/dept',
      },
    ],
  },
];

export default routes;
