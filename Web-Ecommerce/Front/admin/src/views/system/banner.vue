<template>
  <div class="banner-manage">
    <h1 class="page-title">轮播管理</h1>

    <div class="toolbar">
      <div class="toolbar-right">
        <el-button type="primary" @click="handleAdd">新增轮播</el-button>
      </div>
    </div>

    <el-table :data="banners" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="图片" width="120">
        <template #default="{ row }">
          <el-image :src="row.imageUrl" fit="cover" style="width: 80px; height: 40px; border-radius: 4px" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="150" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button text type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl: string
  sortOrder: number
}

const banners = ref<Banner[]>([])

function handleAdd() {
  // TODO: implement
}

function handleEdit(row: Banner) {
  // TODO: implement
}

function handleDelete(id: number) {
  // TODO: implement
}
</script>
