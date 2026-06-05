# Redis 缓存热点数据 — 技术实现方案

## 一、现状分析

### 1.1 已有基础

| 组件 | 状态 | 说明 |
|------|------|------|
| Redis 依赖 | 已引入 | `spring-boot-starter-data-redis` + Jedis 客户端 |
| Redis 连接配置 | 已配置 | `application.yml` 中 `spring.data.redis.host/port` |
| StringRedisTemplate | 已使用 | 仅用于验证码存储（5 分钟 TTL）和秒杀限流（3 秒冷却） |
| Spring Cache 抽象 | **未引入** | 无 `@Cacheable`、`@CacheEvict`、`CacheManager` |

### 1.2 当前问题

每次请求都直接查询 MySQL，以下高频接口无任何缓存：

- `GET /api/products/{id}` — 商品详情，每个用户浏览时必查
- `GET /api/products?sort=sales_desc` — 热销排行，首页核心模块
- `GET /api/products?sort=new` — 新品推荐，首页核心模块
- `GET /api/categories` — 分类列表，导航栏每个页面都加载
- `GET /api/banners` — 轮播图，首页加载

> 在高并发场景下（如秒杀活动），数据库压力集中在商品查询，缓存可大幅降低 MySQL 负载。

---

## 二、缓存策略设计

### 2.1 缓存对象与 TTL

| 缓存区域 | 对应方法 | TTL | 理由 |
|----------|----------|-----|------|
| `product` | `getProductById(id)` | 1 小时 | 访问频次最高；商品信息变更频率低 |
| `hotProducts` | `getHotProducts(limit)` | 8 分钟 | 首页热销排行；销量实时波动，短 TTL 保新鲜度 |
| `newProducts` | `getNewProducts(limit)` | 12 分钟 | 首页新品推荐；与热销错开过期时间，避免同时失效 |
| `category` | `getCategoryTree()` | 2 小时 | 全站导航栏；分类几乎不变 |
| `banner` | `getAllBanners()` | 30 分钟 | 首页轮播；运营偶尔调整 |

> TTL 设计：`hotProducts` 和 `newProducts` 分属两个缓存区且 TTL 不同（8min vs 12min），避免首页两个模块的缓存在同一时刻过期造成瞬时 DB 压力。

### 2.2 缓存更新/失效策略

| 触发操作 | 失效的缓存 |
|----------|-----------|
| 新增商品 (`create`) | 热销列表缓存 + 新品列表缓存 |
| 更新商品 (`update`) | 该商品详情缓存 + 热销列表缓存 + 新品列表缓存 |
| 删除商品 (`delete`) | 该商品详情缓存 + 热销列表缓存 + 新品列表缓存 |
| 订单支付成功 | 热销列表缓存（销量变化） |
| 分类增删改 | 分类列表缓存 |
| Banner 增删改 | Banner 列表缓存 |

> 采用 **Cache-Aside 模式**：读的时候写缓存，写的时候删缓存。避免更新缓存的并发问题，下一次读请求会重新加载最新数据。

### 2.3 用户会话处理

当前 JWT 是无状态的，不做服务端 Session 缓存。改为：

- **Token 黑名单**：用户登出时，JWT 存入 Redis（key = `bl:<userId>:<token前20位>`，TTL = JWT 剩余有效期）
- **拦截器校验**：`LoginInterceptor` 在验证 JWT 前先查黑名单，命中则返回 401
- 好处：弥补 JWT 无法主动失效的缺陷，实现"登出即失效"

---

## 三、技术方案

### 3.1 新增依赖

`pom.xml` 新增：

```xml
<!-- Spring Cache 抽象层 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

> 无需额外加 Caffeine 或 Redis 依赖——缓存后端使用已有的 Redis。

### 3.2 新建 RedisConfig 配置类

`config/RedisConfig.java`：

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Jackson JSON 序列化器（支持 LocalDateTime）
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(...);

        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(mapper);

        // 默认配置：30 分钟 TTL，JSON 序列化，不缓存 null
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(...)
            .serializeValuesWith(...)
            .disableCachingNullValues();

        // 各区域独立 TTL
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("product",     defaultConfig.entryTtl(Duration.ofHours(1)));
        configs.put("hotProducts", defaultConfig.entryTtl(Duration.ofMinutes(8)));
        configs.put("newProducts", defaultConfig.entryTtl(Duration.ofMinutes(12)));
        configs.put("category",    defaultConfig.entryTtl(Duration.ofHours(2)));
        configs.put("banner",      defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(configs)
            .build();
    }
}
```

**关键设计决策：**

- 使用 **Jackson JSON 序列化** 而非 JDK 序列化：值可读、跨语言、更紧凑
- `activateDefaultTyping`：序列化时写入类型信息，反序列化时还原为正确的 Java 类型（如 `Product`、`List<Product>`）
- **禁用 null 缓存**：查不到数据时不缓存，避免缓存穿透
- `JavaTimeModule`：支持 `LocalDateTime` 的 JSON 序列化

### 3.3 Service 层加缓存注解

以 `ProductServiceImpl` 为例：

```java
// 查询：加 @Cacheable — 缓存命中直接返回，未命中则执行方法并缓存结果
@Override
@Cacheable(value = "product", key = "#id")
public Product getProductById(Long id) { ... }

@Override
@Cacheable(value = "hotProducts", key = "#limit")
public List<Product> getHotProducts(int limit) { ... }

@Override
@Cacheable(value = "newProducts", key = "#limit")
public List<Product> getNewProducts(int limit) { ... }

// 新增：清空热销和新品列表缓存
@Override
@Transactional
@Caching(evict = {
    @CacheEvict(value = "hotProducts", allEntries = true),
    @CacheEvict(value = "newProducts", allEntries = true)
})
public Product create(ProductForm form) { ... }

// 更新：清单个商品 + 清空热销和新品列表
@Override
@Transactional
@Caching(evict = {
    @CacheEvict(value = "product", key = "#id"),
    @CacheEvict(value = "hotProducts", allEntries = true),
    @CacheEvict(value = "newProducts", allEntries = true)
})
public void update(Long id, ProductForm form) { ... }

// 删除：同上
@Override
@Caching(evict = {
    @CacheEvict(value = "product", key = "#id"),
    @CacheEvict(value = "hotProducts", allEntries = true),
    @CacheEvict(value = "newProducts", allEntries = true)
})
public void delete(Long id) { ... }
```

> **为什么用 `allEntries = true`**：热销和新品列表的缓存 key 是 `#limit`，不同 `limit` 值会产生不同缓存条目（如 `limit=6`、`limit=10`）。用 `allEntries` 一次性清空整个缓存区，比逐个 key 清除更可靠。`hotProducts` 和 `newProducts` 分属两个独立缓存区，TTL 不同（8min / 12min），避免同时过期。

**注解说明：**

| 注解 | 作用 | 触发时机 |
|------|------|----------|
| `@Cacheable` | 先查缓存，命中则跳过方法直接返回 | 方法调用前 |
| `@CacheEvict` | 方法执行后删除缓存 | 方法执行后（`beforeInvocation=false`） |
| `@Caching` | 组合多个缓存操作 | — |

> **不缓存分页列表**（`getProductPage`）：分页查询参数组合太多（关键词 + 分类 + 价格区间 + 排序 + 分页），缓存命中率极低，而且搜索结果是动态变化的，缓存意义不大。

### 3.4 分类和 Banner 缓存

```java
// CategoryServiceImpl
@Override
@Cacheable(value = "category", key = "'tree'")
public List<Category> getCategoryTree() { ... }

@Override
@CacheEvict(value = "category", key = "'tree'")
public Category create(Category category) { ... }

@Override
@CacheEvict(value = "category", key = "'tree'")
public void update(Long id, Category category) { ... }

@Override
@CacheEvict(value = "category", key = "'tree'")
public void delete(Long id) { ... }

// BannerServiceImpl
@Override
@Cacheable(value = "banner", key = "'all'")
public List<Banner> getAllBanners() { ... }

@Override
@CacheEvict(value = "banner", key = "'all'")
public Banner create(Banner banner) { ... }

@Override
@CacheEvict(value = "banner", key = "'all'")
public void update(Long id, Banner banner) { ... }

@Override
@CacheEvict(value = "banner", key = "'all'")
public void delete(Long id) { ... }

@Override
@CacheEvict(value = "banner", key = "'all'")
public void toggleStatus(Long id, Integer status) { ... }
```

> 注：Banner 的 `getAllBanners()` 返回全部 Banner，用户端 `BannerController.list()` 在内存中过滤 `status=1` 并 `limit(6)`。缓存 key 统一为 `'all'`，简化管理。

### 3.5 Token 黑名单

**LoginInterceptor 改造**：通过构造函数注入 `StringRedisTemplate`（与现有 `JwtUtils` 注入方式一致）。

```java
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    public LoginInterceptor(JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }
        String token = authHeader.substring(7);

        // 1. 先解析 JWT 拿到 userId（不等后续，先解析）
        Claims claims;
        try {
            claims = jwtUtils.parseToken(token);
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
        Long userId = claims.get("userId", Long.class);

        // 2. 查黑名单：仅用 userId + token 前 20 位作为 key
        String blacklistKey = "bl:" + userId + ":" + token.substring(0, 20);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            response.setStatus(401);
            return false;
        }

        UserContext.setUserId(userId);
        UserContext.setRole(claims.get("role", String.class));
        return true;
    }
}
```

**WebMvcConfig 同步修改**：

```java
@Autowired
private StringRedisTemplate stringRedisTemplate;

// 拦截器注册时传入 redisTemplate
registry.addInterceptor(new LoginInterceptor(jwtUtils, stringRedisTemplate))
        .addPathPatterns(...);
```

**AuthController 登出接口**：

```java
@PostMapping("/logout")
public Result<?> logout(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0) {
                stringRedisTemplate.opsForValue().set(
                    "bl:" + userId + ":" + token.substring(0, 20),
                    "1", Duration.ofMillis(remaining));
            }
        } catch (Exception ignored) {
            // token 已过期，不需要加入黑名单
        }
    }
    return Result.ok();
}
```

**Key 设计说明**：

| 方案 | Key 示例 | 长度 |
|------|----------|------|
| ~~完整 token~~ | `blacklist:1:eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInJvbGUiOiJ1c2VyIiwiZXhwIjoxNzE3...` | ~250 字符 |
| **截取前 20 位** | `bl:1:eyJhbGciOiJIUzI1NiJ9` | ~35 字符 |

JWT 前 20 个字符是 Header 部分（Base64 编码的 `{"alg":"HS256"}`），每个 token 的 Header 相同，但配合 `userId` 组合后仍可区分不同用户。即使不同 token 前 20 位相同，配合 userId 也能唯一标识。若需更严谨，可截取前 30 位（含部分 payload）。

---

## 四、改动范围汇总

| 文件 | 操作 | 改动量 |
|------|------|--------|
| `pom.xml` | 新增 1 个依赖 | +5 行 |
| `config/RedisConfig.java` | **新建** | ~55 行 |
| `service/impl/ProductServiceImpl.java` | 3 个 `@Cacheable` + 3 个 `@Caching`（create/update/delete） | +15 行 |
| `service/impl/CategoryServiceImpl.java` | 1 个 `@Cacheable` + 3 个 `@CacheEvict`（create/update/delete） | +4 行 |
| `service/impl/BannerServiceImpl.java` | 1 个 `@Cacheable` + 4 个 `@CacheEvict`（create/update/delete/toggleStatus） | +5 行 |
| `service/impl/OrderServiceImpl.java` | 支付成功方法加 1 个 `@CacheEvict` | +1 行 |
| `security/LoginInterceptor.java` | 构造函数注入 RedisTemplate + 黑名单检查 | +15 行 |
| `config/WebMvcConfig.java` | 注入 StringRedisTemplate 并传给 LoginInterceptor | +3 行 |
| `controller/AuthController.java` | **新增** `/api/auth/logout` 接口 | +20 行 |

> **约 123 行代码，0 行业务逻辑改动，全部改动都是加注解和配置。**

---

## 五、风险与注意事项

### 5.1 缓存穿透

**风险**：恶意请求不存在的商品 ID（如 `id=-1`），缓存中始终没有，每次请求穿透到 DB。

**实际影响有限**：
- `getProductById` 查不到会立刻抛 `BusinessException(404)`，Spring Cache 默认不缓存异常抛出的方法结果
- 恶意请求只会触发一次简单的 `SELECT BY ID`（主键查询），MySQL 单次查询成本极低
- 课程项目的 QPS 远达不到穿透攻击的阈值

**当前设计已足够**：`disableCachingNullValues()` 配合 `@Cacheable` 的默认行为（不缓存 null/异常），穿透风险已基本覆盖。后续若有高并发需求，可加入布隆过滤器。

### 5.2 缓存雪崩

**风险**：大量缓存在同一时刻过期，瞬间流量全部打到 DB。

**应对**：
- 不同数据不同 TTL（1h / 2h / 10min / 30min），天然错开大部分
- `hotProducts` 中 `getHotProducts` 和 `getNewProducts` 都是 10min TTL，且都在首页同时加载，建议错开为 8min 和 12min
- 若后续需要更强防护，可在 `RedisConfig` 中配置 `CacheManager` 时对每个 TTL 加随机偏移（例如 `Duration.ofMinutes(10).plus(Duration.ofSeconds(ThreadLocalRandom.current().nextInt(60)))`）

### 5.3 缓存与数据库一致性

**风险**：更新 DB 成功但删缓存失败，导致读到旧数据。

**应对**：
- `@CacheEvict(beforeInvocation=false)`（默认）：方法执行完成且未抛异常后清除缓存。若事务回滚，缓存已被清除，下次查询会从 DB 重新加载正确数据——性能有损但数据一致
- 如果极端情况删缓存失败（Redis 网络抖动），缓存会在 TTL 到期后自动失效，最多脏读 1 小时（商品详情）/ 12 分钟（列表）
- 可接受范围：读多写少的电商场景，短暂不一致不影响用户体验

### 5.4 开发环境

**注意**：配置了 `spring-boot-starter-data-redis` 后，Redis 连不上会导致应用**启动失败**（`RedisConnectionFailureException`），Spring Cache 不会自动降级。

**开发环境建议**：
- **Redis 正常时**：`application.yml` 不额外配置，默认走 Redis 缓存
- **Redis 不可用时**：临时切换为 `spring.cache.type: simple`（JVM 内存缓存），应用正常启动
- 上线前改回 `type: redis`

```yaml
# 开发环境 profile（application-dev.yml）
spring:
  cache:
    type: simple   # Redis 不可用时用 JVM 内存缓存兜底
```

> 或者在 `application.yml` 中不显式指定 `spring.cache.type`，只在确保 Redis 可用的环境启动。

---

## 六、性能预期

| 场景 | 无缓存（现状） | 有缓存后 |
|------|-------------|---------|
| 商品详情页 | 查 DB 2~3 次（product + SKU + category） | 首次查 DB，后续命中 Redis，响应 ~5ms |
| 首页加载 | 查 DB 4~5 次（hot + new + banner + category） | 全部命中缓存，响应 ~20ms |
| 秒杀商品查询 | 并发全打在 DB 上 | 缓存吸收绝大部分请求 |

> Redis 单机读性能约 10 万 QPS，足以应对课程项目的任何并发场景。

---

## 七、实施顺序

1. `pom.xml` 加 `spring-boot-starter-cache` 依赖
2. 新建 `config/RedisConfig.java`（5 个缓存区：product / hotProducts / newProducts / category / banner）
3. `ProductServiceImpl` 加缓存注解（`getProductById` / `getHotProducts` / `getNewProducts` 的 `@Cacheable`；`create` / `update` / `delete` 的 `@Caching`）
4. `CategoryServiceImpl` 加缓存注解（`getCategoryTree` 的 `@Cacheable`；`create` / `update` / `delete` 的 `@CacheEvict`）
5. `BannerServiceImpl` 加缓存注解（`getAllBanners` 的 `@Cacheable`；`create` / `update` / `delete` / `toggleStatus` 的 `@CacheEvict`）
6. `OrderServiceImpl` 支付成功方法加 `@CacheEvict(value = "hotProducts", allEntries = true)`
7. `LoginInterceptor` 改造（构造函数 + 黑名单检查）+ `WebMvcConfig` 注入 StringRedisTemplate + `AuthController` **新增** `/api/auth/logout` 接口
8. **验证**：
   ```bash
   # 启动项目后，访问首页和商品详情
   # ① 观察 MyBatis SQL 日志：首次有 SQL，再次无 SQL（命中缓存）
   # ② 检查 Redis 中是否生成了缓存 key
   redis-cli KEYS "product::*"
   redis-cli KEYS "hotProducts::*"
   redis-cli KEYS "newProducts::*"
   redis-cli KEYS "category::*"
   redis-cli KEYS "banner::*"
   # ③ 检查 TTL
   redis-cli TTL "product::1"
   # ④ 更新商品后确认缓存已清除
   redis-cli KEYS "product::1"   # 应为 (nil)
   redis-cli KEYS "hotProducts::*"  # 应为 (empty)
   # ⑤ 登出后确认黑名单生效
   redis-cli KEYS "bl:*"
   ```
