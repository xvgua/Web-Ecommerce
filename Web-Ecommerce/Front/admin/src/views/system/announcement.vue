<template>
  <div class="announcement-manage">
    <h1 class="page-title">公告管理</h1>

    <div class="toolbar">
      <div class="toolbar-right">
        <el-button type="primary" @click="handleAdd">发布公告</el-button>
      </div>
    </div>

    <el-table :data="announcements" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="createTime" label="发布时间" width="170" :formatter="(_, __, val) => formatDate(val)" />
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
import { formatDate } from '@/utils/format'

interface Announcement {
  id: number
  title: string
  content: string
  createTime: string
}

const announcements = ref<Announcement[]>([])

function handleAdd() {
  // TODO: implement
}

function handleEdit(row: Announcement) {
  // TODO: implement
}

function handleDelete(id: number) {
  // TODO: implement
}
</script>
