<template>
  <div class="category-manage">
    <h1 class="page-title">分类管理</h1>

    <div class="toolbar">
      <div class="toolbar-right">
        <el-button type="primary" @click="handleAdd">新增分类</el-button>
      </div>
    </div>

    <el-table :data="categories" v-loading="loading" border row-key="id">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="分类名称" min-width="150" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="450px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { ElMessageBox, ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/admin'
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
  sortOrder: 0,
})

const rules: FormRules = {
  name: [requiredRule('分类名称')],
}

async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategoryList()
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editId.value = 0
  form.name = ''
  form.sortOrder = 0
  dialogVisible.value = true
}

function handleEdit(row: Category) {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该分类吗？', '提示', { type: 'warning' })
  await deleteCategory(id)
  ElMessage.success('已删除')
  loadCategories()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editId.value, form)
      ElMessage.success('已更新')
    } else {
      await createCategory(form)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadCategories()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCategories()
})
</script>
