# easy-mall 微商城

一个前后端分离的微商城项目，包含 **Web 管理后台**（运营端）与 **微信小程序商城**（用户端），两个后端服务共享一个 MySQL 库。

- 后端：Spring Boot 4.1.2 单体应用（Maven 父工程 + 2 个可独立启动的模块）
- 管理后台前端：Vben Admin v5.7.0（web-ele）· Vue 3 · Element Plus · 动态菜单 + RBAC
- 小程序端：uni-app + Vue3 + TypeScript + Vite + Pinia + SCSS
- 数据库：MySQL 8（两个后端服务共用同一个库）

## 功能总览

**管理后台**（按菜单分组，菜单与路由由后端动态下发，按角色授权）

| 一级菜单 | 功能 | 说明 |
| --- | --- | --- |
| 工作台 | 分析页 / 工作台 | 今日订单、今日销售额（含较昨日环比，涨红跌绿）、待发货、库存预警等实时经营指标 |
| 商品管理 | 商品列表 / 商品分类 / 品牌管理 / 库存管理 | 商品上下架、分类树、品牌（重名与删除保护）、库存调整（原子防超卖）与流水 |
| 订单管理 | 订单列表 / 待发货 / 售后退款 / 评价管理 | 订单详情与关闭、发货（乐观锁防并发）、售后审核→确认退款（订单售后状态联动）、评价显示/隐藏/回复/删除 |
| 会员管理 | 会员列表 / 等级与权益 / 积分管理 / 标签与分群 | 会员四维筛选（关键词/等级/**来源渠道**（公众号/小程序/H5 扫码）/标签）、积分调整与流水、等级折扣与成长值门槛、标签打标与分群 |
| 营销中心 | 优惠券 / 拼团秒杀 / 签到管理 / 满减活动 | 满减券/折扣券/无门槛券（固定日期或领取后 N 天有效、限领、领取/核销记录）、秒杀与拼团活动（成团人数与时长、已售/库存）、签到规则（每日积分 + 连续签到额外奖励）与签到记录、阶梯满减与每满减 |
| 商城装修 | 页面装修（可视化拖拽编辑器） | **三栏设计器**（组件库 / 手机画布 / 属性面板）：拖拽插入与排序、选中高亮、显示隐藏、复制删除、**撤销/重做**（⌘Z / ⇧⌘Z）；右侧改配置画布即时生效，草稿防抖自动保存；12 个内置组件（搜索框、公告、轮播、单图、图标导航、标题、富文本、商品列表、限时秒杀、优惠券、辅助空白、店铺信息）；发布生成快照，**真机扫码预览**（免登录预览页 + 二维码） |
| 系统管理 | 部门 / 用户 / 角色 / 菜单 | 组织架构、账号管理、RBAC 角色授权、菜单维护（登录后 `/system/menu/routes` 动态下发路由，按钮级权限走 `/user/codes`） |

**小程序商城**：微信登录（code 换 token，本地可模拟）、首页聚合、分类/商品列表与详情、购物车、收货地址、下单（拍库存快照防超卖）、模拟支付、取消订单（回滚库存）、订单列表/详情。

## 目录结构

```
easy-mall/
├── backend/                     # 后端 Maven 父工程
│   ├── pom.xml                  # Java 25 / Spring Boot 4.1.2 / Lombok
│   ├── easy-mall-admin/         # 管理后台接口服务，端口 8081，context-path=/admin
│   └── easy-mall-api/           # 小程序接口服务，端口 8082，context-path=/api
├── web-admin/                   # 管理后台前端（Vben Admin 5 · web-ele），见 web-admin/README.md
├── miniprogram/                 # 小程序端（uni-app）
└── db/
    ├── easy_mall.sql            # 建库建表 + 演示数据（基础版）
    └── migrations/              # 增量迁移脚本（全部幂等，可重复执行）
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
    ├── auth           # 登录（admin 含 /user/info、/user/codes）
    ├── system         # 部门/用户/角色/菜单 + 动态路由（仅 admin）
    ├── dashboard      # 经营统计（仅 admin）
    ├── goods          # 商品/分类/品牌/库存（仅 admin）
    ├── order          # 订单/售后/评价（admin 为管理，api 为交易）
    ├── member         # 会员/等级/积分/标签（仅 admin）
    ├── marketing      # 优惠券/拼团秒杀/签到/满减（仅 admin）
    ├── decorate       # 商城装修：页面 CRUD/发布/扫码预览（仅 admin）
    ├── user           # 小程序用户（仅 api）
    ├── cart           # 购物车（仅 api）
    └── address        # 收货地址（仅 api）
```

`entity` / `mapper` 平铺在模块根下，是因为它们属于数据层、天然跨业务复用（分类被商品和购物车同时用到，订单要读商品快照）；按业务切分会逼出无意义的跨模块依赖。业务规则仍然收在 `modules/<biz>/service` 里。

## 数据库表结构

表名按模块划分前缀：**订单 `o_`、会员 `m_`、营销 `mk_`、装修 `dc_`、商品 `mall_`、系统 `sys_`**。

| 模块 | 表 |
| --- | --- |
| 系统 | sys_user、sys_dept、sys_role、sys_menu、sys_user_role、sys_role_menu |
| 商品 | mall_goods、mall_category、mall_brand、mall_stock_log |
| 订单 | o_order、o_order_item、o_order_refund、o_order_comment |
| 会员 | m_member、m_member_level、m_points_log、m_member_tag、m_member_user_tag |
| 营销 | mk_coupon、mk_coupon_user、mk_promotion、mk_signin_rule、mk_signin_log、mk_reduction |
| 装修 | dc_page（草稿/发布双 JSON 快照 + 预览码） |
| 小程序 | cart_item、user_address |

增量迁移脚本（老库按序执行即可，全部幂等）：

| 脚本 | 内容 |
| --- | --- |
| `20261026_add_audit_columns.sql` | 全表补 created_by / updated_by / version 审计字段 |
| `20261027_system_module.sql` | 系统管理五张表 + 种子数据 |
| `20261028_dynamic_menu.sql` | 工作台菜单入库并授权 |
| `20261029_goods_module.sql` | 品牌/库存表、商品管理菜单 |
| `20261030_order_module.sql` | 订单发货/售后字段、售后/评价表、订单管理菜单 |
| `20261031_member_module.sql` | 会员等级/积分/标签表、会员字段、会员管理菜单 |
| `20261101_rename_tables.sql` | 表名规范化（订单 o_、会员 m_ 前缀） |
| `20261102_marketing_module.sql` | 优惠券/领券/拼团秒杀/签到/满减表、营销中心菜单 |
| `20261103_decorate_module.sql` | 装修页面表、商城装修菜单、演示首页 |

## 技术栈与版本

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 25 | `maven.compiler.release=25` |
| Spring Boot | 4.1.2 | 单体，无微服务、无注册中心 |
| Lombok | 由 Boot BOM 管理 | 实体 / DTO / 配置类 |
| MyBatis-Plus | 3.5.18 | `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser` |
| Spring Security | 7.x（随 Boot） | 仅 admin 模块使用 |
| JJWT | 0.13.0 | HS256，admin token 12h / api token 7d，密钥独立 |
| SpringDoc OpenAPI | 3.2.0 | Swagger UI |
| Vben Admin | 5.7.0 | web-ele 应用，Vite 8 + pnpm 10（`packageManager` 锁定） |
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
# 再按序执行全部迁移
for f in db/migrations/*.sql; do mysql -uroot -p easy_mall < "$f"; done
```

基础脚本会创建 `easy_mall` 库并写入演示数据；迁移补齐系统管理、动态菜单、商品/订单/会员模块与菜单授权。默认管理员：**admin / admin123**。

> `easy_mall.sql` 开头是 `DROP TABLE IF EXISTS`，**重复执行会清空数据**，只适合初始化；有数据的库只跑 `db/migrations/` 增量脚本。

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

### 3.5 启动 Web 管理后台

```bash
cd web-admin
corepack pnpm install   # 首次（根 package.json 锁定 pnpm@10.33.4）
pnpm dev                # http://localhost:5777，默认账号 admin / admin123
```

dev 模式下 `/admin` 请求由 Vite 代理转发到 `http://127.0.0.1:8081`，需要先启动 `easy-mall-admin`。裁剪与对接细节见 `web-admin/README.md`。

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
{ "code": 0, "message": "操作成功", "data": {} }
```

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 401 / 403 / 500 | 未登录 / 无权限 / 系统异常 |
| 4xxx | 业务错误（4001 商品不存在、4003 库存不足、4006 数据冲突、4010 重名冲突…） |

**编辑带版本号（乐观锁）**

返回体里的实体都会带审计字段：`createdAt / updatedAt / createdBy / updatedBy / version`。
编辑类接口（`/system/user`、`/categories`、`/goods`、`/member-levels` 等 PUT）需要把详情里拿到的 `version` 原样回传：

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
| GET | `/user/info` `/user/codes` | 当前用户信息 / 按钮权限码（RBAC） |
| GET | `/dashboard/overview` | 经营指标（今日订单/销售额 + 较昨日环比、待发货、库存预警等） |
| GET | `/system/menu/routes` | ★ 动态菜单：登录后按角色下发 vben 路由树 |
| GET/POST/PUT/DELETE | `/system/dept` | 部门管理 |
| GET/POST/PUT/DELETE | `/system/user` | 账号管理（含重置密码、启停用、分配角色） |
| GET/POST/PUT/DELETE | `/system/role` | 角色管理（含授权菜单、启停用） |
| GET/POST/PUT/DELETE | `/system/menu` | 菜单管理（含按用户过滤、名称/路径查重） |
| GET | `/categories` | 分类树 |
| POST/PUT/DELETE | `/categories/{id}` | 分类增删改 |
| GET | `/goods` | 商品分页（关键字 / 分类 / 品牌 / 状态） |
| GET/POST/PUT/DELETE | `/goods/{id}` | 商品详情 / 新增 / 编辑 / 删除 |
| PUT | `/goods/{id}/status` | 上架 / 下架 |
| GET | `/brands` | 品牌分页 |
| POST/PUT/DELETE | `/brands/{id}` | 品牌增删改（重名 / 被引用删除保护） |
| POST | `/stock/adjust` | 库存调整（入库/出库/盘点，原子防超卖） |
| GET | `/stock/logs` | 库存流水 |
| GET | `/orders` | 订单分页（订单号/收货人/手机号、状态、售后状态、日期区间） |
| GET | `/orders/{id}` | 订单详情（含商品明细） |
| PUT | `/orders/{id}/deliver` | 发货（仅待发货，乐观锁） |
| PUT | `/orders/{id}/close` | 关闭订单（进行中售后时拒绝） |
| GET | `/refunds` | 售后分页 |
| PUT | `/refunds/{id}/audit` | 售后审核（同意/拒绝，联动订单售后状态） |
| PUT | `/refunds/{id}/confirm` | 确认退款 |
| GET | `/comments` | 评价分页 |
| PUT/DELETE | `/comments/{id}` | 评价显示/隐藏、商家回复、删除 |
| GET | `/members` | 会员分页（关键词/等级/**来源渠道**/标签筛选） |
| GET | `/members/{id}` | 会员详情（聚合等级、标签） |
| PUT | `/members/{id}/points` | 调整积分（余额校验 + 流水留痕） |
| PUT | `/members/{id}/tags` | 打标签（全量覆盖） |
| GET | `/points` | 积分流水分页 |
| GET/POST/PUT/DELETE | `/member-levels` | 等级 CRUD（重名校验、删除保护）+ 选项 |
| GET/POST/PUT/DELETE | `/member-tags` | 标签 CRUD（删除级联解除关联）+ 选项（含会员数） |

小程序端（`/api`，★ 表示需要登录）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 微信 code 换 token |
| GET | `/home` | 首页聚合（分类 + 热销 + 新品） |
| GET | `/categories` | 分类列表 |
| GET | `/goods` | 商品列表（分类 / 关键字 / 排序） |
| GET | `/goods/{id}` | 商品详情 |
| GET/PUT | `/user/profile` ★ | 当前用户 / 修改昵称头像 |
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
- **动态菜单 + RBAC**：菜单、角色、授权关系全部入库；登录后前端调 `GET /system/menu/routes` 获取按角色过滤的 vben 路由树（`accessMode: 'backend'`），按钮级权限码走 `/user/codes`。菜单树构建时自动补齐祖先链、外链映射 IFrameView、按钮类型不生成路由。JWT 中携带 `roles` claim，服务端可校验角色。
- **库存与订单一致性**：扣库存使用 `update ... where id=? and stock >= ?` 的条件更新（下单出库、盘点同理），受影响行数为 0 直接抛「库存不足」；取消订单回滚库存与销量；每次库存变动写 `mall_stock_log` 流水。发货、积分调整等写操作均基于乐观锁防并发。
- **订单金额快照**：订单落库时把收货人/电话/地址与商品单价拍成快照，不依赖地址表、商品表后续修改。
- **售后状态机**：申请（0 待审核）→ 审核同意（1 待退款）/ 拒绝（2）→ 确认退款（3 已退款）；审核与确认退款同步联动订单上的售后状态，操作人与时间落库。
- **会员模型**：`m_member` 承载等级 / 积分 / 来源渠道 / 累计消费与订单数（按已付款订单聚合）；积分调整带余额校验、乐观锁，并写 `m_points_log` 流水（含操作人）；标签与会员多对多，删除标签级联解除关联。
- **审计字段 + 逻辑删除**：`BaseEntity` 统一 `created_at / updated_at / created_by / updated_by / deleted`。时间与人员由 `MyMetaObjectHandler` 填充，创建人/修改人取当前登录态（admin 是管理员 ID，api 是小程序用户 ID），无登录态的写入（微信首次注册）留空。注意修改人不能用 `strictUpdateFill`——实体从库里查出来时字段已有值，strict 模式会跳过，必须用 `setFieldValByName` 覆盖。
- **乐观锁**：`BaseEntity.version` 加 `@Version`，两个模块都注册了 `OptimisticLockerInnerInterceptor`。`updateById` 时自动追加 `where version = ?` 并把版本 +1，受影响行数为 0 就抛 `4006 数据已被其他人修改`。管理端编辑表单需要把 `version` 原样回传；小程序端的资料、地址更新走「先查后改」，版本号取自库里的最新值。购物车是临时数据、物理删除，不参与乐观锁。
- **异常处理**：`@RestControllerAdvice` 统一转换；未预期异常打印完整堆栈并返回 500，不向客户端暴露内部细节。
- **小程序端为什么没用 UnoCSS**：uni-app 的小程序构建只把 `App.vue` 的样式汇总进 `app.wxss`，`import 'uno.css'` 的虚拟模块不会进入产物，页面里写的原子类会全部失效。这里改为 `styles/utilities.scss` 手写少量工具类，产物可控；如果后续要上原子化 CSS，需要用 `@unocss/cli` 预生成 CSS 或引入 `@uni-helper/unocss-preset-uni` 并自行验证产物。

## 已验证内容

`cd backend && mvn test` 全部通过（admin 12 个 + api 4 个用例，H2 内存库，不依赖本地 MySQL）：

- admin：Spring 上下文启动、无 token 401、登录与 `/auth/me`、错误密码 4002、乐观锁 4006、品牌重名/删除保护、库存不足拒绝与盘点流水、订单发货流程、售后审核联动订单状态、评价显隐与回复、经营统计口径、会员渠道/标签筛选与积分调整
- api：Spring 上下文启动、商品列表免登录可访问、购物车未登录 401、模拟微信登录后可访问购物车

**真实 MySQL 端到端已验证**（MySQL 8.4 + `easy_mall` 库）：

- 小程序全链路：微信模拟登录 → 商品列表 → 加购 → 新增地址 → 下单 → 支付 → 订单列表，金额与库存/销量扣减正确，购物车自动清空
- 管理端：系统管理（建部门→建角色并授权→建用户绑角色→新用户登录后菜单与权限码正确）、动态菜单（不同角色侧边栏不同）、商品/品牌/库存增删改与防超卖、订单发货/重复发货拒绝、售后审核→确认退款联动、积分调整与超额拒绝、等级删除保护、未登录访问一律 401

日志另做了真实启动验证：`logs/{app}.log` 与 `logs/{app}-error.log` 正常落盘滚动，`X-Request-Id` 透传生效，`test` profile 不产生日志文件。小程序端 mp-weixin 与 H5 构建均通过。

## 后续规划

1. 商品多规格 SKU、优惠券与营销活动
2. 微信支付（统一下单 + 回调）、订单超时自动关闭
3. Redis 缓存热点商品、token 黑名单（强制下线）
4. 文件上传（对象存储）、商品图片管理
5. 服务端权限闭环：`/system/*` 等读接口补 `@PreAuthorize`，按钮权限码纳入 JWT authorities
6. 小程序 tabBar 图标替换为正式设计稿、真实微信支付接入
