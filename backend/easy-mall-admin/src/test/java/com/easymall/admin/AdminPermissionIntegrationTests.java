package com.easymall.admin;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端权限链路集成测试：JWT 每次请求回库刷新角色 / 菜单权限。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPermissionIntegrationTests {

    private static final long OPERATOR_USER_ID = 100L;
    private static final long OPERATOR_ROLE_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpOperator() {
        jdbcTemplate.update("INSERT INTO sys_role (id, name, code, sort, status) VALUES (?, ?, ?, ?, ?)",
                OPERATOR_ROLE_ID, "运营人员", "OPERATOR", 2, 1);
        jdbcTemplate.update("INSERT INTO sys_user (id, username, password, nickname, role, status, dept_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                OPERATOR_USER_ID, "operator", passwordEncoder.encode("operator123"), "运营人员",
                "OPERATOR", 1, 1);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                OPERATOR_USER_ID, OPERATOR_ROLE_ID);
        // 只授予“用户管理”页面权限，不授予新增 / 编辑 / 删除等按钮权限
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)",
                OPERATOR_ROLE_ID, 2L);
    }

    @Test
    @DisplayName("ADMIN 即使未单独配置按钮菜单，也能访问操作接口")
    void adminHasImplicitOperationPermissions() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(post("/brands")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"权限兜底测试品牌\",\"sort\":1,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("页面权限不等于操作权限：只授予列表页时新增仍返回 403")
    void pagePermissionDoesNotGrantOperation() throws Exception {
        String token = login("operator", "operator123");

        mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"forbidden_user\",\"password\":\"123456\","
                                + "\"nickname\":\"无权限新增\",\"status\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("前端按钮权限码只下发已授权菜单，ADMIN 额外获得代码兜底码")
    void accessCodesFeedFrontendButtonVisibility() throws Exception {
        String operatorToken = login("operator", "operator123");
        List<String> operatorCodes = readCodes(operatorToken);
        assertTrue(operatorCodes.contains("system:user:list"));
        assertFalse(operatorCodes.contains("system:user:create"));

        String adminToken = login("admin", "admin123");
        List<String> adminCodes = readCodes(adminToken);
        assertTrue(adminCodes.contains("goods:brand:create"));
    }

    @Test
    @DisplayName("用户停用后，旧 token 立即失效并返回 401")
    void disabledUserInvalidatesExistingToken() throws Exception {
        String token = login("operator", "operator123");

        jdbcTemplate.update("UPDATE sys_user SET status = 0 WHERE id = ?", OPERATOR_USER_ID);

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("待发货页面权限不等于发货权限：缺少 order:order:ship 时返回 403")
    void deliveryPagePermissionDoesNotGrantShipOperation() throws Exception {
        jdbcTemplate.update("INSERT INTO sys_menu (id, pid, name, title, type, auth_code, path, order_no, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                10L, 0L, "OrderDelivery", "待发货", "menu", "order:order:deliver",
                "/order/delivery", 10, 1);
        jdbcTemplate.update("INSERT INTO sys_menu (id, pid, name, title, type, auth_code, order_no, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                11L, 10L, "PermBtnOrderShip", "订单发货", "button", "order:order:ship", 10, 1);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)",
                OPERATOR_ROLE_ID, 10L);

        String token = login("operator", "operator123");

        mockMvc.perform(put("/orders/1002/deliver")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deliveryCompany\":\"顺丰速运\",\"deliveryNo\":\"SF1234567890\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("订单列表权限不等于详情 / 物流权限，回收按钮权限后接口不可绕过")
    void orderListPermissionDoesNotGrantDetailOrLogistics() throws Exception {
        jdbcTemplate.update("INSERT INTO sys_menu (id, pid, name, title, type, auth_code, path, order_no, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                12L, 0L, "OrderList", "订单列表", "menu", "order:order:list",
                "/order/list", 10, 1);
        jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)",
                OPERATOR_ROLE_ID, 12L);

        String token = login("operator", "operator123");

        mockMvc.perform(get("/orders/1002")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/orders/1002/logistics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("角色停用后，旧 token 立即失去该角色权限")
    void disabledRoleRevokesExistingTokenPermissions() throws Exception {
        String token = login("operator", "operator123");

        jdbcTemplate.update("UPDATE sys_role SET status = 0 WHERE id = ?", OPERATOR_ROLE_ID);

        mockMvc.perform(get("/system/user/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private List<String> readCodes(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/user/codes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data");
    }
}
