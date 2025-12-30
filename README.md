# 外卖订餐系统（Spring Boot + Vue 3）

本项目实现一个多角色外卖订餐系统，用于《软件设计与编程实践》课程实验/答辩演示。系统围绕“饭店上架菜品 → 食客下单支付 → 骑手抢单/派单配送 → 完成/评价 → 管理员统计对账”这一主流程，提供平台端、商家端、食客端、骑手端的完整功能闭环。

系统设置四类账号（角色）：平台管理员（ADMIN）、饭店商家（RESTAURANT）、食客（CUSTOMER）、骑手（RIDER）。每个角色在前端有对应页面入口，后端以不同接口前缀提供相应能力，并在关键动作（支付、取消、退款、派单、完成配送）处对状态与权限进行校验，避免“重复退款/越权改状态/已退款仍可配送”等异常数据。

为了便于演示“按距离和负载分配骑手”，项目支持百度地图坐标选点与（可选）路网距离计算：前端在下单时可选真实地点并提交坐标，骑手可上报当前位置；后端在派单/路线展示时优先用百度路网距离，未配置 AK 时自动回退直线距离估算，保证离线可演示。

## 技术栈

后端使用 Spring Boot 3.3.x + Spring Web + Spring Data JPA，数据库使用 MySQL（开发环境），JDK 版本为 Java 17。前端使用 Vue 3 + TypeScript + Vite + Vue Router。项目默认后端端口为 `8081`，前端开发服务器端口为 `5173`。

## 目录结构（仓库级）

`takeout-backend/` 是后端工程，包含实体模型、仓库、业务服务与接口控制器。`takeout-frontend/` 是前端工程，包含路由、布局与各角色页面。`soybean-admin-main/` 为参考的后台管理系统界面风格工程（用于学习布局与交互风格，不直接参与运行）。

## 运行环境

需要 JDK 17、Maven 3.8+、Node.js 18+（建议 18/20）、MySQL 8.x。默认数据库连接配置为 MySQL `root/3`，数据库名为 `takeout`。

## 快速开始（本机开发）

### 1）准备 MySQL

先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS takeout DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

后端配置在 `takeout-backend/src/main/resources/application.yml`，包含 MySQL 连接与项目端口。后端开启 `spring.jpa.hibernate.ddl-auto=update`，首次启动会自动建表/补列（只需确保数据库存在即可）。

### 2）启动后端（8081）

在 `takeout-backend/` 目录执行：

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn -DskipTests package
java -jar target/takeout-backend-0.0.1-SNAPSHOT.jar
```

启动成功后访问 `http://localhost:8081/api/health`，应返回 `{"status":"OK"}`。

### 3）启动前端（5173）

在 `takeout-frontend/` 目录执行：

```bash
npm install
npm run dev
```

前端默认调用 `http://localhost:8081` 的后端接口。

## 关键配置说明

### 配送分配模式（抢单 / 自动派单）

在 `takeout-backend/src/main/resources/application.yml` 中，通过 `delivery.assignment.mode` 控制：

`HALL` 表示抢单模式：订单支付后进入“接单大厅”，等待骑手抢单；`AUTO` 表示自动派单：订单支付后系统按“距离 + 负载”自动分配骑手。两种模式均可用于答辩演示，推荐默认用 `HALL`，便于演示骑手抢单流程。

### 订单超时策略（自动取消 / 自动派单 / 自动退款）

在 `takeout-backend/src/main/resources/application.yml` 中，通过 `order.timeout.*` 控制。未支付超时自动取消默认开启；已支付但无人抢单可选“超时自动派单”；已支付但长期无人接单可选“超时自动取消并退款”（默认关闭，按需打开）。

### 百度地图 AK（可选）

前端选点/定位转换通过 `takeout-frontend/.env` 的 `VITE_BAIDU_MAP_AK` 配置。后端路网距离/地理编码通过环境变量 `BAIDU_MAP_AK` 配置（示例：PowerShell 执行 `$env:BAIDU_MAP_AK="你的AK"` 后再启动后端）。未配置 AK 时，后端会自动回退直线距离估算，不影响核心流程演示。

## 数据初始化（示例 SQL）

本项目不强制内置固定默认账号，你可以用 SQL 初始化第一个管理员、骑手、饭店、食客账号。下面示例仅用于开发环境测试，用户名/密码可自行调整。

```sql
-- 管理员
INSERT INTO admins(username, password, role, status)
VALUES ('admin', 'admin123', 'SUPER_ADMIN', 'ACTIVE');

-- 骑手 + 骑手登录账号
INSERT INTO delivery_staff(name, phone, status, current_load)
VALUES ('骑手1', '13900000001', 'ACTIVE', 0);
SET @sid = LAST_INSERT_ID();
INSERT INTO delivery_staff_users(delivery_staff_id, username, password, status)
VALUES (@sid, 'rider1', '123456', 'ACTIVE');

-- 饭店 + 饭店登录账号
INSERT INTO restaurants(name, address, phone, status, commission_rate, created_at)
VALUES ('幸福小馆', '天津市xx路xx号', '13900000011', 'ACTIVE', 10.00, NOW());
SET @rid = LAST_INSERT_ID();
INSERT INTO restaurant_users(restaurant_id, username, password, status)
VALUES (@rid, 'fd', '123456', 'ACTIVE');

-- 食客
INSERT INTO customers(username, password, real_name, phone, status, created_at)
VALUES ('jin', '123456', '金同学', '13900000000', 'ACTIVE', NOW());
```

## 功能概览（按角色）

管理员端可以管理饭店/食客/骑手信息与账号状态、配置平台抽成、查看订单详情、对账支付/退款流水并查看统计报表。商家端可以维护菜品分类与菜品、设置优惠活动、接单与查看销售统计。食客端支持注册登录、地址簿、浏览饭店菜品、下单支付、取消/退款与评价。骑手端支持接单大厅抢单、配送与完成、上报位置、查看路线距离与个人工作台数据。

## 代码文件说明（逐文件）

本节对“本项目主要源码文件”逐一说明每个文件包含的内容与作用，便于检查工程结构与答辩讲解。

### 后端（`takeout-backend/src/main/java/com/example/takeout`）

**启动与配置**

- `takeout-backend/src/main/java/com/example/takeout/TakeoutApplication.java`：Spring Boot 启动入口，启用 JPA 扫描与定时任务调度。
- `takeout-backend/src/main/java/com/example/takeout/config/WebCorsConfig.java`：CORS 跨域配置，允许前端开发端口访问后端接口。

**实体（`entity/`，对应数据库表）**

- `takeout-backend/src/main/java/com/example/takeout/entity/Admin.java`：管理员账号实体（用户名/密码/角色/状态）。
- `takeout-backend/src/main/java/com/example/takeout/entity/Customer.java`：食客账号实体（实名信息、手机号、状态等）。
- `takeout-backend/src/main/java/com/example/takeout/entity/Restaurant.java`：饭店实体（名称/地址/电话/状态/抽成/可选坐标）。
- `takeout-backend/src/main/java/com/example/takeout/entity/RestaurantUser.java`：饭店登录账号，与 `Restaurant` 关联。
- `takeout-backend/src/main/java/com/example/takeout/entity/DeliveryStaff.java`：骑手实体（姓名/电话/状态/负载/当前位置坐标/更新时间）。
- `takeout-backend/src/main/java/com/example/takeout/entity/DeliveryStaffUser.java`：骑手登录账号，与 `DeliveryStaff` 关联。
- `takeout-backend/src/main/java/com/example/takeout/entity/DishCategory.java`：菜品分类实体（饭店下的分类）。
- `takeout-backend/src/main/java/com/example/takeout/entity/Dish.java`：菜品实体（名称、价格、状态、归属饭店与分类）。
- `takeout-backend/src/main/java/com/example/takeout/entity/CustomerOrder.java`：订单主表（状态、支付状态、金额、抽成、收货信息、坐标、关联骑手等）。
- `takeout-backend/src/main/java/com/example/takeout/entity/OrderItem.java`：订单明细表（菜品、单价、数量）。
- `takeout-backend/src/main/java/com/example/takeout/entity/PaymentLog.java`：支付/退款流水表（PAY/REFUND，便于管理员对账）。
- `takeout-backend/src/main/java/com/example/takeout/entity/RestaurantPromotion.java`：饭店优惠活动（如满减）实体。
- `takeout-backend/src/main/java/com/example/takeout/entity/RestaurantRating.java`：食客对饭店的评价记录。
- `takeout-backend/src/main/java/com/example/takeout/entity/DeliveryRating.java`：食客对骑手的评价记录。
- `takeout-backend/src/main/java/com/example/takeout/entity/CustomerAddress.java`：食客地址簿（含默认地址能力）。
- `takeout-backend/src/main/java/com/example/takeout/entity/Address.java`：地址实体（用于地址簿/订单收货信息的结构化存储）。
- `takeout-backend/src/main/java/com/example/takeout/entity/PlatformConfig.java`：平台配置（默认抽成比例等）。
- `takeout-backend/src/main/java/com/example/takeout/entity/RestaurantApplication.java`：饭店入驻/认证申请实体（管理员审核使用）。

**仓库（`repository/`，JPA 数据访问层）**

- `takeout-backend/src/main/java/com/example/takeout/repository/AdminRepository.java`：管理员登录与查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/CustomerRepository.java`：食客登录与查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/RestaurantRepository.java`：饭店查询（含公开展示的 ACTIVE 饭店）。
- `takeout-backend/src/main/java/com/example/takeout/repository/RestaurantUserRepository.java`：饭店账号登录与查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/DeliveryStaffRepository.java`：骑手查询（含可派单骑手列表）。
- `takeout-backend/src/main/java/com/example/takeout/repository/DeliveryStaffUserRepository.java`：骑手账号登录与查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/DishRepository.java`：菜品 CRUD 与查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/DishCategoryRepository.java`：菜品分类 CRUD 与查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/CustomerOrderRepository.java`：订单列表/统计投影查询、骑手大厅查询、超时订单查询等。
- `takeout-backend/src/main/java/com/example/takeout/repository/OrderItemRepository.java`：订单明细查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/PaymentLogRepository.java`：支付/退款流水查询。
- `takeout-backend/src/main/java/com/example/takeout/repository/RestaurantPromotionRepository.java`：优惠活动查询（含生效时间筛选）。
- `takeout-backend/src/main/java/com/example/takeout/repository/RestaurantRatingRepository.java`：饭店评价查询（管理员/商家筛选）。
- `takeout-backend/src/main/java/com/example/takeout/repository/DeliveryRatingRepository.java`：骑手评价查询（管理员/骑手筛选）。
- `takeout-backend/src/main/java/com/example/takeout/repository/CustomerAddressRepository.java`：地址簿 CRUD（含默认地址逻辑支持）。
- `takeout-backend/src/main/java/com/example/takeout/repository/AddressRepository.java`：地址实体数据访问。
- `takeout-backend/src/main/java/com/example/takeout/repository/PlatformConfigRepository.java`：平台抽成等全局配置读取。
- `takeout-backend/src/main/java/com/example/takeout/repository/RestaurantApplicationRepository.java`：饭店入驻申请查询与审核。

**服务（`service/`，业务逻辑层）**

- `takeout-backend/src/main/java/com/example/takeout/service/DeliveryAssignmentService.java`：派单算法（距离 + 负载），支持百度路网距离（可选）与直线距离回退。
- `takeout-backend/src/main/java/com/example/takeout/service/BaiduMapService.java`：封装百度地理编码与路网距离接口，提供回退估算方法。
- `takeout-backend/src/main/java/com/example/takeout/service/OrderAutoCloseService.java`：订单超时治理（未支付自动取消、已支付无人抢单自动派单、可选自动取消并退款）。
- `takeout-backend/src/main/java/com/example/takeout/service/RestaurantAdminService.java`：饭店相关后台管理业务（含级联删除等）。

**接口（`web/`，REST Controller 层）**

- `takeout-backend/src/main/java/com/example/takeout/web/HealthController.java`：健康检查接口 `/api/health`。
- `takeout-backend/src/main/java/com/example/takeout/web/AuthController.java`：统一登录接口 `/api/auth/login`，支持多角色登录与状态校验。
- `takeout-backend/src/main/java/com/example/takeout/web/PublicBrowseController.java`：公开浏览（展示可下单饭店/菜品等）。
- `takeout-backend/src/main/java/com/example/takeout/web/CustomerRegistrationController.java`：食客注册接口。
- `takeout-backend/src/main/java/com/example/takeout/web/CustomerProfileController.java`：食客个人信息与改密码接口。
- `takeout-backend/src/main/java/com/example/takeout/web/CustomerAddressController.java`：食客地址簿接口（含默认地址）。
- `takeout-backend/src/main/java/com/example/takeout/web/CustomerOrderController.java`：食客下单/支付/取消/退款/我的订单详情等接口。
- `takeout-backend/src/main/java/com/example/takeout/web/CustomerStatsController.java`：食客个人订单统计接口。
- `takeout-backend/src/main/java/com/example/takeout/web/RiderLocationController.java`：骑手上报当前位置接口。
- `takeout-backend/src/main/java/com/example/takeout/web/RiderOrderController.java`：骑手端订单列表/抢单大厅/抢单/路线/工作台/完成配送等接口。
- `takeout-backend/src/main/java/com/example/takeout/web/MerchantCatalogController.java`：商家端菜品分类/菜品管理接口。
- `takeout-backend/src/main/java/com/example/takeout/web/MerchantPromotionController.java`：商家端优惠活动管理接口。
- `takeout-backend/src/main/java/com/example/takeout/web/MerchantRatingController.java`：商家端评价查看接口。
- `takeout-backend/src/main/java/com/example/takeout/web/MerchantStatsController.java`：商家端统计接口。
- `takeout-backend/src/main/java/com/example/takeout/web/RestaurantOnboardingController.java`：饭店入驻申请提交接口。
- `takeout-backend/src/main/java/com/example/takeout/web/RestaurantApplicationAdminController.java`：管理员审核入驻申请接口。
- `takeout-backend/src/main/java/com/example/takeout/web/RestaurantController.java`：管理员饭店信息 CRUD 接口。
- `takeout-backend/src/main/java/com/example/takeout/web/RestaurantUserAdminController.java`：管理员饭店账号管理接口。
- `takeout-backend/src/main/java/com/example/takeout/web/DeliveryStaffAdminController.java`：管理员骑手信息管理接口。
- `takeout-backend/src/main/java/com/example/takeout/web/DeliveryStaffUserAdminController.java`：管理员骑手账号管理接口。
- `takeout-backend/src/main/java/com/example/takeout/web/CustomerAdminController.java`：管理员食客信息管理与启用/禁用接口。
- `takeout-backend/src/main/java/com/example/takeout/web/PlatformConfigController.java`：平台抽成配置接口。
- `takeout-backend/src/main/java/com/example/takeout/web/PaymentLogController.java`：支付/退款流水查询接口。
- `takeout-backend/src/main/java/com/example/takeout/web/RatingController.java`：食客提交评价接口。
- `takeout-backend/src/main/java/com/example/takeout/web/AdminRatingController.java`：管理员评价管理与筛选接口。
- `takeout-backend/src/main/java/com/example/takeout/web/OrderController.java`：后台订单查询/详情/状态维护/派单接口（含状态机校验与取消自动退款）。
- `takeout-backend/src/main/java/com/example/takeout/web/StatsController.java`：平台统计相关接口。
- `takeout-backend/src/main/java/com/example/takeout/web/AdminDashboardController.java`：管理员仪表盘数据接口。

### 前端（`takeout-frontend/src`）

前端采用单页应用结构，路由集中在 `main.ts` 中配置。页面以角色为单位组织在 `views/` 目录，通用组件放在 `components/`，布局放在 `layouts/`。

**入口与全局样式**

- `takeout-frontend/src/main.ts`：创建 Vue 应用与路由，定义各角色入口路径（如 `/` 管理后台、`/c` 食客端、`/d` 骑手端）。
- `takeout-frontend/src/App.vue`：根组件，承载路由视图。
- `takeout-frontend/src/style.css`：全局样式（基础布局与表格/按钮等）。
- `takeout-frontend/src/assets/vue.svg`：前端模板自带静态资源图标（可用于占位或演示）。

**布局与通用组件**

- `takeout-frontend/src/layouts/BasicLayout.vue`：后台管理布局（侧边栏 + 顶部 + 内容区），用于管理员/商家管理页面。
- `takeout-frontend/src/components/BaiduLocationPicker.vue`：百度地图选点组件（地图点选/搜索/定位），用于食客下单选择真实地点与坐标。
- `takeout-frontend/src/components/HelloWorld.vue`：模板组件（可保留用于演示/占位）。

**管理员/后台页面（`views/`）**

- `takeout-frontend/src/views/LoginView.vue`：统一登录页（支持角色切换登录）。
- `takeout-frontend/src/views/DashboardView.vue`：后台首页/仪表盘入口页。
- `takeout-frontend/src/views/OrdersView.vue`：后台订单管理（列表、详情、派单、状态维护、退款标识等）。
- `takeout-frontend/src/views/RestaurantListView.vue`：饭店管理（CRUD、抽成比例展示等）。
- `takeout-frontend/src/views/CustomerListView.vue`：食客管理（启用/禁用等）。
- `takeout-frontend/src/views/RestaurantUsersView.vue`：饭店账号管理。
- `takeout-frontend/src/views/DeliveryStaffView.vue`：骑手信息管理。
- `takeout-frontend/src/views/DeliveryStaffUsersView.vue`：骑手账号管理。
- `takeout-frontend/src/views/RestaurantApplicationsView.vue`：饭店入驻申请审核页面。
- `takeout-frontend/src/views/CommissionConfigView.vue`：平台抽成配置页面。
- `takeout-frontend/src/views/PlatformStatsView.vue`：平台统计报表页面。
- `takeout-frontend/src/views/PaymentLogsView.vue`：支付/退款流水查询页面（对账）。
- `takeout-frontend/src/views/RatingAdminView.vue`：评价管理页面（饭店/骑手评价）。

**商家页面（`views/merchant/`）**

- `takeout-frontend/src/views/merchant/MerchantDishesView.vue`：商家菜品与分类管理。
- `takeout-frontend/src/views/merchant/MerchantPromotionsView.vue`：商家优惠活动管理。
- `takeout-frontend/src/views/merchant/MerchantStatsView.vue`：商家销售统计与热销排行。

**食客页面（`views/customer/`）**

- `takeout-frontend/src/views/customer/CustomerRegisterView.vue`：食客注册页。
- `takeout-frontend/src/views/customer/CustomerHomeView.vue`：食客点餐主页（饭店/菜品浏览、下单、地图选点）。
- `takeout-frontend/src/views/customer/CustomerOrdersView.vue`：食客订单列表与详情（含退款时间/取消原因展示等）。
- `takeout-frontend/src/views/customer/CustomerAddressesView.vue`：地址簿管理（含默认地址）。
- `takeout-frontend/src/views/customer/CustomerProfileView.vue`：个人信息与改密码。
- `takeout-frontend/src/views/customer/CustomerStatsView.vue`：个人订单统计页面。

**骑手页面（`views/rider/`）**

- `takeout-frontend/src/views/rider/RiderHallView.vue`：接单大厅（显示未分配已支付订单、抢单、自动刷新、上报位置）。
- `takeout-frontend/src/views/rider/RiderOrdersView.vue`：骑手订单工作台（配送中/已完成列表、订单详情、路线距离展示、上报位置）。

## 配置与构建文件说明（非源码但必需）

这些文件不属于业务“源码”，但决定项目如何构建、如何启动以及如何在本机开发环境下运行。

**后端**

- `takeout-backend/pom.xml`：Maven 构建描述文件（Java 版本、Spring Boot 版本、依赖与插件配置）。
- `takeout-backend/src/main/resources/application.yml`：后端运行配置（端口、MySQL、JPA、日志、百度 AK、派单/超时策略等）。

**前端**

- `takeout-frontend/package.json`：前端依赖与脚本（`dev/build/preview`）。
- `takeout-frontend/package-lock.json`：npm 依赖锁定文件，保证依赖版本一致。
- `takeout-frontend/vite.config.ts`：Vite 构建/开发服务器配置（如代理、HMR 等）。
- `takeout-frontend/index.html`：Vite SPA 入口 HTML。
- `takeout-frontend/tsconfig.json`、`takeout-frontend/tsconfig.app.json`、`takeout-frontend/tsconfig.node.json`：TypeScript 编译配置。
- `takeout-frontend/.env`：前端环境变量（例如 `VITE_BAIDU_MAP_AK`），仅用于本机开发与构建。

## 常见问题

如果前端出现 CORS 报错，请确认后端已启动在 `8081` 且 `WebCorsConfig` 已生效。若 Maven `clean/package` 失败且提示无法删除/重命名 jar，通常是后端 jar 仍在运行占用文件，先结束相关 Java 进程后再重新构建。若抢单大厅看不到订单，请检查 `delivery.assignment.mode` 是否为 `HALL`（自动派单模式 `AUTO` 会直接分配骑手，订单不会出现在大厅）。

## 微信支付（可选，扫码支付）

项目已接入微信支付 V3 的 **Native（扫码）** 流程：食客下单后可选择“微信支付（扫码）”，前端弹出二维码；支付成功后由微信回调通知后端，后端将订单更新为 `PAID`。

说明：本项目的“取消/退款”目前仍为模拟退款流水（未对接微信退款接口）。

## 无商户号：微信支付（模拟扫码）

如果你没有微信支付商户号（例如课程作业/演示环境），可以使用“微信支付（模拟扫码）”来演示扫码闭环：前端弹出二维码，手机扫码后打开一个确认页，点击“确认支付”后端会把订单更新为 `PAID`。

**关键点**：手机扫码访问的 URL 不能是 `localhost`，需要配置后端对外可访问的地址（同一 WiFi 下用电脑局域网 IP 即可）：

```powershell
$env:APP_PUBLIC_BASE_URL = 'http://192.168.1.10:8081'
```

**开启方式（本机开发）**

1. 在微信支付商户平台准备：`AppID`、`商户号`、`APIv3Key`、`商户证书序列号`、`商户私钥文件 apiclient_key.pem`。
2. 配置后端 `takeout-backend/src/main/resources/application.yml` 的 `wechatpay.*`（推荐用环境变量）：

```powershell
$env:WECHATPAY_ENABLED = 'true'
$env:WECHATPAY_APP_ID = 'wx...'
$env:WECHATPAY_MCH_ID = '1900...'
$env:WECHATPAY_API_V3_KEY = '...'
$env:WECHATPAY_MCH_SERIAL_NO = '...'
$env:WECHATPAY_PRIVATE_KEY_PATH = 'C:\\path\\to\\apiclient_key.pem'
$env:WECHATPAY_NOTIFY_URL = 'https://你的公网域名/api/wechatpay/notify'
```

3. `WECHATPAY_NOTIFY_URL` 必须是微信服务器可访问的公网地址；本机调试一般需要内网穿透（如 ngrok、frp）。
