# 订单导出 Excel — 技术实现方案

> 编写日期：2026-06-05
> 基于需求澄清文档的用户选择

---

## 决策汇总

| # | 决策 | 要点 |
|---|------|------|
| Q1 | A | 导出当前筛选条件下的全部订单 |
| Q2 | B | 上限 10000 条，超出提示缩小筛选范围 |
| Q3 | 除备注外全部 | 13 个字段：订单编号、用户ID、收货人、联系电话、收货地址、商品原价合计、优惠券抵扣、商品折扣、实付金额、订单状态、下单时间、支付时间、成交时间 |
| Q4 | C | 双 Sheet：Sheet1=订单主表，Sheet2=订单明细（通过订单编号关联） |
| Q5 | C | 完整退款信息：退款类型、退款金额、退款原因、退款状态、退款申请时间、退款处理时间 |
| Q6 | A | EasyExcel 默认样式 |
| Q7 | B | 文件名带时间戳 `订单导出_yyyyMMdd_HHmmss.xlsx` |
| Q8 | C | 按钮放在表格右上角工具栏区域 |
| Q9 | B | 按钮 loading 态，完成自动下载 |
| Q10 | C | 失败弹窗提示错误详情 |
| Q11 | A | 复用现有 `adminGetOrderPage` 查询逻辑 |
| Q12 | A | 一次性查询全部数据写入 Excel |
| Q13 | B | 需管理员登录权限（`/api/admin/**` 拦截器已有） |

---

## 一、技术选型

**EasyExcel 4.0.3**，已在 `pom.xml` 中引入，无需新增依赖。

理由：
- 注解驱动，一个 POJO + `@ExcelProperty` 即可定义 Excel 列映射
- 内存占用低（SAX 解析，不会一次性加载整个文件）
- 支持多 Sheet 写入（`ExcelWriter` + 多个 `WriteSheet`）
- 与现有产品导出功能共用同一技术栈

---

## 二、后端实现

### 2.1 新增文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `dto/OrderExcelDTO.java` | **新建** | Sheet1 订单主表 Excel 行模型 |
| `dto/OrderItemExcelDTO.java` | **新建** | Sheet2 订单明细 Excel 行模型 |

### 2.2 修改文件清单

| 文件 | 说明 |
|------|------|
| `controller/admin/AdminOrderController.java` | 新增 `GET /export` 端点 |
| `service/OrderService.java` | 新增 `exportOrders()` 方法签名 |
| `service/impl/OrderServiceImpl.java` | 实现导出逻辑 |

### 2.3 OrderExcelDTO 设计（Sheet1 — 订单主表）

```java
@Data
public class OrderExcelDTO {
    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("收货人")
    private String receiverName;

    @ExcelProperty("联系电话")
    private String receiverPhone;

    @ExcelProperty("收货地址")
    private String receiverAddress;

    @ExcelProperty("商品原价合计")
    private BigDecimal totalAmount;

    @ExcelProperty("优惠券抵扣")
    private BigDecimal couponDiscount;

    @ExcelProperty("商品折扣")
    private BigDecimal discountAmount;

    @ExcelProperty("实付金额")
    private BigDecimal payAmount;

    @ExcelProperty("订单状态")
    private String statusText;

    @ExcelProperty("下单时间")
    private String createTime;

    @ExcelProperty("支付时间")
    private String payTime;

    @ExcelProperty("成交时间")
    private String dealTime;

    // ===== 退款字段（Q5-C：完整退款信息）=====

    @ExcelProperty("退款类型")
    private String refundTypeText;

    @ExcelProperty("退款金额")
    private BigDecimal refundAmount;

    @ExcelProperty("退款原因")
    private String refundReasonText;

    @ExcelProperty("退款状态")
    private String refundStatusText;

    @ExcelProperty("退款申请时间")
    private String refundApplyTime;

    @ExcelProperty("退款处理时间")
    private String refundDealTime;
}
```

**字段说明：**
- 金额字段使用 `BigDecimal`，EasyExcel 自动处理格式
- 日期字段使用 `String`（格式化后的 `yyyy-MM-dd HH:mm:ss`），避免 Excel 日期格式问题
- 状态字段使用中文文本（"待支付""已完成"等），非数字
- 退款字段仅在订单有退款记录时填充，无退款时为空
- `收货地址` 拼接省/市/区 + 详细地址
- `商品折扣(discountAmount)` 当前版本固定为 0，后续支持商品级折扣活动时生效

### 2.4 OrderItemExcelDTO 设计（Sheet2 — 订单明细）

```java
@Data
public class OrderItemExcelDTO {
    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("规格")
    private String specDesc;

    @ExcelProperty("单价")
    private BigDecimal price;

    @ExcelProperty("数量")
    private Integer quantity;

    @ExcelProperty("小计")
    private BigDecimal subtotal;
}
```

### 2.5 接口设计

#### GET `/api/admin/orders/export` — 导出订单 Excel

```
Query: keyword(订单号), userId, status（与订单列表筛选条件一致）
Response: 成功 → application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
          失败 → application/json { code: xxx, message: "..." }
```

**逻辑流程：**

1. 接收筛选条件（keyword / userId / status），复用 `adminGetOrderPage` 的查询条件构建逻辑
2. 查询匹配订单总数，若 > 10000 条，返回 `BusinessException`（HTTP 400，前端弹窗提示）
3. 查询全部匹配订单（不分页），单次 `selectList` 查询
4. 批量加载关联数据：一次查询所有 orderItem + 一次查询所有 address，内存组装
5. 遍历订单，转换为 `OrderExcelDTO` 和 `OrderItemExcelDTO` 列表
6. 设置响应头（Content-Disposition 带时间戳文件名）
7. 使用 `ExcelWriter` 写入两个 Sheet

### 2.6 核心逻辑实现

```java
// OrderServiceImpl.exportOrders
public void exportOrders(ProductQuery query, HttpServletResponse response) throws IOException {
    // 1. 构建查询条件（复用 adminGetOrderPage 逻辑）
    LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
    if (query.getStatus() != null) {
        wrapper.eq(Order::getStatus, query.getStatus());
    }
    if (query.getUserId() != null) {
        wrapper.eq(Order::getUserId, query.getUserId());
    }
    if (StringUtils.hasText(query.getKeyword())) {
        wrapper.eq(Order::getOrderNo, query.getKeyword());
    }
    wrapper.orderByDesc(Order::getCreateTime);

    // 2. 数量上限检查
    long count = orderMapper.selectCount(wrapper);
    if (count > 10000) {
        throw new BusinessException(
            "匹配订单超过 10000 条上限（当前 " + count + " 条），请缩小筛选范围");
    }
    if (count == 0) {
        // 无数据时仍导出仅含表头的空 Excel
        writeEmptyExcel(response);
        return;
    }

    // 3. 查询全部订单（不分页）
    List<Order> orders = orderMapper.selectList(wrapper);

    // 4. 批量加载关联数据（避免 N+1）
    List<Long> orderIds = orders.stream().map(Order::getId).toList();
    Set<Long> addressIds = orders.stream().map(Order::getAddressId)
        .filter(Objects::nonNull).collect(Collectors.toSet());

    // 批量查 orderItem
    List<OrderItem> allItems = orderItemMapper.selectList(
        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
    Map<Long, List<OrderItem>> itemsMap = allItems.stream()
        .collect(Collectors.groupingBy(OrderItem::getOrderId));

    // 批量查 address
    Map<Long, Address> addressMap = Map.of();
    if (!addressIds.isEmpty()) {
        List<Address> addresses = addressMapper.selectBatchIds(addressIds);
        addressMap = addresses.stream()
            .collect(Collectors.toMap(Address::getId, a -> a));
    }

    // 5. 填充订单的非持久化字段
    for (Order order : orders) {
        order.setItems(itemsMap.getOrDefault(order.getId(), List.of()));
        order.setAddress(addressMap.get(order.getAddressId()));
        order.setStatusText(getStatusText(order.getStatus()));
        if (order.getRefundStatus() != null) {
            order.setRefundReasonText(RefundReason.getReasonText(order.getRefundReason()));
            order.setRefundStatusText(RefundStatus.getStatusText(order.getRefundStatus()));
        }
    }

    // 6. 转换为导出 DTO
    List<OrderExcelDTO> sheet1 = orders.stream().map(o -> toOrderExcelDTO(o)).toList();
    List<OrderItemExcelDTO> sheet2 = orders.stream()
        .flatMap(o -> o.getItems().stream().map(item -> toItemExcelDTO(o.getOrderNo(), item)))
        .toList();

    // 7. 设置响应头
    String filename = "订单导出_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        + ".xlsx";
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");
    response.setHeader("Content-Disposition",
        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

    // 8. 双 Sheet 写入（使用同一个 ExcelWriter）
    ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
    WriteSheet sheet1Write = EasyExcel.writerSheet(0, "订单列表")
        .head(OrderExcelDTO.class).build();
    WriteSheet sheet2Write = EasyExcel.writerSheet(1, "订单明细")
        .head(OrderItemExcelDTO.class).build();
    excelWriter.write(sheet1, sheet1Write);
    excelWriter.write(sheet2, sheet2Write);
    excelWriter.finish();
}

// 转换辅助方法
private OrderExcelDTO toOrderExcelDTO(Order o) {
    OrderExcelDTO dto = new OrderExcelDTO();
    dto.setOrderNo(o.getOrderNo());
    dto.setUserId(o.getUserId());
    if (o.getAddress() instanceof Address addr) {
        dto.setReceiverName(addr.getName());
        dto.setReceiverPhone(addr.getPhone());
        dto.setReceiverAddress(
            addr.getProvince() + addr.getCity() + addr.getDistrict() + " " + addr.getDetail());
    }
    dto.setTotalAmount(o.getTotalAmount());
    dto.setCouponDiscount(o.getCouponDiscount());
    dto.setDiscountAmount(o.getDiscountAmount());
    dto.setPayAmount(o.getPayAmount());
    dto.setStatusText(o.getStatusText());
    dto.setCreateTime(formatDateTime(o.getCreateTime()));
    dto.setPayTime(formatDateTime(o.getPayTime()));
    dto.setDealTime(formatDateTime(o.getDealTime()));
    if (o.getRefundStatus() != null) {
        dto.setRefundTypeText(o.getRefundType() != null && o.getRefundType() == 2
            ? "退货退款" : "仅退款");
        dto.setRefundAmount(o.getRefundAmount());
        dto.setRefundReasonText(o.getRefundReasonText());
        dto.setRefundStatusText(o.getRefundStatusText());
        dto.setRefundApplyTime(formatDateTime(o.getRefundApplyTime()));
        dto.setRefundDealTime(formatDateTime(o.getRefundDealTime()));
    }
    return dto;
}

private OrderItemExcelDTO toItemExcelDTO(String orderNo, OrderItem item) {
    OrderItemExcelDTO dto = new OrderItemExcelDTO();
    dto.setOrderNo(orderNo);
    dto.setProductName(item.getProductName());
    dto.setSpecDesc(item.getSpecDesc());
    dto.setPrice(item.getPrice());
    dto.setQuantity(item.getQuantity());
    dto.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    return dto;
}

private String formatDateTime(LocalDateTime dt) {
    return dt != null
        ? dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        : null;
}

private void writeEmptyExcel(HttpServletResponse response) throws IOException {
    String filename = "订单导出_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        + ".xlsx";
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");
    response.setHeader("Content-Disposition",
        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
    ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
    excelWriter.write(List.of(),
        EasyExcel.writerSheet(0, "订单列表").head(OrderExcelDTO.class).build());
    excelWriter.write(List.of(),
        EasyExcel.writerSheet(1, "订单明细").head(OrderItemExcelDTO.class).build());
    excelWriter.finish();
}
```

### 2.7 Service 接口签名

```java
// OrderService.java 新增
void exportOrders(ProductQuery query, HttpServletResponse response) throws IOException;
```

### 2.8 Controller 端点

```java
// AdminOrderController.java 新增
@GetMapping("/export")
public void exportOrders(ProductQuery query, HttpServletResponse response) throws IOException {
    orderService.exportOrders(query, response);
}
```

---

## 三、前端实现

### 3.1 修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `api/admin.ts` | 修改 | 新增 `exportOrders()` API 函数 |
| `views/order/index.vue` | 修改 | 新增导出按钮 + loading + 下载 + 错误弹窗 |

### 3.2 API 函数

```typescript
// admin.ts 新增（放在 Order Management 区域）

export function exportOrders(params: OrderQuery): Promise<Blob> {
  return request.get('/admin/orders/export', {
    params,
    responseType: 'blob',
  })
}
```

**要点：** 设置 `responseType: 'blob'`，Axios 拦截器（`request.ts` 第 26 行）会跳过 JSON 解析直接返回原始数据。成功时得到 Excel blob，失败时得到 JSON blob。

### 3.3 页面改动（`views/order/index.vue`）

#### 模板改动

在搜索栏下方新增工具栏区域，放置导出按钮 + 错误弹窗：

```html
<div class="toolbar">
  <!-- 现有搜索栏保持不变 -->
</div>

<div class="toolbar-actions">
  <el-button type="success" :loading="exporting" @click="handleExport">
    <el-icon><Download /></el-icon> 导出Excel
  </el-button>
</div>

<!-- 错误弹窗 -->
<el-dialog v-model="exportErrorVisible" title="导出失败" width="440px">
  <p>{{ exportErrorMessage }}</p>
  <template #footer>
    <el-button type="primary" @click="exportErrorVisible = false">知道了</el-button>
  </template>
</el-dialog>
```

#### 脚本改动

```typescript
import { Download } from '@element-plus/icons-vue'
import { exportOrders } from '@/api/admin'

const exporting = ref(false)
const exportErrorVisible = ref(false)
const exportErrorMessage = ref('')

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportOrders({
      keyword: keyword.value || undefined,
      userId: userId.value ? Number(userId.value) : undefined,
      status: status.value || undefined,
    })

    // 检查响应是否为错误 JSON（后端 BusinessException 可能返回 200 + JSON）
    if (blob.type === 'application/json') {
      const text = await blob.text()
      try {
        const json = JSON.parse(text)
        exportErrorMessage.value = json.message || '导出失败'
      } catch {
        exportErrorMessage.value = '导出失败，请重试'
      }
      exportErrorVisible.value = true
      return
    }

    // 正常 Excel 下载
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = ''  // 文件名由后端 Content-Disposition 指定
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    // HTTP 4xx/5xx：Axios 拦截器已弹 ElMessage.error，此处补弹窗详情
    exportErrorMessage.value = e?.response?.data?.message
      || e?.message
      || '导出失败，请检查网络后重试'
    exportErrorVisible.value = true
  } finally {
    exporting.value = false
  }
}
```

#### 样式变更

```scss
.toolbar-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
```

### 3.4 交互流程

```
点击 [导出Excel] → 按钮 loading → 调用 API
  ├─ 成功 → 检测 blob.type === 'application/json'
  │         ├─ 是 → 弹窗显示错误消息
  │         └─ 否 → 触发下载 + ElMessage.success("导出成功")
  └─ HTTP 4xx/5xx → 弹窗显示错误详情（如"匹配订单超过 10000 条上限"）
```

---

## 四、实施步骤

### 步骤 1：后端 — DTO

1. 新建 `dto/OrderExcelDTO.java`
2. 新建 `dto/OrderItemExcelDTO.java`

### 步骤 2：后端 — 服务层

3. `OrderService` 新增 `exportOrders()` 方法签名
4. `OrderServiceImpl` 实现导出逻辑（批量查询 → 内存组装 → 双 Sheet 写入）

### 步骤 3：后端 — 控制器层

5. `AdminOrderController` 新增 `GET /export` 端点

### 步骤 4：前端

6. `admin.ts` 新增 `exportOrders()` API 函数
7. `views/order/index.vue` 新增导出按钮 + loading + 下载 + 错误弹窗

---

## 五、边界情况与约束

| 场景 | 处理方式 |
|------|----------|
| 筛选结果为空 | 导出仅含表头的空 Excel（两个 Sheet 都只有表头行） |
| 匹配订单超过 10000 条 | 后端返回 BusinessException(400)，前端弹窗提示"…请缩小筛选范围" |
| 订单无地址信息 | 收货人/电话/地址列留空 |
| 订单无退款记录 | 退款相关 6 列留空 |
| 订单无支付/成交时间 | 对应时间列留空 |
| 文件名特殊字符 | `URLEncoder.encode(filename, UTF_8)` 编码，浏览器自动解码 |
| 并发导出 | 每次请求独立查询，无状态锁，自然支持并发 |
| 大数据量（接近 10000 条） | 批量查询（3 次 DB 查询）+ EasyExcel 流式写入，内存安全 |

---

## 六、权限说明

根据 Q13-B，导出接口挂在 `/api/admin/orders/export` 路径下，受现有 `AdminInterceptor` 保护（`/api/admin/**` 需管理员登录 + role 校验）。当前系统无粒度更细的按钮级权限模型，导出操作与列表查看权限一致。

---

## 七、与产品导出的差异

| 项目 | 产品导出 | 订单导出 |
|------|----------|----------|
| Sheet 数量 | 单 Sheet | 双 Sheet（订单列表 + 明细） |
| 数据量上限 | 5000 | 10000 |
| 关联数据加载 | 每条单独查 SKU（N+1） | 批量查询（3 次 DB） |
| 错误处理 | `catch { ElMessage.error }` | 弹窗 + 详情消息 |
| 空数据 | 导出空 Excel | 导出空 Excel（双 Sheet） |
