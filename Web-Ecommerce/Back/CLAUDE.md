# Backend — 开发约束

> 详细接口定义与数据库设计见 [TECH_SPEC.md](./TECH_SPEC.md)
> 关联文档：[ProjectNorms.md](../../ProjectNorms.md) | [Frontend CLAUDE.md](../Front/CLAUDE.md)

## 技术栈

Spring Boot ≥3.2 + MyBatis-Plus ≥3.5 + MySQL ≥8.0 + Java ≥17 + Maven ≥3.8 + JWT (jjwt ≥0.12) + BCrypt + Lombok

## 目录结构

```
Back/src/main/java/com/ecommerce/
├── config/            # CorsConfig, WebMvcConfig, MyBatisPlusConfig
├── common/            # Result, PageResult, BusinessException, GlobalExceptionHandler
├── security/          # JwtUtils, UserContext(ThreadLocal), LoginInterceptor, AdminInterceptor
├── controller/        # 用户端 Controller（Auth, User, Product, Category, Cart, Order）
├── controller/admin/  # 管理端 Controller（Auth, Dashboard, Product, Category, Order, User, Upload, Banner, Announcement）
├── service/           # 业务接口 + impl/
├── mapper/            # MyBatis Mapper 接口
├── entity/            # 数据库实体
└── dto/               # 请求/响应 DTO
```

## 职责边界

**后端负责：** 金额计算（前端仅展示）、库存扣减（乐观锁防超卖）、权限裁定（JWT + 拦截器）、数据完整性（事务 + 唯一约束）、敏感信息过滤（密码不入返回体）

**后端不负责：** 页面渲染、路由跳转（仅返回 401/403）、图片裁剪压缩（前端做）、真实支付渠道对接（模拟支付即可）

## 核心编码约束

1. **分层职责** — Controller 只做参数接收+调用 Service；Service 承载全部业务逻辑，接口与实现分离；Mapper 仅数据访问，不含业务判断
2. **金额** — 全部用 `BigDecimal`，禁止 `float`/`double`；订单金额以后端计算为准，不信任前端传入金额
3. **SQL 安全** — 参数绑定用 `#{}`，禁止 `${}` 拼接用户输入
4. **事务** — 下单、扣库存、退款等涉及多表写入的操作必须加 `@Transactional`
5. **库存并发** — `UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?`，乐观锁防超卖
6. **用户隔离** — 从 `UserContext`（ThreadLocal）取 userId，不信任前端传参；用户只能操作自己的购物车/订单/地址
7. **密码** — BCrypt 加密存储，不返回给前端
8. **空值** — 列表无数据返回 `[]`，不返回 null；分页默认 `page=1, pageSize=20`
9. **异常** — 统一抛 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 转换为 `{ code, message, data: null }`
10. **日志** — 注册、下单、发货、退款等关键操作用 `@Slf4j` 打日志

## 拦截器规则

```
/api/auth/**          → 公开
/api/products/**      → 公开
/api/categories/**    → 公开
/api/cart/**          → 用户登录
/api/user/**          → 用户登录
/api/orders/**        → 用户登录
/api/admin/auth/**    → 公开
/api/admin/**         → 管理员登录 + role 校验
/api/admin/upload     → 管理员登录
```

## 文件上传

- 仅允许 `jpg/jpeg/png/gif/webp`，≤2MB
- 文件名 UUID + 原始扩展名，存 `./upload/`
- 通过 `WebMvcConfig` 映射为公开 URL

## 验收检查清单

- [ ] 用户端 + 管理端全部接口实现，响应格式 `{ code, message, data }`
- [ ] 分页接口接受 `page`/`pageSize`，返回 `{ records, total, page, pageSize }`
- [ ] 未登录请求返回 401，未授权管理员接口返回 403
- [ ] 密码 BCrypt 加密，SQL 全部 `#{}` 参数化
- [ ] 下单事务内完成：创建订单 + 扣库存 + 清购物车已选
- [ ] 库存扣减使用乐观锁，取消订单恢复库存
- [ ] 订单状态流转遵循状态机，不允许非法跳转
- [ ] 金额用 `BigDecimal`，日期用 `LocalDateTime`
- [ ] Controller 无业务逻辑，Service 接口与实现分离
- [ ] 跨域配置允许 localhost:5173、localhost:5174
- [ ] 异常由 GlobalExceptionHandler 统一兜底，不暴露堆栈
