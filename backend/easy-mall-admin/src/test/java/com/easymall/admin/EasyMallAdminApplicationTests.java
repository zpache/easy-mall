package com.easymall.admin;

import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.context.LoginUser;
import com.easymall.admin.common.context.UserContext;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.Brand;
import com.easymall.admin.entity.Category;
import com.easymall.admin.entity.Order;
import com.easymall.admin.entity.OrderRefund;
import com.easymall.admin.modules.goods.dto.BrandForm;
import com.easymall.admin.modules.goods.dto.CategoryForm;
import com.easymall.admin.modules.goods.dto.StockAdjustForm;
import com.easymall.admin.modules.goods.service.BrandService;
import com.easymall.admin.modules.goods.service.CategoryService;
import com.easymall.admin.modules.goods.service.GoodsService;
import com.easymall.admin.modules.goods.service.StockService;
import com.easymall.admin.modules.dashboard.service.DashboardService;
import com.easymall.admin.modules.dashboard.vo.DashboardOverviewVO;
import com.easymall.admin.modules.order.dto.DeliverForm;
import com.easymall.admin.modules.order.dto.RefundAuditForm;
import com.easymall.admin.modules.order.service.CommentService;
import com.easymall.admin.modules.order.service.OrderService;
import com.easymall.admin.modules.order.service.RefundService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 框架冒烟测试：上下文加载、鉴权拦截、登录链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EasyMallAdminApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private StockService stockService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private DashboardService dashboardService;

    @Test
    @DisplayName("Spring 上下文可正常启动")
    void contextLoads() {
    }

    @Test
    @DisplayName("未携带 token 访问受保护接口返回 401")
    void accessWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/system/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("账号密码登录成功并可用 token 获取当前用户")
    void loginThenFetchCurrentUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.data.accessToken");

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("密码错误返回业务错误码 4002")
    void loginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4002));
    }

    @Test
    @DisplayName("乐观锁：版本号自增，用过期版本号更新返回 4006")
    void optimisticLockRejectsStaleVersion() {
        Category category = categoryService.getById(1L);
        CategoryForm form = new CategoryForm(category.getName(), category.getParentId(),
                category.getIcon(), category.getSort(), category.getStatus(), category.getVersion());
        Integer version = category.getVersion();
        assertNotNull(version, "插入时应自动填充 version=0");

        categoryService.update(1L, form);
        assertEquals(version + 1, categoryService.getById(1L).getVersion(), "更新成功后版本号应 +1");

        // 拿着旧版本号再提交一次，模拟两个管理员同时编辑同一条数据
        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.update(1L, form));
        assertEquals(ResultCode.DATA_CONFLICT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("品牌：重名不允许创建，被商品引用时不允许删除")
    void brandUniqueNameAndDeleteGuard() {
        BrandForm form = new BrandForm("测试品牌", null, 9, 1, null, null);
        Brand brand = brandService.create(form);
        assertNotNull(brand.getId());

        BusinessException duplicate = assertThrows(BusinessException.class, () -> brandService.create(form));
        assertEquals("品牌名称已存在", duplicate.getMessage());

        // 品牌 1 已被种子商品引用
        BusinessException used = assertThrows(BusinessException.class, () -> brandService.delete(1L));
        assertEquals(ResultCode.DATA_CONFLICT.getCode(), used.getCode());

        brandService.delete(brand.getId());
    }

    @Test
    @DisplayName("库存：入库增加、出库不能超量、盘点改为目标值并记录流水")
    void stockAdjustFlow() {
        // 种子商品 1 初始库存 20
        stockService.adjust(new StockAdjustForm(1L, "in", 5, "补货入库"));
        assertEquals(25, goodsService.getById(1L).getStock());

        BusinessException overflow = assertThrows(BusinessException.class,
                () -> stockService.adjust(new StockAdjustForm(1L, "out", 999, "超量出库")));
        assertEquals(ResultCode.DATA_CONFLICT.getCode(), overflow.getCode());
        assertEquals(25, goodsService.getById(1L).getStock(), "出库失败时库存不应变化");

        stockService.adjust(new StockAdjustForm(1L, "check", 8, "月末盘点"));
        assertEquals(8, goodsService.getById(1L).getStock());

        var logs = stockService.pageLogs(1L, 1, 10);
        assertEquals(2, logs.total(), "两次成功调整应各留一条流水");
        assertEquals(-17, logs.records().getFirst().changeQty(), "盘点按差值记录变动量");
    }

    @Test
    @DisplayName("订单：待发货订单发货后进入待收货，重复发货被拒绝")
    void orderDeliverFlow() {
        orderService.deliver(1002L, new DeliverForm("顺丰速运", "SF100200300"));

        Order delivered = orderService.getById(1002L);
        assertEquals(Order.STATUS_UNRECEIVED, delivered.getStatus());
        assertEquals("SF100200300", delivered.getDeliveryNo());
        assertNotNull(delivered.getDeliveryTime(), "发货时间应自动写入");

        BusinessException repeat = assertThrows(BusinessException.class,
                () -> orderService.deliver(1002L, new DeliverForm("顺丰速运", "SF999")));
        assertEquals(ResultCode.DATA_CONFLICT.getCode(), repeat.getCode());
    }

    @Test
    @DisplayName("售后：审核同意后订单标记退款中，确认退款后标记已退款")
    void refundAuditFlow() {
        UserContext.set(new LoginUser(1L, "admin", "超级管理员", java.util.List.of("ADMIN")));
        try {
            refundService.audit(3001L, new RefundAuditForm(true, "同意退货退款"));
            assertEquals(OrderRefund.STATUS_APPROVED, refundService.getById(3001L).getStatus());
            assertEquals(Order.REFUND_PROCESSING, orderService.getById(1007L).getRefundStatus());

            refundService.confirm(3001L);
            assertEquals(OrderRefund.STATUS_REFUNDED, refundService.getById(3001L).getStatus());
            assertEquals(Order.REFUND_DONE, orderService.getById(1007L).getRefundStatus());

            BusinessException repeat = assertThrows(BusinessException.class,
                    () -> refundService.audit(3001L, new RefundAuditForm(false, "重复审核")));
            assertEquals(ResultCode.DATA_CONFLICT.getCode(), repeat.getCode());
        } finally {
            UserContext.clear();
        }
    }

    @Test
    @DisplayName("分析页：统计接口返回今日订单/销售额、待发货、库存预警")
    void dashboardOverviewAggregates() {
        DashboardOverviewVO overview = dashboardService.overview();

        // H2 种子有 2 笔已付款订单（1002 待发货、1007 待收货），created_at 默认当天，均计入今日订单
        assertEquals(2, overview.totalOrders(), "累计订单应为 2 笔");
        assertEquals(1, overview.pendingDelivery(), "待发货应为 1 笔（1002）");
        assertEquals(2, overview.todayOrders(), "种子订单创建于当天，今日订单应为 2");
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(overview.todaySales()),
                "种子订单支付时间在过去，今日销售额应为 0");
        assertEquals(0, new java.math.BigDecimal("89.80").compareTo(overview.totalSales()),
                "累计销售额应为 29.90 + 59.90 = 89.80");

        assertNotNull(overview.finishedOrders());
        assertNotNull(overview.stockWarning());
        assertNotNull(overview.onSaleGoods());
    }

    @Test
    @DisplayName("评价：隐藏后可再显示，并支持商家回复")
    void commentVisibleAndReply() {
        commentService.updateVisible(4003L, 1);
        assertEquals(1, commentService.getById(4003L).getIsShow());

        commentService.reply(4003L, "很抱歉影响体验，已为您补发");
        assertEquals("很抱歉影响体验，已为您补发", commentService.getById(4003L).getReply());
        assertNotNull(commentService.getById(4003L).getRepliedAt(), "回复时间应自动写入");
    }
}
