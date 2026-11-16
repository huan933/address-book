# 代码规范 · address-book

本文件即博客中「代码规范链接」对应的文档。项目为**单文件前端**（`frontend/通讯录.html`，HTML + 内联 CSS + 内联 JS），因此规范以「单文件内部的组织约定」为主，另附仓库级约定。

## 1. 语言与编码

- 字符编码统一 **UTF-8（无 BOM）**，换行符统一 **LF**（由 `.gitattributes` 与编辑器配置保证）。
- 页面声明 `<meta charset="UTF-8">`，`<html lang="zh-CN">`。
- 文件名与目录名可用中文（如 `通讯录.html`），但**链接中一律使用 URL 编码后的形式**，避免跨平台差异。

## 2. 目录与命名

```
address-book/
├── frontend/通讯录.html   前端唯一源文件
├── docs/                  文档、博客、截图
├── backend/               后端占位（本期未实现）
├── index.html             GitHub Pages 入口（frontend/通讯录.html 的副本）
└── README.md
```

- 目录：全小写、短横线分隔（`frontend`、`docs`；中文文档名除外）。
- HTML 元素 `id`：小写，语义前缀 + 短横线，如 `btn-add`、`in-name`、`editor-title`。
- CSS 类名：小写点号类，状态类加 `.on`（如 `.chip.on`、`.star.on`），禁用行内样式。
- JS 变量/函数：`camelCase`；常量用全大写（`KEY`、`TYPES`、`HEADERS`）；DOM 缓存变量以 `$` 前缀（`$editor`、`$methods`）。
- 数据字段：`camelCase`（`createdAt`、`updatedAt`、`favorite`）。

## 3. 代码风格

- 缩进 **2 空格**，不使用 Tab；语句结尾保留分号。
- 全部代码包在 `(function(){ 'use strict'; ... })()` 的 IIFE 中，**不向全局暴露变量**。
- 变量声明统一 `var`（保持 ES5 兼容，Node/旧浏览器均可直接运行，无构建步骤）。
- 字符串拼接优先使用 `+`，模板字符串仅在不影响兼容性时使用。
- 每个功能区用注释分节：`/* ---------- 数据 ---------- */`、`/* ---------- 渲染 ---------- */` 等，顺序固定为：常量 → 工具函数 → 数据层 → 渲染 → 交互 → 导入导出 → 启动。

## 4. 安全与健壮性约定（硬性要求）

1. **所有进入 DOM 的外部数据必须经过 `esc()` 转义**，禁止直接拼接未转义字符串到 `innerHTML`。
2. 所有 `localStorage` 读写包在 `try/catch` 内，存储不可用时降级为内存态并提示用户。
3. 导入数据必须先校验：**缺姓名或没有任何联系方式的行直接跳过**，重名联系人默认跳过并计数。
4. 任何用户可见的失败都要给出**明确中文提示**（toast），不允许静默失败。
5. 不引入第三方 UI 框架；唯一外部依赖是 SheetJS（CDN 加载），且必须提供**离线降级路径**（导出自动改 CSV）。

## 5. Git 约定

- 分支：`main` 为可发布分支；功能/文档改动走 `feature/*`、`docs/*` 分支，经 **Pull Request** 合并，禁止直接在 `main` 上提交功能性改动。
- 提交信息采用 `type: 描述` 格式，type 取 `feat` / `fix` / `docs` / `chore` / `style`；描述用中文，一句话说清做了什么。
- 一个提交只做一件事；不提交构建产物、导出文件与过程截图（见 `.gitignore`）。

## 6. 编辑器配置

仓库根目录的 `.editorconfig` 已固化：UTF-8、LF、2 空格、去尾随空格、文件末尾留一个换行。VS Code 侧另有 `.vscode/settings.json` 固定 Live Server 根目录与编码。
