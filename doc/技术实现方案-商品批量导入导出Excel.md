# 商品批量导入/导出 Excel — 技术实现方案

> 编写日期：2026-06-04
> 基于用户选择：按建议 MVP 实施

---

## MVP 范围确认

| 决策项 | 选择 |
|--------|------|
| 导出范围 | 当前筛选结果导出（Q1 → C） |
| 导出字段 | ID、名称、分类、价格、库存、描述、状态 + SKU 汇总（Q2） |
| 导入模式 | 有 ID → 更新，无 ID → 新增（Q3 → C） |
| 模板 | 导出即模板（Q4 → C） |
| 校验 | 宽松匹配，按表头名称匹配字段（Q5 → B） |
| 数量限制 | 500 条/次（Q6 → B） |
| 结果反馈 | 详细报告弹窗（Q7 → B） |
| SKU 策略 | 导入不处理 SKU，仅商品基本信息（Q8 → A） |
| 技术选型 | EasyExcel（Q9 → B） |
| 前端交互 | 工具栏按钮（Q10 → A） |

---

## 一、技术选型

**EasyExcel 4.x**，理由：
- 注解驱动，一个 POJO + 注解即可定义 Excel 列映射
- 内存占用低（SAX 解析，不会一次性加载整个文件）
- 读写性能优于 POI
- Spring Boot 集成简单

---

## 二、后端实现

### 2.1 新增依赖

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>4.0.3</version>
</dependency>
```

### 2.2 新增文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `dto/ProductExcelDTO.java` | **新建** | Excel 行数据模型（EasyExcel 注解） |
| `dto/ImportResultDTO.java` | **新建** | 导入结果（成功数、失败数、错误列表） |

### 2.3 修改文件清单

| 文件 | 说明 |
|------|------|
| `pom.xml` | 新增 EasyExcel 依赖 |
| `controller/admin/AdminProductController.java` | 新增 export / import 接口 |
| `service/ProductService.java` | 新增方法签名 |
| `service/impl/ProductServiceImpl.java` | 实现导出/导入逻辑 |

### 2.4 ProductExcelDTO 设计

```java
@Data
public class ProductExcelDTO {
    @ExcelProperty("商品ID")
    private Long id;

    @ExcelProperty("商品名称")
    private String name;

    @ExcelProperty("分类名称")
    private String categoryName;

    @ExcelProperty("价格")
    private BigDecimal price;

    @ExcelProperty("库存")
    private Integer stock;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("状态")
    private String statusText;       // "上架" / "下架"

    @ExcelProperty("规格汇总")
    private String skuSummary;       // "颜色:红/黑; 尺寸:S/M/L"  仅用于展示

    @ExcelProperty("销量")
    private Integer sales;

    @ExcelProperty("创建时间")
    private String createTime;
}
```

**字段说明：**
- `分类名称` — 导出时写入分类中文名，导入时根据名称反查分类 ID
- `规格汇总` — 只读字段，导出时汇总 SKU 信息，导入时**忽略此列**
- `状态` — 导出为"上架"/"下架"，导入时解析

### 2.5 接口设计

#### GET `/api/admin/products/export` — 导出 Excel

```
Query: keyword, categoryId, status（同商品列表筛选条件）
Response: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

逻辑：
1. 接收当前筛选条件
2. 查询所有匹配商品（不分页，最多 5000 条）
3. 遍历商品，填充 ProductExcelDTO 列表
4. 通过 EasyExcel 写入 HTTP 响应流

#### POST `/api/admin/products/import` — 导入 Excel

```
Request: multipart/form-data (file)
Response: { code: 200, data: { successCount: 45, failCount: 3, errors: [...] } }
```

逻辑：
1. 校验文件格式（.xlsx / .xls）
2. 使用 EasyExcel 读取，按表头名称匹配字段
3. 逐行校验：
   - 商品名称不能为空
   - 价格 > 0
   - 库存 >= 0
   - 分类名称需在数据库中存在
   - 状态需为"上架"或"下架"（或空，默认下架）
4. 有 ID → 更新已有商品；无 ID → 新增商品
5. 收集所有错误行信息（行号 + 错误原因）
6. 返回导入结果

### 2.6 核心逻辑伪代码

```java
// ProductServiceImpl.exportProducts
public void exportProducts(HttpServletResponse response, ProductQuery query) {
    List<Product> products = productMapper.selectList(wrapper);
    List<ProductExcelDTO> rows = products.stream().map(p -> {
        ProductExcelDTO dto = new ProductExcelDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCategoryName(categoryMap.get(p.getCategoryId()));
        dto.setPrice(p.getPrice());
        dto.setStock(p.getStock());
        dto.setDescription(p.getDescription());
        dto.setStatusText(p.getStatus() == 1 ? "上架" : "下架");
        dto.setSales(p.getSales());
        // SKU 汇总
        List<ProductSku> skus = skuMap.get(p.getId());
        dto.setSkuSummary(buildSkuSummary(skus));
        dto.setCreateTime(formatDate(p.getCreateTime()));
        return dto;
    }).toList();

    EasyExcel.write(response.getOutputStream(), ProductExcelDTO.class)
        .sheet("商品列表").doWrite(rows);
}

// ProductServiceImpl.importProducts
public ImportResultDTO importProducts(MultipartFile file) {
    List<ProductExcelDTO> rows = EasyExcel.read(file.getInputStream())
        .head(ProductExcelDTO.class).sheet().doReadSync();

    ImportResultDTO result = new ImportResultDTO();
    for (int i = 0; i < rows.size() && i < 500; i++) {
        ProductExcelDTO row = rows.get(i);
        try {
            validateRow(row);
            if (row.getId() != null) {
                updateProduct(row);   // 覆盖更新
            } else {
                createProduct(row);   // 新增
            }
            result.successCount++;
        } catch (Exception e) {
            result.failCount++;
            result.addError(i + 2, e.getMessage());  // +2: 行号从1开始 + 表头行
        }
    }
    return result;
}
```

---

## 三、前端实现

### 3.1 修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `views/product/index.vue` | 修改 | 工具栏新增按钮 + 导入对话框 + 上传逻辑 |
| `api/admin.ts` | 修改 | 新增 export / import API 函数 |

### 3.2 界面设计

```
┌──────────────────────────────────────────────────────────────┐
│  商品管理                                                    │
│                                                              │
│  ┌ 搜索 ────┐ ┌ 分类▼ ──┐ ┌ 状态▼ ──┐ [搜索]              │
│  │           │ │         │ │         │                      │
│  └───────────┘ └─────────┘ └─────────┘                      │
│                                                              │
│  [导出Excel] [导入Excel] [下载模板]    [＋ 新增商品]          │
│                                                              │
│  ┌───────────────────────────────────────────┐              │
│  │ (商品表格)                                 │              │
│  └───────────────────────────────────────────┘              │
│                                                              │
│  导入结果弹窗：                                              │
│  ┌──────────────────────────────────────┐                    │
│  │  导入完成                             │                    │
│  │  ✅ 成功：45 条                       │                    │
│  │  ❌ 失败：3 条                        │                    │
│  │                                      │                    │
│  │  错误详情：                           │                    │
│  │  第5行：商品名称不能为空              │                    │
│  │  第12行：分类"手机配件"不存在         │                    │
│  │  第20行：价格格式不正确               │                    │
│  │                                      │                    │
│  │            [关闭]                     │                    │
│  └──────────────────────────────────────┘                    │
└──────────────────────────────────────────────────────────────┘
```

### 3.3 API 函数

```typescript
// admin.ts 新增

// 导出 Excel（直接下载文件，不走 JSON 拦截器）
export function exportProducts(params: ProductQuery): Promise<Blob> {
  return request.get('/admin/products/export', {
    params,
    responseType: 'blob',
  })
}

// 导入 Excel
export function importProducts(file: File): Promise<ApiResponse<ImportResult>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/products/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
```

---

## 四、实施步骤

### 步骤 1：后端 — 依赖 + DTO
1. `pom.xml` 添加 EasyExcel 依赖
2. 新建 `ProductExcelDTO.java`
3. 新建 `ImportResultDTO.java`

### 步骤 2：后端 — 服务层
4. `ProductService` 新增 `exportProducts()` 和 `importProducts()` 方法签名
5. `ProductServiceImpl` 实现导出逻辑
6. `ProductServiceImpl` 实现导入逻辑（校验、分类名反查、新增/更新、错误收集）

### 步骤 3：后端 — 控制器层
7. `AdminProductController` 新增 `GET /export` 和 `POST /import` 接口

### 步骤 4：前端
8. `admin.ts` 新增 `exportProducts` / `importProducts` API
9. `views/product/index.vue` 工具栏增加按钮 + 导入对话框 + 结果弹窗

---

## 五、边界情况与约束

| 场景 | 处理方式 |
|------|----------|
| 导出无数据 | 导出仅含表头的空 Excel |
| 导入文件非 Excel | 返回错误 "仅支持 .xlsx / .xls 格式" |
| 导入超 500 条 | 仅处理前 500 条，超出部分忽略并在结果中提示 |
| 分类名称不存在 | 标记为错误行，跳过该行 |
| 商品名称重复 | 新增时不做去重（运营自行判断），更新时按 ID 即可 |
| 状态列空 | 默认设为 "下架" |
| 价格/库存为空 | 标记为错误 |
| 导出文件编码 | EasyExcel 自动处理，中文 OK |
