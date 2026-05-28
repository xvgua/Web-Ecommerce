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
              :disabled="pi === 0"
              @click="handleMove(primary.id, 'up')"
            >
              <el-icon><Top /></el-icon>
            </el-button>
            <el-button
              text
              size="small"
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

        <div v-if="primary.children?.length" class="primary-card__body">
          <el-table :data="primary.children" size="small" border>
            <el-table-column prop="name" label="子分类名称" min-width="150" />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row, $index }">
                <el-button
                  text
                  size="small"
                  :disabled="$index === 0"
                  @click="handleMove(row.id, 'up')"
                >
                  <el-icon><Top /></el-icon>
                </el-button>
                <el-button
                  text
                  size="small"
                  :disabled="$index === (primary.children?.length || 0) - 1"
                  @click="handleMove(row.id, 'down')"
                >
                  <el-icon><Bottom /></el-icon>
                </el-button>
                <el-button text size="small" type="primary" @click="handleEdit(row)">
                  编辑
                </el-button>
                <el-button text size="small" type="danger" @click="handleDelete(row.id)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="primary-card__empty">
          暂无子分类，点击「+ 子分类」添加
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="460px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" placeholder="无（作为一级分类）" clearable style="width:100%">
            <el-option
              v-for="p in primaryOptions"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Top, Bottom } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { getCategoryList, createCategory, updateCategory, deleteCategory, moveCategorySort } from '@/api/admin'
import { requiredRule } from '@shared/validators'
import type { Category } from '@shared/types/product'

const categories = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  parentId: undefined as number | undefined,
})

const rules: FormRules = {
  name: [requiredRule('分类名称')],
}

const primaryCategories = computed(() => {
  const map = new Map<number, Category[]>()
  for (const c of categories.value) {
    if (c.parentId) {
      const list = map.get(c.parentId) || []
      list.push(c)
      map.set(c.parentId, list)
    }
  }
  return categories.value
    .filter(c => !c.parentId)
    .map(c => ({ ...c, children: map.get(c.id) || [] }))
})

const primaryOptions = computed(() =>
  categories.value.filter(c => !c.parentId)
)

async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategoryList()
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.parentId = undefined
  editId.value = 0
  formRef.value?.resetFields()
}

function handleAddPrimary() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function handleAddChild(parentId: number) {
  isEdit.value = false
  resetForm()
  form.parentId = parentId
  dialogVisible.value = true
}

function handleEdit(row: Category) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.parentId = row.parentId || undefined
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该分类吗？', '提示', { type: 'warning' })
  try {
    await deleteCategory(id)
    ElMessage.success('已删除')
    loadCategories()
  } catch { /* handled by interceptor */ }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: form.name,
      parentId: form.parentId || 0,
    }
    if (isEdit.value) {
      await updateCategory(editId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createCategory(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadCategories()
  } finally {
    submitting.value = false
  }
}

async function handleMove(id: number, direction: 'up' | 'down') {
  try {
    await moveCategorySort(id, direction)
    loadCategories()
  } catch { /* handled by interceptor */ }
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
  margin-bottom: 22px;

  .page-title {
    font-size: 20px;
    font-weight: 700;
    margin: 0;
  }
}

.primary-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.primary-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .05);
  overflow: hidden;

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 20px;
    background: #f8f9ff;
    border-bottom: 1px solid #eef1f8;
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: #2c3a5e;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  &__body {
    padding: 12px 20px 16px;
  }

  &__empty {
    padding: 28px 20px;
    text-align: center;
    color: #b0b8cc;
    font-size: 13px;
  }
}
</style>
