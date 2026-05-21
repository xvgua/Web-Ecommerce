import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'AdminLogin',
    component: () => import('@/views/auth/login.vue'),
    meta: { title: '管理员登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板' },
      },
      {
        path: 'products',
        name: 'ProductManage',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品管理' },
      },
      {
        path: 'products/create',
        name: 'ProductCreate',
        component: () => import('@/views/product/form.vue'),
        meta: { title: '新增商品' },
      },
      {
        path: 'products/:id/edit',
        name: 'ProductEdit',
        component: () => import('@/views/product/form.vue'),
        meta: { title: '编辑商品' },
      },
      {
        path: 'categories',
        name: 'CategoryManage',
        component: () => import('@/views/product/category.vue'),
        meta: { title: '分类管理' },
      },
      {
        path: 'orders',
        name: 'OrderManage',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理' },
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: '订单详情' },
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/banners',
        name: 'BannerManage',
        component: () => import('@/views/system/banner.vue'),
        meta: { title: '轮播管理' },
      },
      {
        path: 'system/announcements',
        name: 'AnnouncementManage',
        component: () => import('@/views/system/announcement.vue'),
        meta: { title: '公告管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
