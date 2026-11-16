# 通讯录 Address Book · 前端

极限编程（Extreme Programming）课程作业的前端部分：一个可直接在浏览器打开的通讯录应用。

## 目录结构

```
address-book/
├── frontend/通讯录.html     前端（单文件，HTML + 内联 CSS/JS）
├── backend/                Spring Boot 后端（Java 17 + JPA + H2 + POI）
├── docs/                   截图、演示视频（占位）
├── .vscode/                VS Code 配置（Live Server 根目录指向 frontend）
├── .gitignore
└── README.md
```

## 运行方式

**方式一（推荐，VS Code）**：VS Code 打开 `address-book` 文件夹 → 装 Live Server 扩展 → 右键 `frontend/通讯录.html` → 「Open with Live Server」，浏览器访问 `http://127.0.0.1:5500/通讯录.html`。

**方式二（最省事）**：直接双击 `frontend/通讯录.html`，无需安装、无需构建、无需启动服务器。

- 数据保存在浏览器 localStorage（key: `xp.addressbook.v1`），刷新不丢；
- 首次打开会自动写入 5 条示例联系人，可随时删除；
- Excel 导入导出依赖 SheetJS（CDN 加载），需要联网。断网时导出会自动降级为 CSV（Excel 可直接打开）。

## 已实现功能（对照评分点）

| 作业要求 | 分值 | 实现情况 |
| --- | --- | --- |
| 页面样式 | 10 | 单列列表 + 内联编辑区，原生无框架、无第三方 UI 库，窄屏自动单列堆叠 |
| 收藏联系人 | 25 | 详情页「收藏」按钮切换；左侧「收藏」视图过滤；列表行显示金色星标 |
| 多个联系方式 | 10 | 一个联系人可挂任意多条联系方式，类型含电话 / 邮箱 / 社交账号 / 地址 / 其他，编辑时可增删行 |
| 导入导出 | 25 | 导出 xlsx：一位联系人一行，姓名 / 单位 / 收藏 / 备注 / 电话1-3 / 邮箱1-2 / 社交1-2 / 地址各占一列；导入时按表头自动识别列，跳过缺姓名或重复的行 |

附加的小功能：搜索（姓名 / 单位 / 备注 / 所有联系方式）、收藏与全部两个视图的计数。

> 更完整的三栏精致版样式留在 `_backup/通讯录-精致版.html`（不属于交付产物，可忽略）。

## 导入的 Excel 表格格式

表头需包含「姓名」列，其余列按关键字自动识别（单位、收藏、备注、电话、邮箱、社交、地址）。推荐直接用「导出 Excel」生成的文件作为导入模板，导出与导入格式完全对齐，可原样回灌。

## 后续接后端时的改动位置

前端的数据读写集中在三处，替换成 `fetch` 调用即可，其余 UI 代码不用动：

1. `load()` —— 页面启动时读数据（现为 localStorage）
2. `save()` —— 每次增删改后持久化（现为 localStorage）
3. `exportRows()` / `importAoA()` —— Excel 读写（现为浏览器端 SheetJS，接后端后可改为调用 `/api/contacts/export`、`/api/contacts/import`）

联系人数据结构（对应后端建表时 contact + contact_method 一对多两张表）：

```json
{
  "id": "c-xxxx",
  "name": "陈默",
  "org": "福州大学 · 电子信息工程",
  "note": "同组，嵌入式",
  "memo": "实验室 3 号楼 402。",
  "favorite": true,
  "createdAt": 1760000000000,
  "updatedAt": 1760000000000,
  "methods": [
    { "type": "phone",   "value": "138 0013 8000" },
    { "type": "email",   "value": "chenmo@example.com" },
    { "type": "social",  "value": "微信：chenmo_fzu" }
  ]
}
```

`type` 取值：`phone` / `email` / `social` / `address` / `other`。

## 部署提示

若要用 GitHub Pages 之类的静态托管，把 `frontend/通讯录.html` 复制或重命名为 `index.html` 放在仓库根目录（或 `docs/`）即可。

## 文件说明

- `frontend/通讯录.html` —— 单文件前端（HTML + 内联 CSS + 内联 JS），全部逻辑在这一个文件里
- `README.md` —— 本说明
- `.vscode/settings.json` —— Live Server 根目录、编码等配置
- `.gitignore` —— 已排除依赖目录、构建产物、导出的 Excel、自检截图
## 相关文档

- 作业博客：[docs/极限编程作业博客.md](docs/极限编程作业博客.md)
- 代码规范：[docs/code-style.md](docs/code-style.md)

## 在线访问（GitHub Pages）

已部署到 GitHub Pages，可直接打开：

- 在线演示：https://huan933.github.io/address-book/
- 源码仓库：https://github.com/huan933/address-book

仓库根目录的 `index.html` 是 `frontend/通讯录.html` 的副本，仅作为 Pages 入口。改完前端代码后需把改动同步到 `index.html`（或把 Pages 源改成 `frontend/` 目录）。