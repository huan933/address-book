# 通讯录后端（Spring Boot）

通讯录 Web 应用的 Java 后端。前端页面（`../index.html`）保持 localStorage 版本不变，
后端作为仓库内可独立运行的模块提供同等的数据能力：联系人与多联系方式、收藏、搜索、
Excel 导入导出。

## 技术栈

| 项 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.3.5（Java 17） |
| 持久层 | Spring Data JPA + H2（文件模式，免安装） |
| 校验 | Jakarta Bean Validation |
| Excel | Apache POI 5.2.5 |
| 构建 | Maven |

## 目录结构

```
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/fzu/addressbook/
    │   │   ├── AddressBookApplication.java     启动类
    │   │   ├── config/WebConfig.java           CORS
    │   │   ├── domain/Contact.java             联系人实体（contact 表）
    │   │   ├── domain/ContactMethod.java       联系方式实体（contact_method 表）
    │   │   ├── repository/ContactRepository.java
    │   │   ├── service/ContactService.java     业务逻辑 + 导入导出
    │   │   ├── service/ExcelSupport.java       12 列 Excel 契约读写
    │   │   └── web/                            Controller / DTO / 异常处理
    │   └── resources/application.yml
    └── test/java/com/fzu/addressbook/AddressBookApiTest.java
```

## 启动

需要 JDK 17 与 Maven 3.9+：

```bash
cd backend
mvn spring-boot:run
```

默认监听 `http://localhost:8080`，数据文件生成在 `backend/data/addressbook.mv.db`（首次启动自动建表）。

打包成可执行 jar：

```bash
mvn clean package
java -jar target/address-book-backend-1.0.0.jar
```

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/contacts?favorite=true&q=关键字` | 列表；`favorite=true` 只看收藏，`q` 模糊搜索姓名/单位/备注/联系方式 |
| GET | `/api/contacts/{id}` | 详情，不存在返回 404 |
| POST | `/api/contacts` | 新增，返回 201 |
| PUT | `/api/contacts/{id}` | 更新（联系方式整体替换） |
| POST | `/api/contacts/{id}/favorite` | 收藏开关（调用一次取反） |
| DELETE | `/api/contacts/{id}` | 删除，返回 204 |
| GET | `/api/contacts/export` | 导出 xlsx（一名联系人一行、每类信息一列） |
| POST | `/api/contacts/import` | 导入 xlsx/xls/csv（`multipart/form-data`，字段名 `file`） |
| GET | `/api/contacts/health` | 健康检查 `{"status":"ok"}` |

新增 / 更新请求体：

```json
{
  "name": "陈默",
  "org": "福州大学",
  "note": "同组",
  "memo": "示例数据",
  "favorite": true,
  "methods": [
    { "type": "phone", "value": "138 0000 0001" },
    { "type": "email", "value": "chenmo@example.com" },
    { "type": "social", "value": "微信 chenmo" }
  ]
}
```

`type` 仅接受 `phone / email / social / address / other`；`name` 必填，`methods` 至少一条，否则返回 400。

## Excel 契约

与前端导出完全一致的 12 列，顺序固定：

```
姓名 | 单位 | 收藏 | 备注 | 电话1 | 电话2 | 电话3 | 邮箱1 | 邮箱2 | 社交1 | 社交2 | 地址
```

- 收藏列写 `是` / `否`；导入时 `是 / y / yes / true / 1` 都按收藏处理。
- 超出列宽的联系方式（第 4 个电话、第 3 个邮箱等）与 `address`、`other` 类型合并进「地址」列，用 `；` 连接，避免信息丢失。
- 导入在前 5 行内自动探测表头，容忍中英文写法（姓名/name、单位/公司/org、电话/手机/tel、邮箱/邮件/email、社交/微信、地址/address）。
- 缺姓名、或整行没有任何联系方式的行计入 `invalid`；与库中已有联系人重名的计入 `duplicates`。
- 导入结果：`{"added": 1, "duplicates": 0, "invalid": 0}`。

## 数据模型

```
contact (id, name, org, note, memo, favorite, created_at, updated_at)
  └─ contact_method (id, contact_id, type, value)    一对多，删除联系人级联删联系方式
```

## 测试

```bash
mvn test
```

`AddressBookApiTest` 用内存 H2 覆盖新增、无联系方式拒绝、收藏切换、搜索、导出 xlsx 字节头、
CSV 导入去重、删除后 404 共 6 个用例。

此外对打包后的 jar 做过一轮真实启动的在线冒烟（18 项全部通过）：新增含 4 条联系方式的联系人、
中文搜索、收藏切换与收藏列表、导出文件名中文编码、导出表头 12 列顺序、回灌自己导出的文件判重名、
CSV 导入并落库收藏标记、无联系方式返回 400、不存在 id 返回 404、删除返回 204 且随后 404。

## 与前端的关系

前端「导出 / 导入」目前走浏览器端 SheetJS，不依赖后端；后端接口用于后续把数据搬到服务端时替换
`load()` / `save()` 两处实现，接口字段与前端的数据结构一一对应，切换成本只有这两个函数。
