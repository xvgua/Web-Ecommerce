import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品列表' },
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: () => import('@/views/product/detail.vue'),
        meta: { title: '商品详情' },
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/cart/index.vue'),
        meta: { title: '购物车', requiresAuth: true },
      },
      {
        path: 'order/confirm',
        name: 'OrderConfirm',
        component: () => import('@/views/order/confirm.vue'),
        meta: { title: '确认订单', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/order/list.vue'),
        meta: { title: '我的订单', requiresAuth: true },
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: '订单详情', requiresAuth: true },
      },
      {
        path: 'orders/:id/pay',
        name: 'OrderPay',
        component: () => import('@/views/order/pay.vue'),
        meta: { title: '订单支付', requiresAuth: true },
      },
      {
        path: 'orders/:orderId/review/:productId',
        name: 'OrderReview',
        component: () => import('@/views/order/review.vue'),
        meta: { title: '发表评价', requiresAuth: true },
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('@/views/user/profile.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'user/address',
        name: 'UserAddress',
        component: () => import('@/views/user/address.vue'),
        meta: { title: '收货地址', requiresAuth: true },
      },
    ],
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/user/login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/user/register.vue'),
    meta: { title: '注册' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
