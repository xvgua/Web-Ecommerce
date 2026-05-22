# Backend — 技术规范文档

> 关联文档：[CLAUDE.md](./CLAUDE.md) | [ProjectNorms.md](../../ProjectNorms.md) | [Frontend CLAUDE.md](../Front/CLAUDE.md)

## 一、统一响应格式

```json
{ "code": 200, "message": "success", "data": {} }
```

| code | 含义 | 触发场景 |
|------|------|----------|
| 200 | 成功 | 正常返回 |
| 400 | 参数错误 | 校验失败、参数不合法 |
| 401 | 未认证 | Token 缺失/过期/无效 |
| 403 | 无权限 | 非管理员访问管理接口 |
| 404 | 不存在 | 资源未找到 |
| 500 | 服务错误 | 未捕获异常（GlobalExceptionHandler 兜底） |

分页响应 `data` 结构：

```json
{ "records": [], "total": 100, "page": 1, "pageSize": 20 }
```

RESTful 命名：

```
GET    /api/resource          → 分页列表（Query: page, pageSize, keyword, ...）
GET    /api/resource/:id      → 详情
POST   /api/resource          → 新增（Body: JSON）
PUT    /api/resource/:id      → 更新（Body: JSON）
DELETE /api/resource/:id      → 删除
PUT    /api/resource/:id/cancel|confirm|ship  → 特殊动作
```

---

## 二、用户端 API（19 个端点）

### 2.1 认证 — `/api/auth`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| POST | `/auth/register` | 无 | `{ username, password, confirmPassword, email, captcha }` | `null` |
| POST | `/auth/login` | 无 | `{ username, password, remember? }` | `{ token: string, user: User }` |

### 2.2 用户 — `/api/user`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/user/info` | 用户 | — | `User` |
| PUT | `/user/info` | 用户 | `Partial<User>` | `null` |
| GET | `/user/addresses` | 用户 | — | `Address[]` |
| POST | `/user/addresses` | 用户 | `AddressForm` | `Address` |
| PUT | `/user/addresses/:id` | 用户 | `AddressForm` | `null` |
| DELETE | `/user/addresses/:id` | 用户 | — | `null` |

### 2.3 商品 — `/api/products`

| 方法 | 路径 | 鉴权 | Query / Body | 响应 data |
|------|------|------|-------------|-----------|
| GET | `/products` | 无 | `?page, pageSize, keyword?, categoryId?, sort?, minPrice?, maxPrice?` | `PageResponse<Product>` |
| GET | `/products/:id` | 无 | — | `Product`（含 skus[]） |
| GET | `/products/hot` | 无 | `?limit` | `Product[]` |
| GET | `/products/new` | 无 | `?limit` | `Product[]` |

### 2.4 分类 — `/api/categories`

| 方法 | 路径 | 鉴权 | 响应 data |
|------|------|------|-----------|
| GET | `/categories` | 无 | `Category[]`（树形，含 children） |

### 2.5 购物车 — `/api/cart`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/cart` | 用户 | — | `CartItem[]` |
| POST | `/cart` | 用户 | `{ productId, skuId, quantity }` | `null` |
| PUT | `/cart/:id` | 用户 | `{ quantity?, checked? }` | `null` |
| DELETE | `/cart/:id` | 用户 | — | `null` |
| DELETE | `/cart/clear` | 用户 | — | `null` |

> 购物车存数据库（user_id 关联），`checked` 状态也持久化。同一用户重复添加同商品同 SKU 时执行 `INSERT ... ON DUPLICATE KEY UPDATE quantity = quantity + ?`，不产生重复行。

### 2.6 订单 — `/api/orders`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/orders` | 用户 | `?page, pageSize, status?` | `PageResponse<Order>` |
| GET | `/orders/:id` | 用户 | — | `Order`（含 items[] + address） |
| POST | `/orders` | 用户 | `{ addressId, cartItemIds: number[], remark? }` | `Order` |
| PUT | `/orders/:id/pay` | 用户 | `{ payMethod? }` | `null` |
| PUT | `/orders/:id/cancel` | 用户 | — | `null` |
| PUT | `/orders/:id/confirm` | 用户 | — | `null` |

> 创建订单事务内完成：生成订单号 → 创建 order + order_item → 扣减库存 → 清空购物车已选。支付接口为模拟支付，调用后订单状态 0→1。

### 2.7 文件上传 — `/api/admin/upload`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| POST | `/admin/upload` | 管理员 | `multipart/form-data` | `{ url: string }` |

> 仅允许 jpg/jpeg/png/gif/webp，≤2MB。文件名 UUID + 扩展名。存 `./upload/`。对应前端 `el-upload` action 属性 `/api/admin/upload`。

---

## 三、管理端 API（18 个端点）

### 3.1 认证 — `/api/admin/auth`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| POST | `/admin/auth/login` | 无 | `{ username, password }` | `{ token: string }` |

### 3.2 数据看板 — `/api/admin/dashboard`

| 方法 | 路径 | 鉴权 | 响应 data |
|------|------|------|-----------|
| GET | `/admin/dashboard/stats` | 管理员 | `{ totalUsers, totalOrders, totalSales, todayOrders, todaySales, pendingOrders, shippingOrders, completedOrders, cancelledOrders }` |

### 3.3 商品管理 — `/api/admin/products`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/admin/products` | 管理员 | `?page, pageSize, keyword?, categoryId?, status?` | `PageResponse<Product>` |
| GET | `/admin/products/:id` | 管理员 | — | `Product`（含 skus[]） |
| POST | `/admin/products` | 管理员 | `ProductForm` | `Product` |
| PUT | `/admin/products/:id` | 管理员 | `ProductForm` | `null` |
| DELETE | `/admin/products/:id` | 管理员 | — | `null` |

### 3.4 分类管理 — `/api/admin/categories`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/admin/categories` | 管理员 | — | `Category[]` |
| POST | `/admin/categories` | 管理员 | `{ name, parentId?, sortOrder? }` | `Category` |
| PUT | `/admin/categories/:id` | 管理员 | `{ name?, parentId?, sortOrder? }` | `null` |
| DELETE | `/admin/categories/:id` | 管理员 | — | `null` |

> 删除分类前校验：有子分类或关联商品时拒绝删除，返回 400 + "分类下有子分类/商品，无法删除"。

### 3.5 订单管理 — `/api/admin/orders`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/admin/orders` | 管理员 | `?page, pageSize, status?, orderNo?, userId?` | `PageResponse<Order>` |
| GET | `/admin/orders/:id` | 管理员 | — | `Order`（含 items[] + address） |
| PUT | `/admin/orders/:id/ship` | 管理员 | — | `null` |
| PUT | `/admin/orders/:id/cancel` | 管理员 | — | `null` |

### 3.6 用户管理 — `/api/admin/users`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/admin/users` | 管理员 | `?page, pageSize, keyword?` | `PageResponse<User>` |
| PUT | `/admin/users/:id/status` | 管理员 | `{ status }` | `null` |

### 3.7 轮播管理 — `/api/admin/banners`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/admin/banners` | 管理员 | `?page, pageSize` | `PageResponse<Banner>` |
| POST | `/admin/banners` | 管理员 | `{ title, imageUrl, linkUrl?, sortOrder? }` | `Banner` |
| PUT | `/admin/banners/:id` | 管理员 | `{ title?, imageUrl?, linkUrl?, sortOrder? }` | `null` |
| DELETE | `/admin/banners/:id` | 管理员 | — | `null` |

### 3.8 公告管理 — `/api/admin/announcements`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| GET | `/admin/announcements` | 管理员 | `?page, pageSize` | `PageResponse<Announcement>` |
| POST | `/admin/announcements` | 管理员 | `{ title, content }` | `Announcement` |
| PUT | `/admin/announcements/:id` | 管理员 | `{ title?, content? }` | `null` |
| DELETE | `/admin/announcements/:id` | 管理员 | — | `null` |

---

## 四、数据库设计

### 4.1 用户表 `user`

```sql
CREATE TABLE user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  email       VARCHAR(100) NOT NULL,
  nickname    VARCHAR(50)  DEFAULT '',
  avatar      VARCHAR(500) DEFAULT '',
  phone       VARCHAR(20)  DEFAULT '',
  status      TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.2 管理员表 `admin`

```sql
CREATE TABLE admin (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  role        VARCHAR(20)  DEFAULT 'ADMIN',
  status      TINYINT      DEFAULT 1,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
);
```

### 4.3 商品分类表 `category`

```sql
CREATE TABLE category (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL,
  parent_id   BIGINT       DEFAULT 0 COMMENT '0=一级分类',
  sort_order  INT          DEFAULT 0,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
);
```

### 4.4 商品表 `product`

```sql
CREATE TABLE product (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(200)   NOT NULL,
  category_id BIGINT         NOT NULL,
  price       DECIMAL(10,2)  NOT NULL,
  stock       INT            DEFAULT 0,
  description TEXT,
  main_image  VARCHAR(500),
  images      VARCHAR(2000)  DEFAULT '' COMMENT 'JSON array of URLs',
  status      TINYINT        DEFAULT 1 COMMENT '1=上架 0=下架',
  sales       INT            DEFAULT 0,
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.5 商品 SKU 表 `product_sku`

```sql
CREATE TABLE product_sku (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id  BIGINT         NOT NULL,
  spec_name   VARCHAR(50)    NOT NULL,
  spec_value  VARCHAR(50)    NOT NULL,
  price       DECIMAL(10,2)  NOT NULL,
  stock       INT            DEFAULT 0
);
```

### 4.6 购物车表 `cart`

```sql
CREATE TABLE cart (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT  NOT NULL,
  product_id  BIGINT  NOT NULL,
  sku_id      BIGINT  DEFAULT 0,
  quantity    INT     DEFAULT 1,
  checked     TINYINT DEFAULT 1 COMMENT '1=选中 0=未选',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product_sku (user_id, product_id, sku_id)
);
```

### 4.7 订单表 `order`

```sql
CREATE TABLE `order` (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no      VARCHAR(32)    NOT NULL UNIQUE,
  user_id       BIGINT         NOT NULL,
  address_id    BIGINT         NOT NULL,
  total_amount  DECIMAL(10,2)  NOT NULL,
  status        TINYINT        DEFAULT 0 COMMENT '0=待支付 1=待发货 2=待收货 3=已完成 4=已取消 5=退款中',
  remark        VARCHAR(500)   DEFAULT '' COMMENT '用户备注',
  pay_time      DATETIME       NULL,
  create_time   DATETIME       DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.8 订单明细表 `order_item`

```sql
CREATE TABLE order_item (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id      BIGINT         NOT NULL,
  product_id    BIGINT         NOT NULL,
  product_name  VARCHAR(200)   NOT NULL COMMENT '冗余，防商品被删后无法展示',
  product_image VARCHAR(500)   DEFAULT '',
  sku_id        BIGINT         DEFAULT 0,
  spec_desc     VARCHAR(100)   DEFAULT '',
  quantity      INT            NOT NULL,
  price         DECIMAL(10,2)  NOT NULL COMMENT '下单时单价'
);
```

### 4.9 收货地址表 `address`

```sql
CREATE TABLE address (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT       NOT NULL,
  name        VARCHAR(50)  NOT NULL,
  phone       VARCHAR(20)  NOT NULL,
  province    VARCHAR(50)  NOT NULL,
  city        VARCHAR(50)  NOT NULL,
  district    VARCHAR(50)  NOT NULL,
  detail      VARCHAR(200) NOT NULL,
  is_default  TINYINT      DEFAULT 0,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
);
```

### 4.10 商品评价表 `review`

```sql
CREATE TABLE review (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT         NOT NULL,
  username    VARCHAR(50)    NOT NULL COMMENT '冗余，防用户被删后无法展示',
  avatar      VARCHAR(500)   DEFAULT '',
  product_id  BIGINT         NOT NULL,
  order_id    BIGINT         NOT NULL,
  rating      TINYINT        NOT NULL COMMENT '1-5星',
  content     TEXT,
  images      VARCHAR(2000)  DEFAULT '' COMMENT 'JSON array',
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP
);
```

### 4.11 商品收藏表 `favorite`

```sql
CREATE TABLE favorite (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL,
  product_id  BIGINT   NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id)
);
```

### 4.12 轮播图表 `banner`

```sql
CREATE TABLE banner (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(100) NOT NULL,
  image_url   VARCHAR(500) NOT NULL,
  link_url    VARCHAR(500) DEFAULT '',
  sort_order  INT          DEFAULT 0,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
);
```

### 4.13 公告表 `announcement`

```sql
CREATE TABLE announcement (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  content     TEXT         NOT NULL,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 4.14 设计约束

- 主键 `BIGINT AUTO_INCREMENT`
- 金额 `DECIMAL(10,2)`，禁止浮点运算
- 状态字段 `TINYINT` + 代码常量（对齐前端枚举）
- `create_time` 默认 `CURRENT_TIMESTAMP`，`update_time` 加 `ON UPDATE CURRENT_TIMESTAMP`
- 唯一约束在数据库层定义（username, order_no, uk_user_product）
- 不设物理外键，关联在业务层维护

---

## 五、状态枚举定义

```java
public class OrderStatus {
    public static final int PENDING_PAY = 0;   // 待支付
    public static final int PENDING_SHIP = 1;  // 待发货
    public static final int SHIPPED = 2;       // 待收货
    public static final int COMPLETED = 3;     // 已完成
    public static final int CANCELLED = 4;     // 已取消
    public static final int REFUNDING = 5;     // 退款中
}

public class UserStatus {
    public static final int ACTIVE = 1;
    public static final int DISABLED = 0;
}

public class ProductStatus {
    public static final int ON_SALE = 1;
    public static final int OFF_SHELF = 0;
}
```

---

## 六、JWT 认证流程

```
客户端                          后端
  │  POST /api/auth/login        │
  │─────────────────────────────→│  BCrypt 验证密码
  │  { token, user }            │  签发 JWT (userId, role, 7天过期)
  │←─────────────────────────────│
  │  GET /api/cart               │
  │  Authorization: Bearer xxx   │
  │─────────────────────────────→│  LoginInterceptor → 验证 token
  │                              │  解析 userId → UserContext.set(userId)
  │                              │  放行
```

**安全约束：**
- 密码 BCrypt 加密，JWT 密钥从配置文件读取
- Token 过期 7 天，`remember=true` 延长至 30 天
- 管理员/用户两套 JWT（claim 中 `role` 区分），管理员接口接受 `role=ADMIN` 或 `role=SUPER_ADMIN`。当前阶段两者权限等价，预留 SUPER_ADMIN 用于后续角色分级
- 用户只能操作自己的数据（从 UserContext 取 userId）

---

## 七、订单状态机

```
                   ┌──────────┐
                   │ 待支付(0) │
                   └────┬─────┘
                        │ 模拟支付
                        ↓
                   ┌──────────┐
         ┌─────────│ 待发货(1) │
         │ 取消    └────┬─────┘
         ↓              │ 管理员发货
   ┌──────────┐         ↓
   │ 已取消(4) │    ┌──────────┐
   └──────────┘    │ 待收货(2) │
                   └────┬─────┘
                        │ 确认收货
                        ↓
                   ┌──────────┐
                   │ 已完成(3) │
                   └──────────┘

退款中(5)：从"已发货"或"已完成"发起，管理员处理后→已完成
```

规则：只有待支付可取消、只有待发货可发货、只有待收货可确认收货、取消后恢复库存。

---

## 八、配置模板

```yaml
# application-dev.yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 10MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: ${JWT_SECRET}
  expiration: 604800000  # 7天（毫秒）

upload:
  path: ./upload
  allowed-extensions: jpg,jpeg,png,gif,webp
  max-size: 2097152  # 2MB
```

---

## 九、开发顺序

| 阶段 | 内容 |
|------|------|
| 一、基础设施 | Spring Boot 项目 + pom.xml + 建表 + Result/异常/JWT/拦截器/跨域/分页 |
| 二、核心业务 | 用户注册登录 → 商品分类/列表/详情 → 购物车 CRUD → 订单（事务+库存）→ 地址 CRUD |
| 三、管理后台 | 管理员认证 → 数据看板 → 商品管理 → 分类管理 → 订单管理 → 用户管理 |
| 四、辅助功能 | 文件上传 → 轮播图 CRUD → 公告 CRUD → 评价 → 收藏 |
| 五、联调测试 | 前后端联调 → Postman/JUnit → 异常场景 → 数据一致性 |

---

## 十、模糊搜索规范

### 10.1 适用范围

以下接口通过 `keyword` 参数支持模糊搜索：

| 接口 | 搜索字段 | 匹配方式 |
|------|---------|----------|
| `GET /api/products` | `product.name` | 前后模糊 |
| `GET /api/admin/products` | `product.name` | 前后模糊 |
| `GET /api/admin/users` | `user.username`, `user.nickname` | 前后模糊 |

> 订单搜索（`/api/admin/orders`）中的 `orderNo`、`userId` 为精确匹配，不属于模糊搜索范畴。

### 10.2 SQL 实现标准

使用 MyBatis-Plus `like()` 方法，底层生成 `LIKE '%keyword%'`：

```java
// 正确：参数化查询，自动处理 % 包裹
lambdaQueryWrapper.like(StringUtils.hasText(keyword), Product::getName, keyword);
// 等价 SQL：WHERE name LIKE CONCAT('%', ?, '%')
```

### 10.3 硬性约束

| 约束 | 说明 |
|------|------|
| **参数化查询** | 必须使用 `#{}` 或 MyBatis-Plus `like()`，禁止 `${}` 拼接用户输入 |
| **必须分页** | 搜索接口必须配合 `page` + `pageSize`，禁止无分页的全量模糊搜索 |
| **前后模糊** | 统一使用 `%keyword%`（前后均模糊匹配），不要求精确前缀匹配 |
| **空结果** | 搜索无结果返回 `{ records: [], total: 0 }`，不返回 404 |
| **最小输入** | 前端输入长度 ≥1 才发起请求，空字符串或纯空格不传 `keyword` 参数 |

### 10.4 前端搜索规范

```ts
import { useDebounceFn } from '@vueuse/core'

const keyword = ref('')

// 300ms 防抖，避免每次按键都发起请求
const debouncedSearch = useDebounceFn(() => {
  const v = keyword.value.trim()
  fetchProducts({ page: 1, keyword: v || undefined })
}, 300)
```

- 搜索时重置页码为 1
- 输入内容 `trim()` 后再传参，纯空格视为无 keyword
- keyword 为空时参数传 `undefined`（不发送该字段），后端忽略该条件

### 10.5 安全约束

| 约束 | 说明 |
|------|------|
| **防 SQL 注入** | keyword 使用参数绑定，MyBatis-Plus `like()` 内部对 `%`、`_` 等 SQL 通配符做转义 |
| **防 XSS** | 搜索框输入不直接回显到 HTML，Vue 模板 `{{ }}` 默认转义即可 |
| **防 DOS** | 依赖分页限制单次返回量，禁止超长 keyword（前端限制 ≤100 字符） |

### 10.6 禁止事项

- 禁止对 `description`（TEXT 长文本字段）做模糊搜索
- 禁止引入 MySQL FULLTEXT 全文索引 — 当前数据规模不需要
- 禁止前端一次性加载全部数据后本地 filter 模拟搜索
- 禁止后端做分词、拼音搜索、拼写纠错 — 保持简单 LIKE 匹配

---

## 十一、环境变量

| 变量 | 说明 | 示例值 |
|------|------|--------|
| `DB_PASSWORD` | MySQL 密码 | `root` |
| `JWT_SECRET` | JWT 签名密钥 | ≥32 位随机字符串 |
| `UPLOAD_PATH` | 文件上传目录 | `./upload`（默认） |
