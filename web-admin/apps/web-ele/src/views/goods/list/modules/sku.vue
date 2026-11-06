<script lang="ts" setup>
import type { GoodsApi } from '#/api/goods/goods';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  ElButton,
  ElInput,
  ElInputNumber,
  ElMessage,
  ElMessageBox,
  ElSwitch,
  ElTable,
  ElTableColumn,
  ElTag,
} from 'element-plus';

import { getGoodsSku, saveGoodsSku } from '#/api/goods/goods';
import { GOODS_PERMISSIONS } from '#/constants/permissions';
import { usePermission } from '#/composables/use-permission';

const emit = defineEmits<{ success: [] }>();
const { can } = usePermission();
const canEdit = computed(() => can(GOODS_PERMISSIONS.goodsSku));

/** 抽屉里编辑的规格组：values 用数组便于增删 */
interface SpecGroupForm {
  name: string;
  values: string[];
}

interface SkuRow {
  id?: number;
  skuCode?: string;
  specText: string;
  price: number;
  originalPrice?: null | number;
  stock: number;
  status: number;
}

const goods = ref<GoodsApi.Goods | null>(null);
const specs = ref<SpecGroupForm[]>([]);
const skus = ref<SkuRow[]>([]);
const loading = ref(false);
const saving = ref(false);
/** 每个规格组正在输入的新规格值 */
const valueInputs = ref<string[]>([]);

const hasSpecs = computed(() =>
  specs.value.some((group) => group.name.trim() && group.values.length > 0),
);

/**
 * 按当前规格组展开全部组合，规格值用英文逗号拼接（与下单、购物车的 spec_text 口径一致）。
 */
const combinations = computed<string[]>(() => {
  const groups = specs.value.filter(
    (group) => group.name.trim() && group.values.length > 0,
  );
  if (groups.length === 0) {
    return [];
  }
  return groups.reduce<string[]>(
    (acc, group) => {
      const next: string[] = [];
      for (const prefix of acc) {
        for (const value of group.values) {
          next.push(prefix ? `${prefix},${value}` : value);
        }
      }
      return next;
    },
    [''],
  );
});

const totalStock = computed(() =>
  skus.value.reduce((sum, sku) => sum + Number(sku.stock || 0), 0),
);

function addGroup() {
  specs.value.push({ name: '', values: [] });
  valueInputs.value.push('');
}

function removeGroup(index: number) {
  specs.value.splice(index, 1);
  valueInputs.value.splice(index, 1);
  regenerate();
}

function addValue(index: number) {
  const text = (valueInputs.value[index] ?? '').trim();
  if (!text) {
    return;
  }
  if (text.includes(',')) {
    ElMessage.warning('规格值不能包含英文逗号');
    return;
  }
  const group = specs.value[index];
  if (!group) {
    return;
  }
  if (group.values.includes(text)) {
    ElMessage.warning(`规格值「${text}」已存在`);
    return;
  }
  group.values.push(text);
  valueInputs.value[index] = '';
  regenerate();
}

function removeValue(groupIndex: number, valueIndex: number) {
  specs.value[groupIndex]?.values.splice(valueIndex, 1);
  regenerate();
}

/**
 * 规格变化后重新生成 SKU 行：已有的规格组合保留价格/库存/编码，新增组合用商品现价兜底，
 * 已被删掉的组合直接从表格移除（保存时后端做逻辑删除）。
 */
function regenerate() {
  const previous = new Map(skus.value.map((sku) => [sku.specText, sku]));
  const fallbackPrice = Number(goods.value?.price ?? 0);
  skus.value = combinations.value.map((specText) => {
    const existed = previous.get(specText);
    if (existed) {
      return existed;
    }
    return {
      specText,
      price: fallbackPrice,
      originalPrice: goods.value?.originalPrice ?? null,
      stock: 0,
      status: 1,
    };
  });
}

async function load() {
  if (!goods.value?.id) {
    return;
  }
  loading.value = true;
  try {
    const config = await getGoodsSku(goods.value.id);
    specs.value = parseSpecs(config.specs);
    valueInputs.value = specs.value.map(() => '');
    skus.value = (config.skus ?? []).map((sku) => ({
      id: sku.id,
      skuCode: sku.skuCode,
      specText: sku.specText,
      price: Number(sku.price ?? 0),
      originalPrice: sku.originalPrice ?? null,
      stock: Number(sku.stock ?? 0),
      status: sku.status ?? 1,
    }));
  } finally {
    loading.value = false;
  }
}

function parseSpecs(raw: null | string | undefined): SpecGroupForm[] {
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.map((group: any) => ({
      name: String(group?.name ?? ''),
      values: Array.isArray(group?.values)
        ? group.values.map((value: any) => String(value))
        : [],
    }));
  } catch {
    return [];
  }
}

function validate(): string | null {
  for (const group of specs.value) {
    if (!group.name.trim()) {
      return '规格名称不能为空';
    }
    if (group.values.length === 0) {
      return `规格「${group.name}」至少需要一个规格值`;
    }
  }
  const names = specs.value.map((group) => group.name.trim());
  if (new Set(names).size !== names.length) {
    return '规格名称不能重复';
  }
  if (skus.value.length === 0) {
    return '请先生成规格组合';
  }
  for (const sku of skus.value) {
    if (!(Number(sku.price) >= 0)) {
      return `SKU「${sku.specText}」的销售价不合法`;
    }
    if (!(Number(sku.stock) >= 0)) {
      return `SKU「${sku.specText}」的库存不合法`;
    }
  }
  return null;
}

async function onSave() {
  if (!canEdit.value) {
    return;
  }
  const error = validate();
  if (error) {
    ElMessage.warning(error);
    return;
  }
  if (!hasSpecs.value) {
    ElMessage.warning('请至少配置一个规格组');
    return;
  }
  saving.value = true;
  try {
    await saveGoodsSku(goods.value!.id, {
      specs: JSON.stringify(
        specs.value.map((group) => ({
          name: group.name.trim(),
          values: group.values,
        })),
      ),
      skus: skus.value.map((sku) => ({
        id: sku.id,
        price: Number(sku.price),
        originalPrice:
          sku.originalPrice === null || sku.originalPrice === undefined
            ? null
            : Number(sku.originalPrice),
        specText: sku.specText,
        status: sku.status,
        stock: Number(sku.stock),
      })),
    });
    ElMessage.success('规格已保存');
    emit('success');
    drawerApi.close();
  } finally {
    saving.value = false;
  }
}

/** 改回单规格：清空规格定义与 SKU，商品价格/库存沿用商品表 */
async function toSingleSpec() {
  if (!canEdit.value) {
    return;
  }
  await ElMessageBox.confirm(
    '改为单规格会删除该商品的全部 SKU，已有订单不受影响，确定继续吗？',
    '改为单规格',
    { type: 'warning' },
  );
  saving.value = true;
  try {
    await saveGoodsSku(goods.value!.id, { specs: null, skus: [] });
    ElMessage.success('已改为单规格');
    emit('success');
    drawerApi.close();
  } finally {
    saving.value = false;
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  footer: true,
  onConfirm: onSave,
  onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      return;
    }
    goods.value = drawerApi.getData<GoodsApi.Goods>();
    specs.value = [];
    skus.value = [];
    valueInputs.value = [];
    load();
  },
});
</script>

<template>
  <Drawer :title="`规格库存 - ${goods?.name ?? ''}`" class="w-[900px]">
    <div class="sku-config">
      <div class="sku-config__tip">
        配置规格后，商品价格与库存以 SKU 为准；商品列表展示最低价与库存合计。
      </div>

      <div v-for="(group, gi) in specs" :key="gi" class="spec-group">
        <div class="spec-group__head">
          <ElInput
            v-model="group.name"
            class="w-[200px]"
            :disabled="!canEdit"
            placeholder="规格名称，如 颜色"
            @change="regenerate"
          />
          <ElButton
            v-access:code="GOODS_PERMISSIONS.goodsSku"
            link
            type="danger"
            @click="removeGroup(gi)"
          >
            删除该规格
          </ElButton>
        </div>
        <div class="spec-group__values">
          <ElTag
            v-for="(value, vi) in group.values"
            :key="value"
            :closable="canEdit"
            class="mr-2"
            @close="removeValue(gi, vi)"
          >
            {{ value }}
          </ElTag>
          <div class="flex items-center gap-2">
            <ElInput
              v-model="valueInputs[gi]"
              class="w-[160px]"
              :disabled="!canEdit"
              placeholder="规格值，回车添加"
              @keyup.enter="addValue(gi)"
            />
            <ElButton
              v-access:code="GOODS_PERMISSIONS.goodsSku"
              @click="addValue(gi)"
            >
              添加
            </ElButton>
          </div>
        </div>
      </div>

      <div class="flex gap-2">
        <ElButton
          v-access:code="GOODS_PERMISSIONS.goodsSku"
          :disabled="specs.length >= 3"
          @click="addGroup"
        >
          添加规格组
        </ElButton>
        <ElButton
          v-access:code="GOODS_PERMISSIONS.goodsSku"
          :disabled="!hasSpecs"
          @click="regenerate"
        >
          生成规格组合
        </ElButton>
        <ElButton
          v-if="skus.length > 0"
          v-access:code="GOODS_PERMISSIONS.goodsSku"
          type="danger"
          plain
          @click="toSingleSpec"
        >
          改为单规格
        </ElButton>
      </div>

      <div v-if="skus.length > 0" class="sku-table">
        <div class="sku-table__head">
          <span>共 {{ skus.length }} 个 SKU，库存合计 {{ totalStock }}</span>
        </div>
        <ElTable :data="skus" border size="small">
          <ElTableColumn label="规格值" min-width="180" prop="specText" />
          <ElTableColumn label="SKU 编码" min-width="160">
            <template #default="{ row }">
              <span class="text-gray-400">{{ row.skuCode ?? '保存后生成' }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="销售价" width="140">
            <template #default="{ row }">
              <ElInputNumber
                v-model="row.price"
                :disabled="!canEdit"
                :min="0"
                :precision="2"
              />
            </template>
          </ElTableColumn>
          <ElTableColumn label="划线价" width="140">
            <template #default="{ row }">
              <ElInputNumber
                v-model="row.originalPrice"
                :disabled="!canEdit"
                :min="0"
                :precision="2"
              />
            </template>
          </ElTableColumn>
          <ElTableColumn label="库存" width="140">
            <template #default="{ row }">
              <ElInputNumber v-model="row.stock" :disabled="!canEdit" :min="0" />
            </template>
          </ElTableColumn>
          <ElTableColumn label="启用" width="100">
            <template #default="{ row }">
              <ElSwitch
                v-model="row.status"
                :active-value="1"
                :disabled="!canEdit"
                :inactive-value="0"
              />
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
      <div v-else-if="!loading" class="sku-config__empty">
        还没有规格，先添加规格组并录入规格值，再生成规格组合。
      </div>
    </div>
    <template #footer>
      <ElButton @click="drawerApi.close()">取消</ElButton>
      <ElButton
        v-access:code="GOODS_PERMISSIONS.goodsSku"
        :loading="saving"
        type="primary"
        @click="onSave"
      >
        保存
      </ElButton>
    </template>
  </Drawer>
</template>

<style scoped>
.sku-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sku-config__tip {
  padding: 8px 12px;
  font-size: 12px;
  color: #909399;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.sku-config__empty {
  padding: 24px 0;
  font-size: 13px;
  color: #bbb;
  text-align: center;
}

.spec-group {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.spec-group__head {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.spec-group__values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.sku-table {
  margin-top: 8px;
}

.sku-table__head {
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
