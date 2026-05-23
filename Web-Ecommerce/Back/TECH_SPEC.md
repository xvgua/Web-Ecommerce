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
| GET | `/products/:id/reviews` | 无 | `?page, pageSize, rating?` | `PageResponse<Review> + ratingStats` |

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

### 2.7 评价 — `/api/reviews`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| POST | `/reviews` | 用户 | `{ orderId, productId, rating (1-5), content, images? }` | `Review` |

> 仅可对已完成的订单商品评价，一个订单商品仅可评价一次。评价后更新 product 表 `avg_rating` 和 `review_count`。

### 2.8 订单评价查询 — `/api/orders`

| 方法 | 路径 | 鉴权 | 响应 data |
|------|------|------|-----------|
| GET | `/orders/:id/reviewable-items` | 用户 | `ReviewableItem[]`，含 `{ productId, productName, productImage, reviewed }` |

> 仅已完成订单返回数据。`reviewed` 字段表示该商品是否已评价。

### 2.9 用户文件上传 — `/api/upload`

| 方法 | 路径 | 鉴权 | 请求体 | 响应 data |
|------|------|------|--------|-----------|
| POST | `/upload` | 用户 | `multipart/form-data` | `{ url: string }` |

> 校验规则同管理员上传（仅允许 jpg/jpeg/png/gif/webp，≤2MB）。用于评价图片上传等用户端场景。

### 2.10 文件上传 — `/api/admin/upload`

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
  avg_rating  DECIMAL(2,1)   DEFAULT 0 COMMENT '平均评分 0.0~5.0',
  review_count INT           DEFAULT 0 COMMENT '评价总数',
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FULLTEXT INDEX ft_product_name (name) WITH PARSER ngram
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
  images      VARCHAR(2000)  DEFAULT '' COMMENT 'JSON array，最多3张',
  create_time DATETIME       DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_product_id (product_id),
  INDEX idx_user_id (user_id),
  UNIQUE KEY uk_order_product (order_id, product_id)
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
| 四、辅助功能 | 文件上传 → 轮播图 CRUD → 公告 CRUD → ~~评价~~（已迁至独立章节 [十一](#十一商品评价系统)）→ 收藏 |
| 五、联调测试 | 前后端联调 → Postman/JUnit → 异常场景 → 数据一致性 |

---

## 十、模糊搜索规范

### 10.1 技术选型

#### 方案对比

MySQL 生态下实现模糊搜索有三种主流方案：

| 维度 | LIKE '%keyword%' | FULLTEXT + ngram | Elasticsearch |
|------|:---:|:---:|:---:|
| 中文支持 | 差（逐字符匹配，无语义） | 好（ngram 字符级分词） | 好（ik/jieba 词典分词） |
| 性能（10万行） | 全表扫描，~1s+ | 倒排索引，~10ms | 倒排索引，~5ms |
| 性能（100万行） | 全表扫描，~10s+ | 倒排索引，~50ms | 倒排索引，~10ms |
| 数据一致性 | ACID（同一 DB） | ACID（同一 DB） | 最终一致（需同步） |
| 额外依赖 | 无 | 无（MySQL 8.0 内置） | ES 集群 + DTS/Canal |
| 运维复杂度 | 零 | 零 | 高 |
| 相关性排序 | 不支持 | 支持（MATCH score） | 支持（BM25） |
| 拼写纠错 / 同义词 | 不支持 | 不支持 | 支持 |

#### 选型结论

**本项目采用 MySQL 8.0 InnoDB FULLTEXT 索引 + ngram 解析器。**

理由：

1. **MySQL 8.0 已内置** — InnoDB FULLTEXT 自 5.6 引入，ngram 解析器自 5.7.6 引入，本项目最低要求 MySQL 8.0，无需任何额外组件
2. **中文友好** — ngram 将文本切分为连续 N 字符片段（默认 `n=2`），对中文、日文、韩文有原生支持；`LIKE '%keyword%'` 对中文只是逐字符暴力匹配，无分词能力
3. **性能跨越式提升** — 倒排索引替代全表扫描，搜索延迟从秒级降至毫秒级
4. **零运维成本** — 索引在同一个 MySQL 实例内，写入立即可搜索（无同步延迟），ACID 事务保障
5. **规模适配** — 当前数据量虽小，但 FULLTEXT 索引的 DDL 和查询语法与 LIKE 的改动量相近，直接采用规范做法避免后续迁移

**不选 Elasticsearch 的原因：** 本项目数据规模远未达到需要独立搜索引擎的量级，引入 ES 会带来集群部署、数据同步、双写一致性等问题，超过当前收益。

**不选 LIKE 的原因：** 全表扫描不可控，中文匹配效果差，随着数据增长性能线性恶化。仅保留作为 FULLTEXT 的降级兜底（见 10.8）。

---

### 10.2 适用范围

以下接口通过 `keyword` 参数支持模糊搜索：

| 接口 | 搜索字段 | 索引方式 |
|------|---------|----------|
| `GET /api/products` | `product.name` | FULLTEXT (ngram) |
| `GET /api/admin/products` | `product.name` | FULLTEXT (ngram) |
| `GET /api/admin/users` | `user.username`, `user.nickname` | LIKE（见 10.3 说明） |

> **说明：**
> - 订单搜索（`/api/admin/orders`）中的 `orderNo`、`userId` 为精确匹配，不属于模糊搜索范畴
> - 用户搜索 (`username`, `nickname`) 字段值通常较短（≤20 字符），ngram token 粒度过细反而降低准确度，维持 LIKE 匹配

---

### 10.3 DDL — 全文索引定义

```sql
-- product 表：对 name 字段建立 FULLTEXT 索引，使用 ngram 解析器
ALTER TABLE product ADD FULLTEXT INDEX ft_product_name (name) WITH PARSER ngram;

-- 确认 ngram token 大小（默认 2，适合中文双字片段匹配）
-- SHOW VARIABLES LIKE 'ngram_token_size';  -- 预期: 2
```

**索引维护说明：**
- FULLTEXT 索引由 InnoDB 自动维护，INSERT/UPDATE/DELETE 时同步更新，无需手动重建
- 初始建表时可直接在 CREATE TABLE 中定义：
  ```sql
  CREATE TABLE product (
    -- ... 其他列 ...
    name VARCHAR(200) NOT NULL,
    FULLTEXT INDEX ft_product_name (name) WITH PARSER ngram
  );
  ```

---

### 10.4 后端实现规范

#### 10.4.1 Mapper 层

需在 `ProductMapper` 中新增自定义查询方法，因 MyBatis-Plus 的 `like()` 生成的是 `LIKE` 语法，无法利用 FULLTEXT 索引：

```java
// ProductMapper.java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 使用 FULLTEXT 全文索引搜索商品，支持分页。
     * MATCH ... AGAINST 使用 IN BOOLEAN MODE 以支持 + 前缀匹配
     */
    @Select("SELECT p.* FROM product p " +
            "WHERE MATCH(p.name) AGAINST(CONCAT('+', #{keyword}) IN BOOLEAN MODE) " +
            "ORDER BY MATCH(p.name) AGAINST(CONCAT('+', #{keyword}) IN BOOLEAN MODE) DESC, p.sales DESC")
    List<Product> searchByKeyword(@Param("keyword") String keyword, Page<Product> page);
}
```

> MATCH ... AGAINST 底层走倒排索引，不等同于 LIKE。MyBatis-Plus 的 `wrapper.like()` 生成的 SQL 是 `LIKE '%keyword%'`，会绕过 FULLTEXT 索引并退化为全表扫描，因此必须使用自定义查询。

#### 10.4.2 Service 层

```java
// ProductServiceImpl.java

@Override
public PageResult<Product> getProductPage(ProductQuery query) {
    String keyword = query.getKeyword();
    Page<Product> page = new Page<>(query.getPage(), query.getPageSize());

    if (StringUtils.hasText(keyword) && keyword.trim().length() > 0) {
        // 走 FULLTEXT 索引
        String kw = keyword.trim();
        productMapper.searchByKeyword(kw, page);
    } else {
        // 无 keyword 时走普通查询，支持分类、排序、价格区间筛选
        LambdaQueryWrapper<Product> wrapper = buildProductWrapper(query);
        productMapper.selectPage(page, wrapper);
    }
    return PageResult.of(page);
}

// buildProductWrapper 封装分类/排序/价格筛选逻辑，与 keyword 搜索互斥
private LambdaQueryWrapper<Product> buildProductWrapper(ProductQuery query) {
    LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(query.getCategoryId() != null, Product::getCategoryId, query.getCategoryId());
    wrapper.eq(query.getStatus() != null, Product::getStatus, query.getStatus());
    if (query.getMinPrice() != null) wrapper.ge(Product::getPrice, query.getMinPrice());
    if (query.getMaxPrice() != null) wrapper.le(Product::getPrice, query.getMaxPrice());
    // sort 映射见 PRODUCT_SORT_MAP
    return wrapper;
}
```

> 注意：FULLTEXT 搜索与分类/价格筛选**不可同时叠加**（MySQL 优化器在混合 MATCH 和普通 WHERE 条件时可能选择全表扫描）。当 keyword 存在时，FULLTEXT 结果优先；无 keyword 时走普通筛选逻辑。

#### 10.4.3 用户搜索（保持 LIKE）

```java
// AdminServiceImpl.java — 用户搜索维持 LIKE
if (StringUtils.hasText(query.getKeyword())) {
    wrapper.and(w -> w
        .like(User::getUsername, query.getKeyword())
        .or().like(User::getNickname, query.getKeyword())
    );
}
```

> username、nickname 字段较短（≤50 字符），数据量有限，LIKE 即可满足。ngram 对极短文本分词效果不佳，反而引入噪音。

---

### 10.5 前端搜索规范

前端搜索入口集中于**导航栏搜索框**（`DefaultLayout.vue`），用户在所有页面均可通过该搜索框发起搜索。

```ts
// useSearchHistory.ts — 搜索历史（纯前端，localStorage）
import { useDebounceFn } from '@vueuse/core'

const keyword = ref('')

// 300ms 防抖，避免每次按键都发起请求
const debouncedSearch = useDebounceFn(() => {
  const v = keyword.value.trim()
  fetchProducts({ page: 1, keyword: v || undefined })
}, 300)
```

| 规范 | 说明 |
|------|------|
| **防抖** | 输入 300ms 后才发起请求（`useDebounceFn`） |
| **重置页码** | 搜索时页码重置为 1 |
| **trim 处理** | 输入内容 `trim()` 后传参，纯空格视为无 keyword |
| **空值处理** | keyword 为空时传 `undefined`（不发送该字段），后端忽略该条件 |
| **回显** | 产品列表页从 `route.query.keyword` 读取 keyword 回显到搜索框，watch 变化后重新加载 |

---

### 10.6 安全约束

| 约束 | 说明 |
|------|------|
| **防 SQL 注入** | FULLTEXT 查询使用 MyBatis 参数绑定 `#{keyword}`，不拼接字符串 |
| **防 XSS** | 搜索关键词在 Vue 模板中通过 `{{ }}` 默认转义，不通过 `v-html` 回显 |
| **防 DOS** | 分页限制单次返回量（默认 20 条）；前端限制输入 ≤100 字符（`maxlength="100"`） |
| **特殊字符** | FULLTEXT IN BOOLEAN MODE 中 `+ - > < ( ) ~ * " @` 为保留字符，后端需使用 `escapeKeyword()` 转义： |

```java
/**
 * 转义 FULLTEXT BOOLEAN MODE 保留字符，防止用户输入被误解析为操作符
 */
private static final Pattern BOOLEAN_SPECIAL = Pattern.compile("[+\\-><\\(\\)~\\*\"@]");

public static String escapeKeyword(String keyword) {
    if (keyword == null || keyword.isEmpty()) return "";
    return BOOLEAN_SPECIAL.matcher(keyword.trim()).replaceAll("\\\\$0");
}
```

---

### 10.7 搜索历史记录

搜索历史为纯前端特性，基于浏览器 `localStorage` 实现，无需后端接口。

| 约束 | 说明 |
|------|------|
| **存储位置** | 浏览器 `localStorage`，key = `search_history` |
| **数据格式** | `[{ keyword: string, timestamp: number }]` |
| **最大条数** | 20 条（超出截断最早记录） |
| **去重策略** | 同 `keyword` 去重，最新搜索移至顶部 |
| **触发方式** | 点击搜索框（focus）弹出历史列表，输入内容后隐藏 |
| **持久化** | 页面刷新后保留，清除浏览器数据后丢失 |
| **删除方式** | 支持逐条删除（点击 ✕ 图标）和「清除全部历史」一键清空 |
| **异常处理** | `JSON.parse` / `localStorage` 异常时静默降级为空数组，不阻塞搜索 |
| **记录时机** | 仅当按 Enter 键触发搜索时写入历史，trim 后为空不记录 |

**实现方式：**
- Composables：`client/src/composables/useSearchHistory.ts`，导出 `getAll()` / `add()` / `remove()` / `clear()` 四个纯函数
- 组件：`DefaultLayout.vue` 使用 `el-autocomplete` + `trigger-on-focus="true"`，`fetchSuggestions` 内部调用 `getHistory()` 读取历史
- 下拉列表末位追加「清除全部历史」行（特殊 item `type: 'clear'`）

**禁止事项：**
- 禁止将搜索历史上传至后端
- 禁止在 `localStorage` 中存储敏感信息
- 禁止在 product 页面搜索框中展示历史（仅 navbar 搜索框生效）

---

### 10.8 降级策略

当 FULLTEXT 索引因故不可用（如 MySQL 版本 < 5.7、ngram 插件未加载）时，回退至 LIKE 查询：

```java
// ProductServiceImpl.java — 降级逻辑
private static boolean fulltextAvailable = true;

private void searchProduct(ProductQuery query, Page<Product> page) {
    String kw = query.getKeyword().trim();

    if (fulltextAvailable) {
        try {
            productMapper.searchByKeyword(escapeKeyword(kw), page);
            return;
        } catch (Exception e) {
            log.warn("FULLTEXT search failed, falling back to LIKE: {}", e.getMessage());
            fulltextAvailable = false;
        }
    }

    // 降级：MyBatis-Plus LIKE
    LambdaQueryWrapper<Product> wrapper = buildProductWrapper(query);
    wrapper.like(Product::getName, kw);
    productMapper.selectPage(page, wrapper);
}
```

---

### 10.9 禁止事项

- 禁止对 `description`（TEXT 长文本字段，通常数千字）建立 FULLTEXT 索引 — 索引体积过大，且用户不按描述搜索商品
- 禁止前端一次性加载全部数据后在本地 filter 模拟搜索
- 禁止后端做拼音搜索、拼写纠错、同义词扩展 — 保持 ngram 字符级匹配即可
- 禁止引入 Elasticsearch — 当前及可预见的规模不需要
- 禁止在 `%keyword%` 前缀通配场景中依赖 B+ 树索引 — 前缀 `%` 会使 B+ 树失效

---

## 十一、商品评价系统

### 11.1 功能概述

- 用户可对已购买且已完成的订单商品进行评价（1-5 星 + 文字评论 + 可选图片）
- 商品详情页展示评价列表，支持按星级分类筛选
- 商品列表/详情页展示平均评分和评价总数
- 每个订单商品仅可评价一次，评价后不可修改或删除

### 11.2 数据库变更

#### 11.2.1 product 表新增字段

```sql
ALTER TABLE product
  ADD COLUMN avg_rating   DECIMAL(2,1) DEFAULT 0 COMMENT '平均评分' AFTER sales,
  ADD COLUMN review_count INT          DEFAULT 0 COMMENT '评价总数' AFTER avg_rating;
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `avg_rating` | `DECIMAL(2,1)` | 平均星级（0.0 ~ 5.0），新增评价时实时重算 |
| `review_count` | `INT` | 累计评价数，新增评价时 +1 |

> 反范式设计：避免每次商品列表查询都 JOIN + COUNT + AVG review 表。`avg_rating` 在新增评价时通过 `AVG(rating) FROM review WHERE product_id = ?` 重算并写回 product 表。

#### 11.2.2 review 表索引补充

```sql
-- 商品评价列表查询（WHERE product_id = ? ORDER BY create_time DESC）
ALTER TABLE review ADD INDEX idx_product_id (product_id);

-- 防重复评价：同一用户对同一订单的同一商品只能评价一次
ALTER TABLE review ADD UNIQUE KEY uk_order_product (order_id, product_id);

-- 用户查询自己的评价列表
ALTER TABLE review ADD INDEX idx_user_id (user_id);
```

### 11.3 API 端点

#### 11.3.1 商品评价列表 — `GET /api/products/:id/reviews`

| 项目 | 说明 |
|------|------|
| 鉴权 | 无（公开） |
| Query 参数 | `page`（默认 1）, `pageSize`（默认 10）, `rating?`（1-5，筛选指定星级） |
| 响应 | `PageResponse<Review>`，每条记录含 `username`, `avatar`, `rating`, `content`, `images`, `createTime` |
| 排序 | 按 `create_time DESC`（最新在前） |

响应 data 结构（ratingStats 通过 PageResult.extra 返回）：

```json
{
  "records": [ ... ],
  "total": 25,
  "page": 1,
  "pageSize": 10,
  "extra": {
    "avgRating": 4.3,
    "reviewCount": 25,
    "distribution": { "5": 12, "4": 8, "3": 3, "2": 1, "1": 1 }
  }
}
```

#### 11.3.2 发表评价 — `POST /api/reviews`

| 项目 | 说明 |
|------|------|
| 鉴权 | 用户登录 |
| 请求体 | `{ orderId: number, productId: number, rating: number (1-5), content: string, images?: string[] }` |
| 响应 | `Review` |

**业务校验（任一不通过返回 400）：**

1. 订单必须存在且属于当前用户
2. 订单状态必须为「已完成」（`status = 3`）
3. 该订单中必须包含该商品（`order_item` 中存在对应记录）
4. 该订单商品未被评价过（`uk_order_product` 唯一约束兜底）
5. `rating` 取值 1-5，`content` 长度 ≥ 1 且 ≤ 500

**后端处理流程（事务内）：**

```
1. 校验订单归属 + 状态 + 包含该商品 + 未重复评价
2. INSERT INTO review
3. UPDATE product SET avg_rating = (SELECT AVG(rating) FROM review WHERE product_id = ?),
                     review_count = review_count + 1
                 WHERE id = ?
4. 返回 Review 对象
```

#### 11.3.3 待评价订单商品 — `GET /api/orders/:id/reviewable-items`

| 项目 | 说明 |
|------|------|
| 鉴权 | 用户登录 |
| 响应 | `ReviewableItem[]`，含 `{ productId, productName, productImage, reviewed: boolean }` |

> 仅当订单状态为「已完成」时返回数据，否则返回空数组。前端根据 `reviewed` 字段决定展示「评价」按钮还是「已评价」标签。

### 11.4 前端页面规范

#### 11.4.1 商品详情页 — 评价区域

商品详情页（`product/detail.vue`）底部增加评价模块：

| UI 要素 | 说明 |
|------|------|
| **评分概览** | 展示平均分（大字号 + 星级条）+ 评价总数 |
| **星级分布** | 5 条进度条（5 星 ~ 1 星），每行格式：`5 星 ████████ 12` |
| **评价筛选** | 标签页/按钮组：全部 | 好评(4-5星) | 中评(3星) | 差评(1-2星)，或按 1-5 星逐级筛选 |
| **评价列表** | 每条评价展示：用户头像 + 用户名（脱敏中间字符）+ 星级 + 日期 + 文字内容 + 图片（若有） |
| **分页** | `el-pagination`，每页 10 条 |
| **空状态** | 暂无评价时展示 `el-empty`，提示"暂无评价" |

#### 11.4.2 订单详情页 — 评价入口

| UI 要素 | 说明 |
|------|------|
| **评价按钮** | 已完成订单的每个商品行末展示「评价」按钮（`reviewed = false` 时）或「已评价」标签（`reviewed = true` 时） |
| **评价弹窗** | `el-dialog` 内含：星级选择（`el-rate` 组件，整数星级，`allow-half="false"`）、文字输入（`el-input type="textarea"`，maxlength=500）、图片上传（可选，最多 3 张，调用 `POST /api/upload`） |
| **提交** | 按钮带 loading 锁，成功后关闭弹窗 + toast 提示 + 刷新订单详情 |

#### 11.4.3 商品列表页 — 评分展示

商品卡片（`ProductCard.vue`）上已有信息中增加评分展示：
- 若 `reviewCount > 0`：展示星级 + 平均分（如 ★ 4.3）+ 评价数（如 25 条评价）
- 若 `reviewCount === 0`：展示灰色文字"暂无评价"

### 11.5 安全约束

| 约束 | 说明 |
|------|------|
| **防刷评价** | 一个订单商品仅可评价一次（数据库唯一约束 + Service 层预校验） |
| **防越权** | 评价时必须校验订单归属（`order.userId === UserContext.getUserId()`） |
| **防 XSS** | `content` 文本在 Vue 模板中通过 `{{ }}` 默认转义，禁止 `v-html` 渲染用户评论 |
| **图片安全** | 评价图片通过 `POST /api/upload`（用户登录）上传，校验规则同管理员上传（类型白名单 + 大小限制） |
| **脱敏** | 前端展示用户名时中间字符替换为 `*`（如「张**三」） |

### 11.6 性能约束

| 约束 | 说明 |
|------|------|
| **评价列表分页** | 每页 10 条，禁止一次性加载全部评价 |
| **评分聚合** | `avg_rating` 和 `review_count` 反范式存储在 product 表，商品列表查询无需 JOIN review 表 |
| **图片懒加载** | 评价图片使用 `el-image` 的 `lazy` 属性懒加载 |
| **索引覆盖** | `idx_product_id` 索引覆盖评价列表的分页排序查询 |

### 11.7 兼容性约束

| 约束 | 说明 |
|------|------|
| **已有商品** | 执行 `ALTER TABLE` 后 `avg_rating = 0`, `review_count = 0`，前端按 0 条评价展示 |
| **已有订单** | 已完成的订单对应用户可以评价，但需从 `reviewable-items` 接口判断是否已评价 |
| **用户删除** | `review.username` 和 `review.avatar` 为冗余字段，用户被删除后评价仍可展示 |
| **商品删除** | 商品删除后其评价保留在 `review` 表中（不设外键），但不影响正常查询（商品已不可见） |

### 11.8 禁止事项

- 禁止用户修改或删除已发表的评价（一锤定音，保持评价公信力）
- 禁止匿名评价（必须登录且购买后评价）
- 禁止对未完成的订单（待支付/待发货/待收货/已取消）发表评价
- 禁止评价内容为空或纯空格
- 禁止前端直接计算平均分（以后端 product.avg_rating 为准）
- 禁止在商品列表 API 中 JOIN review 表做实时聚合（用反范式字段）
- 禁止评价上传视频（仅支持图片，最多 3 张）

---

## 十二、环境变量

| 变量 | 说明 | 示例值 |
|------|------|--------|
| `DB_PASSWORD` | MySQL 密码 | `root` |
| `JWT_SECRET` | JWT 签名密钥 | ≥32 位随机字符串 |
| `UPLOAD_PATH` | 文件上传目录 | `./upload`（默认） |
