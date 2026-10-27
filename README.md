# easy-mall 微商城

一个前后端分离的微商城项目，当前完成 **后端框架** 与 **小程序端框架**，Web 管理后台前端待开发。

- 后端：Spring Boot 4.1.2 单体应用（Maven 父工程 + 2 个可独立启动的模块）
- 小程序端：uni-app + Vue3 + TypeScript + Vite + Pinia + SCSS
- 数据库：MySQL 8（两个后端服务共用同一个库）

## 目录结构

```
easy-mall/
├── backend/                     # 后端 Maven 父工程
│   ├── pom.xml                  # Java 25 / Spring Boot 4.1.2 / Lombok
│   ├── easy-mall-admin/         # 管理后台接口服务，端口 8081，context-path=/admin
│   └── easy-mall-api/           # 小程序接口服务，端口 8082，context-path=/api
├── miniprogram/                 # 小程序端（uni-app）
└── db/easy_mall.sql             # 建库建表 + 演示数据
```

### 后端模块内部分层

```
com.easymall.{admin|api}
├── EasyMall{Admin|Api}Application.java
├── common/            # 统一响应 Result、分页、异常、登录上下文、日志 traceId 过滤器
├── config/            # MyBatis-Plus、Swagger、跨域、安全
├── security/          # JWT 签发解析、认证过滤器 / 拦截器
├── entity/            # 数据层：实体基类 BaseEntity + 全部实体，不按业务拆分
├── mapper/            # 数据层：全部 Mapper 接口，不按业务拆分
└── modules/           # 业务层：按业务垂直切分，模块内只放 controller/service/dto/vo
    ├── auth           # 登录
    ├── system         # 管理员（仅 admin）
    ├── goods          # 商品 / 分类
    ├── user           # 小程序用户（仅 api）
    ├── cart           # 购物车（仅 api）
    ├── address        # 收货地址（仅 api）
    └── order          # 订单（仅 api）
```

`entity` / `mapper` 平铺在模块根下，是因为它们属于数据层、天然跨业务复用（分类被商品和购物车同时用到，订单要读商品快照）；按业务切分会逼出无意义的跨模块依赖。业务规则仍然收在 `modules/<biz>/service` 里。

## 技术栈与版本

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 25 | `maven.compiler.release=25` |
| Spring Boot | 4.1.2 | 单体，无微服务、无注册中心 |
| Lombok | 由 Boot BOM 管理 | 实体 / DTO / 配置类 |
| MyBatis-Plus | 3.5.18 | `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser` |
| Spring Security | 7.x（随 Boot） | 仅 admin 模块使用 |
| JJWT | 0.13.0 | HS256 |
| SpringDoc OpenAPI | 3.2.0 | Swagger UI |
| uni-app | 3.0.0-5020620260917001 | Vue3 + Vite 5 |
| Pinia | 2.3.1 | 状态管理（user / cart） |
| SCSS | uni-app 内置 | `uni.scss` 全局变量 + `styles/utilities.scss` 少量工具类 |

## 快速开始

### 1. 准备 JDK 25

本机默认 `java` 可能是 17，编译前需切换到 25：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
java -version   # 应显示 25.x
```

### 2. 初始化数据库

```bash
mysql -uroot -p < db/easy_mall.sql
```

会创建 `easy_mall` 库，以及 `sys_user / mall_category / mall_goods / app_user / user_address / cart_item / mall_order / order_item` 表，并写入 3 个分类、6 个演示商品。
默认管理员：**admin / admin123**。

> 脚本开头是 `DROP TABLE IF EXISTS`，**重复执行会清空数据**，只适合初始化，不要在有数据的环境直接跑。

本地开发默认连接 `127.0.0.1:3306/easy_mall`，账号 `root`。**密码不入库**，以下两种方式任选其一（同时存在时环境变量优先）：

1. 生成本机私有配置（推荐，一次配好后面不用管）

   ```bash
   cd backend
   for m in easy-mall-admin easy-mall-api; do
     cp $m/src/main/resources/application-local.yml.example \
        $m/src/main/resources/application-local.yml
   done
   # 然后把两个 application-local.yml 里的 password 改成自己的
   ```

   `application-local.yml` 已被 `.gitignore` 忽略，对应 profile `local`，和 `dev` 一起默认激活；文件不存在时应用照常启动，只是连不上库。

2. 用环境变量：`export MYSQL_PASSWORD='你的密码'`

已经初始化过、且库里有数据的实例**不要重跑上面的脚本**，按需执行 `db/migrations/` 下的增量脚本：

```bash
mysql -uroot -p easy_mall < db/migrations/20261026_add_audit_columns.sql   # 补 created_by / updated_by / version
```

### 3. 启动后端

```bash
cd backend

# 管理后台接口（8081）
mvn -pl easy-mall-admin spring-boot:run

# 小程序接口（8082），另开一个终端
mvn -pl easy-mall-api spring-boot:run
```

数据库连接、账号密码通过环境变量覆盖（默认 `127.0.0.1:3306/easy_mall`，`root`）：

```bash
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_DATABASE=easy_mall \
MYSQL_USERNAME=root MYSQL_PASSWORD=yourpwd \
mvn -pl easy-mall-admin spring-boot:run
```

接口文档：

- 管理后台：<http://127.0.0.1:8081/admin/swagger-ui.html>
- 小程序：<http://127.0.0.1:8082/api/swagger-ui.html>

### 4. 启动小程序端

```bash
cd miniprogram
pnpm install
pnpm dev:mp-weixin     # 微信开发者工具导入 miniprogram/dist/dev/mp-weixin
# 或 pnpm dev:h5       # 浏览器里调页面
```

小程序需要配置的变量在 `.env.development`：`VITE_API_BASE_URL=http://127.0.0.1:8082/api`。
本地调试请在微信开发者工具中勾选「不校验合法域名」。

后端 `easy-mall.wx.mock=true` 时，任何 code 都能登录（本地联调用）；接真实小程序时设置：

```bash
WX_APP_ID=xxx WX_APP_SECRET=xxx WX_MOCK=false
```

## 接口约定

**统一响应**

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 / 401 / 403 / 404 / 500 | 参数错误 / 未登录 / 无权限 / 不存在 / 系统异常 |
| 4xxx | 业务错误（4001 商品不存在、4003 库存不足、4006 数据冲突…） |

**编辑带版本号（乐观锁）**

返回体里的实体都会带审计字段：`createdAt / updatedAt / createdBy / updatedBy / version`。
编辑类接口（`/system/users`、`/categories`、`/goods` 的 PUT）需要把详情里拿到的 `version` 原样回传：

```json
{ "name": "新鲜水果", "parentId": 0, "status": 1, "version": 3 }
```

- 版本一致 → 更新成功，库里 `version` 自增 1；
- 版本不一致（期间被别人改过）→ 返回 `4006 数据已被其他人修改，请刷新后重试`，不会覆盖别人的修改。

`createdBy / updatedBy` 由后端从登录态自动填充，前端不用传；`POST` 新增列表类数据时 `version` 留空即可（后端从 0 开始）。小程序自动注册的账号因为注册瞬间还没有登录态，`createdBy` 为空。

**分页结构**

```json
{ "records": [], "total": 0, "pageNum": 1, "pageSize": 10, "pages": 0 }
```

**鉴权**：请求头 `Authorization: Bearer {accessToken}`。
admin 使用 Spring Security 无状态鉴权（token 12 小时），api 使用 MVC 拦截器 + 白名单（token 7 天）。

### 接口清单

管理后台（`/admin`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 账号密码登录 |
| GET | `/auth/me` | 当前登录管理员 |
| POST | `/auth/logout` | 退出登录 |
| GET/POST/PUT/DELETE | `/system/users` | 管理员分页 / 新增 / 编辑 / 删除 |
| GET | `/categories` | 分类列表 |
| POST/PUT/DELETE | `/categories/{id}` | 分类增删改 |
| GET | `/goods` | 商品分页（关键字 / 分类 / 状态） |
| GET/POST/PUT/DELETE | `/goods/{id}` | 商品详情 / 新增 / 编辑 / 删除 |
| PUT | `/goods/{id}/status` | 上架 / 下架 |

小程序端（`/api`，★ 表示需要登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 微信 code 换 token |
| GET | `/home` | 首页聚合（分类 + 热销 + 新品） |
| GET | `/categories` | 分类列表 |
| GET | `/goods` | 商品列表（分类 / 关键字 / 排序） |
| GET | `/goods/{id}` | 商品详情 |
| GET | `/user/profile` ★ | 当前用户 |
| PUT | `/user/profile` ★ | 修改昵称头像 |
| GET/POST/PUT/DELETE | `/cart` ★ | 购物车列表 / 加购 / 改数量 / 勾选 / 删除 |
| GET/POST/PUT/DELETE | `/addresses` ★ | 收货地址维护 |
| POST | `/orders` ★ | 提交订单（购物车已勾选商品） |
| GET | `/orders` `/orders/{id}` ★ | 订单列表 / 详情 |
| PUT | `/orders/{id}/pay` `/orders/{id}/cancel` ★ | 模拟支付 / 取消（回滚库存） |

## 日志规范

后端日志统一由 **Logback** 管理，配置集中在两个模块各自的 `src/main/resources/logback-spring.xml`（两份内容完全一致，文件名/级别差异由 `spring.application.name` 与 profile 决定）。`application*.yml` 中不再写 `logging.*`，避免两处配置互相覆盖。

**依赖链路**：业务代码只用 SLF4J API（`@Slf4j`）→ `logback-classic 1.5.38` 落地；`log4j-to-slf4j` 只做桥接，没有 log4j2 实现，也不允许再引入其它日志门面。

**三种输出**

| 目标 | 内容 | 策略 |
| --- | --- | --- |
| 控制台 | 全量，带颜色 | 交给容器 / 本地终端 |
| `logs/{app}.log` | 全量 | 按天 + 按大小滚动，单文件 100MB，保留 30 天，总量 3GB，自动 gzip |
| `logs/{app}-error.log` | 仅 ERROR | 单文件 50MB，保留 60 天，总量 1GB，便于告警与巡检 |

`{app}` 取 `spring.application.name`，即 `easy-mall-admin` / `easy-mall-api`。日志目录默认是启动目录下的 `logs/`，可用 `--logging.file.path=/var/log/easy-mall` 或环境变量 `LOGGING_FILE_PATH` 覆盖。文件写入用 `AsyncAppender` 包一层（队列 1024，满了阻塞而不是丢日志），业务线程不被磁盘 IO 拖住。

**格式与 traceId**

```
2026-10-26 16:45:25.241 ERROR [http-nio-18082-exec-2] [req-err-0001] c.e.api.common.exception.GlobalExceptionHandler - 系统异常
```

第四段是 `TraceIdFilter` 写入 MDC 的 traceId：优先沿用请求头 `X-Request-Id`（上游网关/前端透传），没有则本地生成 16 位，并回写到响应头。同一次请求的所有日志行可按这个 id 直接串起来，用户报障时让他把响应头里的 `X-Request-Id` 发过来即可定位。

**分环境级别**

| profile | 控制台 | 文件 | `com.easymall` | 框架包 |
| --- | --- | --- | --- | --- |
| `dev` | 有 | 有 | DEBUG（含 MyBatis SQL） | Spring INFO / Security WARN |
| `test` | 有 | 不落盘 | INFO | 根 WARN |
| `prod` | 有 | 有 | INFO | Spring / Security / MyBatis 均 WARN |

**编码约定**

- 统一 `@Slf4j` + `log.xxx("模板 {}", arg)`，禁止 `System.out` 与字符串拼接。
- 异常必须把异常对象作为最后一个参数传入（`log.error("调用微信接口失败", e)`），否则丢失堆栈。
- 预期内的业务失败用 `warn`（参数错误、库存不足、登录失败），只有需要人工介入的用 `error`。
- 记录业务标识而不是整对象：`orderNo / userId / goodsId`；手机号、密码、token、openid 不落日志。

## 设计说明

- **为什么是两个模块而不是两个服务**：单体架构，admin 与 api 各自独立打包启动、独立端口、独立安全策略，但共享一个数据库和同一套表，部署时可以分别扩容，出问题时互不影响。
- **暂未抽 `easy-mall-common`**：两个模块的 DTO、鉴权方式、错误码集合差异较大，当前公共代码（Result/异常/基类）只有几百行。若后续出现大量重复实体与工具类，可新增 `easy-mall-common` 模块，两个模块加一行依赖即可。
- **MyBatis-Plus 3.5.18 的坑**：`ServiceImpl` 已移到 `com.baomidou.mybatisplus.spring.service.impl`；分页与防全表更新插件被拆到 `mybatis-plus-jsqlparser`，不引入会编译不过。
- **下单防超卖**：扣库存使用 `update ... where id=? and stock >= ?` 的条件更新，受影响行数为 0 直接抛「库存不足」；取消订单回滚库存与销量。
- **审计字段 + 逻辑删除**：`BaseEntity` 统一 `created_at / updated_at / created_by / updated_by / deleted`。时间与人员由 `MyMetaObjectHandler` 填充，创建人/修改人取当前登录态（admin 是管理员 ID，api 是小程序用户 ID），无登录态的写入（微信首次注册）留空。注意修改人不能用 `strictUpdateFill`——实体从库里查出来时字段已有值，strict 模式会跳过，必须用 `setFieldValByName` 覆盖。
- **乐观锁**：`BaseEntity.version` 加 `@Version`，两个模块都注册了 `OptimisticLockerInnerInterceptor`。`updateById` 时自动追加 `where version = ?` 并把版本 +1，受影响行数为 0 就抛 `4006 数据已被其他人修改`。管理端编辑表单（用户/分类/商品）需要把 `version` 原样回传；小程序端的资料、地址更新走「先查后改」，版本号取自库里的最新值。购物车是临时数据、物理删除，不参与乐观锁。
- **异常处理**：`@RestControllerAdvice` 统一转换；未预期异常打印完整堆栈并返回 500，不向客户端暴露内部细节。
- **小程序端为什么没用 UnoCSS**：uni-app 的小程序构建只把 `App.vue` 的样式汇总进 `app.wxss`，`import 'uno.css'` 的虚拟模块不会进入产物，页面里写的原子类会全部失效。这里改为 `styles/utilities.scss` 手写少量工具类，产物可控；如果后续要上原子化 CSS，需要用 `@unocss/cli` 预生成 CSS 或引入 `@uni-helper/unocss-preset-uni` 并自行验证产物。

## 已验证内容

`cd backend && mvn test` 全部通过（9 个用例）：

- admin：Spring 上下文启动、无 token 返回 401、admin/admin123 登录并携带 token 访问 `/auth/me`、错误密码返回 4002、乐观锁：版本号自增且过期版本号更新返回 4006
- api：Spring 上下文启动、商品列表免登录可访问、购物车未登录返回 401、模拟微信登录后可访问购物车

测试使用 H2 内存库（`src/test/resources/schema-h2.sql`），不依赖本地 MySQL。

**真实 MySQL 端到端已验证**（MySQL 8.4.13 + `easy_mall` 库）：admin 登录拿到 token 后 `/auth/me`、`/goods` 正常；api 走完「微信模拟登录 → 商品列表 → 加购 2 件 → 新增地址 → 下单 → 支付 → 订单列表」全链路，订单金额 45.80 正确，库存 400→398、销量 33→35 同步扣减，下单后购物车已清空；订单落库时把收货人/电话/地址拍成快照，不依赖地址表后续修改。

真实库上的乐观锁与审计字段也验证过：新建分类返回 `createdBy=1、version=0`；用 `version=0` 更新成功后库里变成 `version=1、updatedBy=1`；再用旧的 `version=0` 提交返回 `4006`；小程序端新增地址 `createdBy=2`、改资料后 `app_user` 的 `updatedBy=2、version=1`，数据符合预期。

日志另做了真实启动验证（`--logging.file.path` 指向临时目录）：两个服务均能正常落盘 `logs/{app}.log` 与 `logs/{app}-error.log`；带 `X-Request-Id: req-err-0001` 请求一个会抛异常的接口，错误文件里正确出现带该 traceId 的 ERROR 记录和完整堆栈；`test` profile 下不产生日志文件。

小程序端 `npx uni build -p mp-weixin` 与 `npx uni build`（H5）均已构建通过，产物分别在
`miniprogram/dist/build/mp-weixin`、`miniprogram/dist/build/h5`。
页面 UI 的最终视觉与真机交互需要在微信开发者工具中打开产物确认（tabBar 图标尚未提供，当前为纯文字）。

## 后续规划

1. Web 管理后台前端（Vue3 + Element Plus），对接 `/admin` 接口
2. 商品多规格 SKU、库存流水、优惠券与营销活动
3. 微信支付（统一下单 + 回调）、订单超时自动关闭
4. Redis 缓存热点商品、token 黑名单（强制下线）
5. 文件上传（对象存储）、商品图片管理
6. admin 端订单管理、发货、售后
7. 小程序 tabBar 图标替换为正式设计稿
