# Frontend — 技术规范与开发约定

> 关联文档：[ProjectNorms.md](../ProjectNorms.md)

## 一、技术边界

### 强制技术栈

| 类别     | 技术                    | 版本要求       |
| -------- | ----------------------- | -------------- |
| 框架     | Vue 3 (Composition API) | ≥3.4           |
| 构建     | Vite                    | ≥5.0           |
| UI 库    | Element Plus            | ≥2.6           |
| 状态管理 | Pinia                   | ≥2.1           |
| 路由     | Vue Router              | ≥4.3           |
| HTTP     | Axios                   | ≥1.7           |
| 语言     | TypeScript              | 推荐，不做强制 |

### 前端不负责

- 数据库直连、ORM、SQL 拼接 — 全部走 HTTP API
- 文件存储（图片上传至后端统一处理，前端仅做预览）
- 支付安全逻辑（仅调用后端支付接口，不本地计算金额）
- 权限裁定（仅根据后端返回的角色/权限码做 UI 展示/隐藏）

---

## 二、工程目录结构

```
Front/
├── client/                        # 客户端（消费者端）
│   ├── src/
│   │   ├── api/                   # Axios 请求封装，按业务域拆分
│   │   ├── assets/                # 静态资源（图片、图标、全局样式）
│   │   ├── components/            # 公共组件（可复用）
│   │   │   ├── common/            #   通用组件（Pagination、Upload、Empty…）
│   │   │   └── business/          #   业务组件（ProductCard、OrderItem…）
│   │   ├── composables/           # 组合式函数（useAuth, useCart, usePagination）
│   │   ├── layouts/               # 布局组件（DefaultLayout, BlankLayout）
│   │   ├── router/                # 路由配置
│   │   ├── stores/                # Pinia store，按业务域拆分
│   │   ├── utils/                 # 工具函数（formatPrice, formatDate, validators）
│   │   └── views/                 # 页面组件
│   │       ├── home/              #   首页
│   │       ├── product/           #   商品（列表、详情、搜索）
│   │       ├── cart/              #   购物车
│   │       ├── order/             #   订单（确认、支付、列表、详情）
│   │       ├── user/              #   用户（登录、注册、个人中心、地址）
│   │       └── ...
│   └── ...
│
├── admin/                         # 管理后台（管理员端）
│   ├── src/
│   │   ├── api/
│   │   ├── assets/
│   │   ├── components/
│   │   │   ├── common/            #   通用组件
│   │   │   └── business/          #   业务组件
│   │   ├── composables/
│   │   ├── layouts/
│   │   ├── router/
│   │   ├── stores/
│   │   ├── utils/
│   │   └── views/
│   │       ├── dashboard/         #   数据看板
│   │       ├── product/           #   商品管理（分类、列表、新增/编辑）
│   │       ├── order/             #   订单管理（列表、详情）
│   │       ├── user/              #   用户管理（列表、详情）
│   │       ├── system/            #   系统管理（轮播、公告、反馈）
│   │       └── auth/              #   管理员登录
│   └── ...
│
└── shared/                        # 共用代码（类型定义、常量、校验规则）
    ├── types/                     # TypeScript 接口定义
    ├── constants/                 # 枚举、业务常量
    └── validators/                # 通用校验规则
```

### 目录约定

- 每个页面模块自成一个子目录，index.vue 为主入口
- `components/common/`：与业务无关的纯 UI 封装（二次封装 Element Plus 组件）
- `components/business/`：跨页面复用的业务组件
- `composables/`：不含模板、仅逻辑的组合函数
- `stores/`：一个文件一个业务域，不混放

---

## 三、API 通信规范

### 3.1 Axios 实例配置

- baseURL 统一从环境变量读取（`.env.development` / `.env.production`）
- 响应拦截器统一处理：
  - `code !== 200` → 弹出错误提示（ElMessage）
  - `code === 401` → 清除 token，跳转登录页
- 请求拦截器自动附 token（JWT / Session，从 Pinia 或 cookie 读）

### 3.2 接口格式约定

后端统一响应格式：

```ts
interface ApiResponse<T> {
  code: number; // 200 = 成功
  message: string; // 提示信息
  data: T; // 业务数据
}

interface PageResponse<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
}
```

### 3.3 API 函数命名

```ts
// api/product.ts
export function getProductList(params: ProductQuery); // GET  列表
export function getProductById(id: number); // GET  详情
export function createProduct(data: ProductForm); // POST 新增
export function updateProduct(id: number, data: ProductForm); // PUT 更新
export function deleteProduct(id: number); // DELETE 删除
```

### 3.4 请求错误处理

- 网络异常统一由拦截器 toast 提示
- 业务异常由调用方根据 `code` 自行处理
- 敏感操作（下单、支付、删除）需加 loading 锁，防止重复提交

---

## 四、组件设计规范

### 4.1 组件职责

- **View**（`views/`）：路由级别页面，负责组装子组件、发请求、传 props，自身不宜过于臃肿
- **Business**（`components/business/`）：承载具体业务的子组件，通过 props 接收数据、emit 抛出事件
- **Common**（`components/common/`）：无业务语义，通过 slots 和 props 暴露通用能力

### 4.2 Composition API 使用原则

- `<script setup lang="ts">` 作为默认写法
- 复杂逻辑抽入 `composables/`，不在组件内写超过 50 行的逻辑函数
- `defineProps` / `defineEmits` 必须显式声明类型

### 4.3 Element Plus 使用

- 全局注册（推荐）或按需引入，统一不混用
- 表单统一用 `el-form` + `el-form-item`，校验规则定义在 `shared/validators/`
- 表格统一用 `el-table` + `el-pagination`，分页参数结构统一
- 消息提示统一用 `ElMessage`（成功/失败/警告），对话框用 `ElMessageBox`
- 不在 Element Plus 原生组件上写内联 style，统一用 class 或 scoped style 覆盖

---

## 五、状态管理（Pinia）

### 5.1 Store 拆分原则

```ts
stores/
├── user.ts       // 用户信息、token、登录状态
├── cart.ts       // 购物车列表、选中状态
├── product.ts    // （可选）商品浏览状态、搜索历史
└── order.ts      // （可选）订单筛选条件暂存
```

### 5.2 使用约束

- 仅全局共享的状态才放入 store（用户信息、购物车、权限标识）
- 页面局部状态放组件自身 `ref` / `reactive`，不提升到 store
- 不直接从 store 修改数据，统一通过 store 提供的 action/方法修改
- store 内不做 HTTP 请求，HTTP 在 api 层完成，store 仅存结果

---

## 六、路由与权限

### 6.1 路由组织

- 客户端路由：公开页面（首页、商品）+ 需登录页面（购物车、订单、个人中心）
- 管理后台路由：全部需管理员登录

### 6.2 路由守卫

- `beforeEach` 检查 token / 登录状态
- 未登录访问需要授权的页面 → 重定向登录页
- 已登录访问登录页 → 重定向首页
- 管理员后台额外检查角色权限

### 6.3 权限控制

- 页面级：路由 meta 标记角色，守卫中校验
- 按钮级：后端返回权限列表，`v-if` / `v-permission` 指令控制显隐

---

## 七、UI / UX 硬性要求

### 7.1 必须遵守

- **操作反馈**：所有按钮点击（新增、删除、提交）必须有 loading 态 + 完成后的成功/失败提示
- **空状态**：列表无数据时用 `el-empty`，不能展示空白区域
- **异常兜底**：图片加载失败 → 默认占位图；接口超时 → 重试按钮
- **表单校验**：所有提交类表单必须有前端校验（必填、格式、长度），不可仅依赖后端
- **确认弹窗**：删除、取消、退款等不可逆操作必须 `ElMessageBox.confirm` 二次确认
- **响应式**：客户端页面对 768px / 1024px / 1440px 三个断点做布局适配，管理后台可仅适配 ≥1280px

### 7.2 客户端特殊性

- 首屏（首页、商品列表）优先保证加载速度
- 商品图使用缩略图 + 原图懒加载
- 购物车数据与后端实时同步（加入/修改/删除后立即刷新）

### 7.3 管理后台特殊性

- 表格数据 >20 条必须分页
- 批量操作前必须已选中项目，未选中时按钮置灰
- 数据看板优先用 `el-card` + `el-statistic` 布局，图表可用 ECharts

---

## 八、图片处理规范

- 上传前做客户端压缩（>2MB 时），使用 `canvas` 或第三方压缩库
- 上传需展示进度条（`el-upload` + `on-progress`）
- 图片预览使用 `el-image`（自带预览和加载失败处理）
- 商品主图与详情图分开处理（主图限 1 张，详情图允许多张）

---

## 九、数据分页规范

- 统一分页参数命名：`page`（当前页，从 1 开始）、`pageSize`（每页条数，默认 20）
- 分页组件统一使用 `el-pagination`，布局为：`total, sizes, prev, pager, next, jumper`
- 搜索条件变更时重置到第 1 页
- 管理后台列表默认 `pageSize` 可设为 10（行高较大时）

---

## 十、命名约定速查

| 类型            | 规则                    | 示例                   |
| --------------- | ----------------------- | ---------------------- |
| 页面组件目录    | `kebab-case`            | `product-detail/`      |
| 组件文件        | `PascalCase`            | `ProductCard.vue`      |
| Composables     | `camelCase`，`use` 前缀 | `useAuth.ts`           |
| Store 文件      | `camelCase`             | `userStore.ts`         |
| API 文件        | `camelCase`             | `product.ts`           |
| 工具函数        | `camelCase`             | `formatPrice.ts`       |
| CSS class       | `kebab-case` 或 BEM     | `.product-card__price` |
| TypeScript 接口 | `PascalCase`            | `Product`, `OrderItem` |

---

## 十一、验收检查清单

在提交前需逐项确认：

- [ ] 所有页面无 console 报错（warn 亦需检查）
- [ ] 客户端 768px / 1024px / 1440px 下布局正常
- [ ] 表单提交有校验，空数据有 `el-empty`
- [ ] 删除/取消有确认弹窗
- [ ] 按钮点击有 loading 反馈
- [ ] 接口异常有 toast 提示
- [ ] 图片有占位图和懒加载
- [ ] 分页参数结构统一
- [ ] token 过期自动跳转登录
- [ ] 无硬编码的 API 地址（走环境变量）
