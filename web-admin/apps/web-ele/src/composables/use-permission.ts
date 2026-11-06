import { useAccess } from '@vben/access';

/**
 * 页面内权限判断。指令用于静态按钮，表格列 / 条件渲染使用 can()。
 */
export function usePermission() {
  const { hasAccessByCodes } = useAccess();

  function can(...codes: string[]) {
    return hasAccessByCodes(codes);
  }

  return { can };
}
