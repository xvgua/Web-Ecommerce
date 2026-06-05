<template>
  <div class="category-manage">
    <div class="page-header">
      <h1 class="page-title">分类管理</h1>
      <el-button type="primary" @click="handleAddPrimary">新增一级分类</el-button>
    </div>

    <el-empty v-if="!loading && !primaryCategories.length" description="暂无分类，点击上方按钮创建" />

    <div v-else v-loading="loading" class="primary-list">
      <div
        v-for="(primary, pi) in primaryCategories"
        :key="primary.id"
        class="primary-card"
      >
        <div class="primary-card__header">
          <div class="primary-card__info">
            <span class="primary-card__name">{{ primary.name }}</span>
            <el-tag size="small" type="info">排序: {{ primary.sortOrder }}</el-tag>
            <el-tag size="small">{{ primary.children?.length || 0 }} 个子分类</el-tag>
          </div>
          <div class="primary-card__actions">
            <el-button
              text
              size="small"
              :loading="movingId === primary.id"
              :disabled="pi === 0"
              @click="handleMove(primary.id, 'up')"
            >
              <el-icon><Top /></el-icon>
            </el-button>
            <el-button
              text
              size="small"
              :loading="movingId === primary.id"
              :disabled="pi === primaryCategories.length - 1"
              @click="handleMove(primary.id, 'down')"
            >
              <el-icon><Bottom /></el-icon>
            </el-button>
            <el-button text size="small" type="primary" @click="handleAddChild(primary.id)">
              + 子分类
            </el-button>
            <el-button text size="small" type="primary" @click="handleEdit(primary)">
              编辑
            </el-button>
            <el-button text size="small" type="danger" @click="handleDelete(primary.id)">
              删除
            </el-button>
          </div>
        </div>

        <div class="primary-card__body">
          <el-table :data="primary.children" size="small" v-if="primary.children?.length">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button text size="small" type="primary" @click="handleEdit(row, primary.id)">
                  编辑
                </el-button>
                <el-button text size="small" type="danger" @click="handleDelete(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="primary-card__empty">暂无子分类</div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingCategory ? '编辑分类' : '新增分类'"
      width="480px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="30" show-word-limit placeholder="分类名" />
        </el-form-item>
        <el-form-item label="上级分类" v-if="!editingCategory">
          <el-select v-model="form.parentId" placeholder="留空为一级分类" clearable>
            <el-option
              v-for="cat in primaryCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Top, Bottom } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getCategoryList,
  createCategory,
  updateCategory,
  deleteCategory,
  moveCategory,
} from '@/api/admin'
import type { Category } from '@shared/types/product'

const primaryCategories = ref<(Category & { children?: Category[] })[]>([])
const loading = ref(false)
const movingId = ref<number | null>(null)

const dialogVisible = ref(false)
const editingCategory = ref<Category | null>(null)
const defaultParentId = ref<number | undefined>(undefined)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  parentId: undefined as number | undefined,
  sortOrder: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategoryList()
    primaryCategories.value = res.data || []
  } finally {
    loading.value = false
  }
}

function handleAddPrimary() {
  editingCategory.value = null
  defaultParentId.value = undefined
  form.name = ''
  form.parentId = undefined
  form.sortOrder = 0
  dialogVisible.value = true
}

function handleAddChild(parentId: number) {
  editingCategory.value = null
  defaultParentId.value = parentId
  form.name = ''
  form.parentId = parentId
  form.sortOrder = 0
  dialogVisible.value = true
}

function handleEdit(cat: Category, parentId?: number) {
  editingCategory.value = cat
  defaultParentId.value = parentId
  form.name = cat.name
  form.parentId = parentId
  form.sortOrder = cat.sortOrder
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingCategory.value) {
      await updateCategory(editingCategory.value.id, {
        ...editingCategory.value,
        name: form.name,
        sortOrder: form.sortOrder,
        parentId: form.parentId,
      })
      ElMessage.success('已更新')
    } else {
      await createCategory({
        name: form.name,
        parentId: form.parentId,
        sortOrder: form.sortOrder,
      })
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadCategories()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定删除该分类？删除后子分类也会移除。', '提示', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('已删除')
  loadCategories()
}

async function handleMove(id: number, direction: 'up' | 'down') {
  movingId.value = id
  try {
    await moveCategory(id, direction)
    loadCategories()
  } catch {
    // handled
  } finally {
    movingId.value = null
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style lang="scss" scoped>
.category-manage {
  max-width: 960px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;

  .page-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0;
    color: var(--org-text);
    letter-spacing: -.4px;
  }
}

.primary-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.primary-card {
  background: var(--org-surface);
  border-radius: var(--org-radius-lg);
  border: 1px solid var(--org-border);
  box-shadow: var(--org-shadow-sm);
  overflow: hidden;
  transition: all var(--org-duration) var(--org-ease-soft);

  &:hover {
    box-shadow: var(--org-shadow-md);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #f9f7f4;
    border-bottom: 1px solid var(--org-border-soft);
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__name {
    font-size: 15px;
    font-weight: 700;
    color: var(--org-text);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__body {
    padding: 16px 24px 20px;
  }

  &__empty {
    padding: 32px 20px;
    text-align: center;
    color: var(--org-text-muted);
    font-size: 13px;
    font-weight: 500;
  }
}
</style>
