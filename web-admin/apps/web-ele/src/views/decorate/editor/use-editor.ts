import type {
  CanvasComponent,
  PageConfig,
  PageSchema,
} from '../widgets/types';

import { computed, onBeforeUnmount, ref } from 'vue';

import { ElMessage } from 'element-plus';

import {
  getDecoratePage,
  publishDecoratePage,
  saveDecoratePage,
} from '#/api/decorate/page';

import { createComponent, createEmptySchema, normalizeSchema } from '../widgets';
import { WIDGETS } from '../widgets';

interface DragState {
  fromIndex?: number;
  kind: 'move' | 'new';
  type?: string;
}

const HISTORY_LIMIT = 60;
const SAVE_DELAY = 800;

/**
 * 装修编辑器状态机：
 * 所有变更都走 commit()（快照入栈 → 变更 → 防抖保存），保证撤销/重做与落库一致。
 */
export function useDecorateEditor(pageId: () => number) {
  const schema = ref<PageSchema>(createEmptySchema());
  const pageName = ref('');
  const pageMeta = ref<any>(null);
  const selectedId = ref<null | string>(null);
  const past = ref<string[]>([]);
  const future = ref<string[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const dirty = ref(false);
  const dragState = ref<DragState | null>(null);

  let saveTimer: any;
  let lastHistoryAt = 0;

  const components = computed(() => schema.value.components);
  const selected = computed<CanvasComponent | undefined>(() =>
    schema.value.components.find((item) => item.id === selectedId.value),
  );
  const canUndo = computed(() => past.value.length > 0);
  const canRedo = computed(() => future.value.length > 0);

  const snapshot = () => JSON.stringify(schema.value);

  function pushHistory(coalesce = false) {
    const now = Date.now();
    // 连续微调（例如拖动数字/输入文字）在 600ms 内合并为一条历史，避免撤销粒度太碎
    if (coalesce && now - lastHistoryAt < 600) {
      return;
    }
    past.value.push(snapshot());
    if (past.value.length > HISTORY_LIMIT) {
      past.value.shift();
    }
    future.value = [];
    lastHistoryAt = now;
  }

  function commit(mutator: () => void, coalesce = false) {
    pushHistory(coalesce);
    mutator();
    scheduleSave();
  }

  function scheduleSave() {
    dirty.value = true;
    clearTimeout(saveTimer);
    saveTimer = setTimeout(() => {
      void save();
    }, SAVE_DELAY);
  }

  async function save(silent = true) {
    clearTimeout(saveTimer);
    if (!dirty.value || !pageId()) {
      return;
    }
    saving.value = true;
    try {
      const result = await saveDecoratePage(pageId(), {
        name: pageName.value,
        schemaJson: JSON.stringify(schema.value),
        version: pageMeta.value?.version,
      });
      pageMeta.value = { ...pageMeta.value, ...result };
      dirty.value = false;
      if (!silent) {
        ElMessage.success('草稿已保存');
      }
    } catch (error: any) {
      ElMessage.error(error?.message || '保存失败，请稍后重试');
      throw error;
    } finally {
      saving.value = false;
    }
  }

  async function load() {
    if (!pageId()) {
      return;
    }
    loading.value = true;
    try {
      const page = await getDecoratePage(pageId());
      pageMeta.value = page;
      pageName.value = page.name;
      schema.value = normalizeSchema(
        page.schemaJson ? safeParse(page.schemaJson) : null,
        page.name,
      );
      selectedId.value = null;
      past.value = [];
      future.value = [];
      dirty.value = false;
    } finally {
      loading.value = false;
    }
  }

  function undo() {
    const previous = past.value.pop();
    if (previous === undefined) {
      return;
    }
    future.value.push(snapshot());
    schema.value = JSON.parse(previous);
    ensureSelectionValid();
    scheduleSave();
  }

  function redo() {
    const next = future.value.pop();
    if (next === undefined) {
      return;
    }
    past.value.push(snapshot());
    schema.value = JSON.parse(next);
    ensureSelectionValid();
    scheduleSave();
  }

  function ensureSelectionValid() {
    if (
      selectedId.value &&
      !schema.value.components.some((item) => item.id === selectedId.value)
    ) {
      selectedId.value = null;
    }
  }

  function select(id: null | string) {
    selectedId.value = id;
  }

  /** 插入组件：index 为空时追加到末尾 */
  function addWidget(type: string, index?: number) {
    const component = createComponent(type);
    if (!component) {
      return;
    }
    commit(() => {
      const at = index === undefined ? schema.value.components.length : index;
      schema.value.components.splice(at, 0, component);
    });
    selectedId.value = component.id;
  }

  function moveWidget(from: number, to: number) {
    if (from === to || from < 0 || to < 0) {
      return;
    }
    commit(() => {
      const list = schema.value.components;
      const [item] = list.splice(from, 1);
      if (!item) {
        return;
      }
      const target = from < to ? to - 1 : to;
      list.splice(target, 0, item);
    });
  }

  function removeWidget(id: string) {
    const index = schema.value.components.findIndex((item) => item.id === id);
    if (index < 0) {
      return;
    }
    commit(() => {
      schema.value.components.splice(index, 1);
    });
    if (selectedId.value === id) {
      selectedId.value = null;
    }
  }

  function duplicateWidget(id: string) {
    const index = schema.value.components.findIndex((item) => item.id === id);
    if (index < 0) {
      return;
    }
    const source = schema.value.components[index]!;
    const copy: CanvasComponent = {
      id: `c_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 5)}`,
      props: JSON.parse(JSON.stringify(source.props)),
      type: source.type,
      visible: source.visible,
    };
    commit(() => {
      schema.value.components.splice(index + 1, 0, copy);
    });
    selectedId.value = copy.id;
  }

  function toggleVisible(id: string) {
    const target = schema.value.components.find((item) => item.id === id);
    if (!target) {
      return;
    }
    commit(() => {
      target.visible = !target.visible;
    });
  }

  function moveBy(id: string, offset: number) {
    const index = schema.value.components.findIndex((item) => item.id === id);
    const target = index + offset;
    if (index < 0 || target < 0 || target >= schema.value.components.length) {
      return;
    }
    commit(() => {
      const list = schema.value.components;
      const [item] = list.splice(index, 1);
      list.splice(target, 0, item!);
    });
  }

  function updateProps(id: string, patch: Record<string, any>) {
    const target = schema.value.components.find((item) => item.id === id);
    if (!target) {
      return;
    }
    commit(() => {
      Object.assign(target.props, patch);
    }, true);
  }

  function updatePageConfig(patch: Partial<PageConfig>) {
    commit(() => {
      Object.assign(schema.value.page, patch);
    }, true);
  }

  function rename(name: string) {
    pageName.value = name;
    commit(() => {
      schema.value.page.title = name;
    }, true);
  }

  /** 拖入：从左侧组件库或画布内已有组件 */
  function startDrag(payload: DragState) {
    dragState.value = payload;
  }

  function endDrag() {
    dragState.value = null;
  }

  /** 放置到指定插入位 */
  function dropAt(index: number) {
    const drag = dragState.value;
    dragState.value = null;
    if (!drag) {
      return;
    }
    if (drag.kind === 'new' && drag.type) {
      addWidget(drag.type, index);
      return;
    }
    if (drag.kind === 'move' && drag.fromIndex !== undefined) {
      moveWidget(drag.fromIndex, index);
    }
  }

  async function publish() {
    await save(true);
    const page = await publishDecoratePage(pageId());
    pageMeta.value = { ...pageMeta.value, ...page };
    ElMessage.success('发布成功，已同步为最新线上版本');
  }

  function handleKeydown(event: KeyboardEvent) {
    const target = event.target as HTMLElement | null;
    const editing =
      target &&
      (target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.isContentEditable);
    if (editing) {
      return;
    }
    const meta = event.metaKey || event.ctrlKey;
    if (meta && event.key.toLowerCase() === 'z') {
      event.preventDefault();
      if (event.shiftKey) {
        redo();
      } else {
        undo();
      }
    } else if (meta && event.key.toLowerCase() === 'y') {
      event.preventDefault();
      redo();
    } else if (meta && event.key.toLowerCase() === 's') {
      event.preventDefault();
      void save(false);
    } else if (
      (event.key === 'Delete' || event.key === 'Backspace') &&
      selectedId.value
    ) {
      event.preventDefault();
      removeWidget(selectedId.value);
    }
  }

  window.addEventListener('keydown', handleKeydown);
  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
    clearTimeout(saveTimer);
  });

  return {
    canRedo,
    canUndo,
    components,
    dirty,
    dragState,
    dropAt,
    duplicateWidget,
    endDrag,
    loading,
    moveBy,
    moveWidget,
    pageMeta,
    pageName,
    publish,
    redo,
    removeWidget,
    rename,
    save,
    saving,
    schema,
    select,
    selected,
    selectedId,
    startDrag,
    toggleVisible,
    undo,
    updatePageConfig,
    updateProps,
    addWidget,
    load,
    widgets: WIDGETS,
  };
}

function safeParse(json: string) {
  try {
    return JSON.parse(json);
  } catch {
    return null;
  }
}
