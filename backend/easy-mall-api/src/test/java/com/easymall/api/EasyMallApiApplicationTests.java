package com.easymall.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 框架冒烟测试：上下文加载、免登录白名单、模拟微信登录、自定义登录态（Redis 会话）拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EasyMallApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Spring 上下文可正常启动")
    void contextLoads() {
    }

    @Test
    @DisplayName("商品列表免登录可访问")
    void goodsListIsPublic() throws Exception {
        mockMvc.perform(get("/goods").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @DisplayName("购物车未登录返回 401")
    void cartRequiresLogin() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("模拟微信登录后可用 token 访问购物车")
    void mockWxLoginThenAccessCart() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String login(String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    @Test
    @DisplayName("退出登录后服务端销毁登录态，原 token 失效")
    void logoutInvalidatesToken() throws Exception {
        String token = login("logout-case");
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(get("/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("单设备登录：同一用户再次登录后旧 token 被挤下线")
    void singleDeviceKicksPreviousToken() throws Exception {
        String first = login("kick-case");
        mockMvc.perform(get("/cart").header("Authorization", "Bearer " + first))
                .andExpect(status().isOk());
        String second = login("kick-case");
        mockMvc.perform(get("/cart").header("Authorization", "Bearer " + second))
                .andExpect(status().isOk());
        mockMvc.perform(get("/cart").header("Authorization", "Bearer " + first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
