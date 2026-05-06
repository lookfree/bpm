# 需要对 jeecgboot-vue3 基座修改的文件

以下文件在原版 jeecgboot-vue3 基础上有改动，集成时需手动应用。

---

## 1. `.env.development`

关闭 mock 模式，对接真实后端：

```diff
-VITE_USE_MOCK = true
+VITE_USE_MOCK = false
```

---

## 2. `src/router/routes/basic.ts`

修复动态路由注册后 404 子路由 name 冲突问题：

```diff
-      name: PAGE_NOT_FOUND_NAME,
+      name: `${PAGE_NOT_FOUND_NAME}Child`,
```

---

## 3. `src/router/guard/permissionGuard.ts`

配合上面 404 子路由 name 变更，两处判断同步更新：

```diff
-    if (from.path === LOGIN_PATH && to.name === PAGE_NOT_FOUND_ROUTE.name && ...) {
+    if (from.path === LOGIN_PATH && (to.name === PAGE_NOT_FOUND_ROUTE.name || to.name === `${PAGE_NOT_FOUND_ROUTE.name}Child`) && ...) {

-    if (to.name === PAGE_NOT_FOUND_ROUTE.name) {
+    if (to.name === PAGE_NOT_FOUND_ROUTE.name || to.name === `${PAGE_NOT_FOUND_ROUTE.name}Child`) {
```

---

## 4. `src/router/routes/index.ts`（或 `asyncRoutes`）

需要引入 BPM 路由模块（文件已在 `src/router/routes/modules/bpm.ts`）：

```ts
import bpm from './modules/bpm';

// 加入路由表
export const asyncRoutes = [
  // ... 其他模块
  bpm,
];
```
