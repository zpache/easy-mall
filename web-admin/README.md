# web-admin（Vben Admin 5 · web-ele）

基于 [Vben Admin](https://doc.vben.pro) v5.5.2（Gitee 镜像，2024-12-28）裁剪的管理后台。

## 裁剪说明

相对上游做了以下调整，**升级 vben 时注意保留这些差异**：

- 只保留 `apps/web-ele`（Element Plus 变体），删除了 `web-antd / web-antdv-next / web-naive / docs / playground / apps/backend-mock`
- 根 `package.json`：`name` 改为 `easy-mall-web-admin`；`dev/build/preview` 直接指向 web-ele；删除了 `prepare`（husky，本仓库 vendored 无独立 .git）和 changeset 相关脚本
- `pnpm-workspace.yaml` 移除 `docs`、`playground`
- `apps/web-ele/.env.development`：`VITE_GLOB_API_URL=/admin`，`VITE_NITRO_MOCK=false`
- `apps/web-ele/vite.config.mts`：dev 代理 `/admin -> http://127.0.0.1:8081`（后端 context-path 就是 /admin，无需 rewrite）
- `src/api/core/auth.ts`：权限码接口改为 `/user/codes`（vben 默认 `/auth/codes`）
- `src/views/_core/authentication/login.vue`：删除演示账号下拉与纯前端滑块验证码，只保留账号密码

## 后端约定（easy-mall-admin）

- 响应统一 `{ code, message, data }`，**成功 code = 0**，业务错误 4xxx
- 登录 `POST /admin/auth/login` 返回 `accessToken / refreshToken`（refreshToken 当前与 accessToken 同值，刷新端点未实现，vben 的 enableRefreshToken 保持 false，过期直接重新登录）
- `GET /admin/user/info` 返回 vben UserInfo 结构（roles 为数组）
- `GET /admin/user/codes` 返回空数组（权限只有 ADMIN/OPERATOR 两级，菜单按角色过滤）

## 常用命令

```bash
cd web-admin
pnpm dev          # 启动 web-ele，http://localhost:5777
pnpm build        # 生产构建（产物 apps/web-ele/dist）
pnpm -F @vben/web-ele run typecheck   # 类型检查
```

> 安装依赖必须用 `corepack pnpm install`（根 package.json 锁定 pnpm@9.15.1）。
> 在 WorkBuddy 等 Agent 终端里执行时需要 `env -u NODE_OPTIONS`，否则注入的 fs shim 会破坏 pnpm 并行安装。

## 升级 vben 的方式

本目录是 vendored（已删除上游 .git），升级时重新 clone 对应版本、重放上面列出的差异点，用 `git diff` 对照即可。业务代码只写在 `apps/web-ele/src`（views / api / router），不要改 `packages/@core`，尽量减少合并冲突。
