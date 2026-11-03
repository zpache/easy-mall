package com.easymall.api.modules.share.service;

import com.easymall.api.common.api.ResultCode;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.modules.auth.config.WxProperties;
import com.easymall.api.modules.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 微信小程序码（getwxacodeunlimit）：分享海报用。
 *
 * <p>链路：stable_token 换 access_token → getwxacodeunlimit 拉图 →
 * 按确定性文件名落盘 uploads/qrcode/{key}.png → 经 /files/** 对外提供访问。</p>
 *
 * <p>同 key 命中本地缓存时不再请求微信，规避调用次数限制；
 * mock 模式（本地联调）返回占位图，避免没有真实 appid 时海报链路完全不可用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxaCodeService {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";
    private static final String QRCODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit";

    /** 微信限制 scene 最长 32 字符 */
    private static final int SCENE_MAX = 32;

    private final WxProperties wxProperties;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    /** access_token 进程内缓存（与 admin 端 WxSubscribeService 各自独立） */
    private volatile String accessToken;
    private volatile long tokenExpireAt;

    /**
     * 商品分享码：scene 带 id（可选 inv=邀请人），返回可直接访问的图片地址。
     *
     * @param goodsId   商品 ID
     * @param inviterId 分享人 ID，可为空
     */
    public String goodsQrcode(long goodsId, Long inviterId) {
        if (goodsId <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        String scene = "id=" + goodsId;
        if (inviterId != null && inviterId > 0) {
            String withInviter = scene + "&inv=" + inviterId;
            if (withInviter.length() <= SCENE_MAX) {
                scene = withInviter;
            }
        }
        String key = "goods-" + goodsId + (scene.contains("&inv=") ? "-inv" + inviterId : "");

        String cached = fileStorageService.findQrcode(key);
        if (cached != null) {
            return cached;
        }
        byte[] image = wxProperties.isMock()
                ? placeholderPng()
                : fetchQrcode(scene, "pages/goods/detail");
        return fileStorageService.storeQrcode(key, image);
    }

    private byte[] fetchQrcode(String scene, String page) {
        if (!StringUtils.hasText(wxProperties.getAppId()) || !StringUtils.hasText(wxProperties.getAppSecret())) {
            throw new BusinessException(ResultCode.WX_QRCODE_FAILED, "未配置小程序 appId/appSecret");
        }
        String token = getAccessToken();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scene", scene);
        payload.put("page", page);
        // check_path=false：页面尚未发布到线上时也能生成，开发/体验阶段可用
        payload.put("check_path", false);
        payload.put("env_version", "release");
        payload.put("width", 430);
        payload.put("auto_color", true);
        byte[] body;
        try {
            body = restClient.post()
                    .uri(QRCODE_URL + "?access_token={token}", token)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(payload))
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("请求小程序码接口失败, scene={}", scene, e);
            throw new BusinessException(ResultCode.WX_QRCODE_FAILED);
        }
        if (body == null || body.length == 0) {
            throw new BusinessException(ResultCode.WX_QRCODE_FAILED);
        }
        // 失败时微信返回 JSON（{"errcode":...}），成功时返回图片二进制
        if (body[0] == '{') {
            String message = parseError(body);
            log.warn("小程序码生成失败: scene={}, resp={}", scene, message);
            throw new BusinessException(ResultCode.WX_QRCODE_FAILED, message);
        }
        return body;
    }

    private String parseError(byte[] body) {
        try {
            JsonNode root = objectMapper.readTree(new String(body, java.nio.charset.StandardCharsets.UTF_8));
            return "errcode=" + root.path("errcode").asInt() + ", errmsg=" + root.path("errmsg").asText("");
        } catch (Exception e) {
            return "未知错误";
        }
    }

    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpireAt) {
            return accessToken;
        }
        synchronized (this) {
            if (accessToken != null && System.currentTimeMillis() < tokenExpireAt) {
                return accessToken;
            }
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credential");
            form.add("appid", wxProperties.getAppId());
            form.add("secret", wxProperties.getAppSecret());
            String body = restClient.post()
                    .uri(TOKEN_URL)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            try {
                JsonNode root = objectMapper.readTree(body);
                String token = root.path("access_token").asText("");
                if (token.isBlank()) {
                    throw new BusinessException(ResultCode.WX_QRCODE_FAILED, "获取 access_token 失败");
                }
                accessToken = token;
                tokenExpireAt = System.currentTimeMillis() + (root.path("expires_in").asLong(7200) - 200) * 1000;
                return token;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("解析 access_token 响应失败", e);
                throw new BusinessException(ResultCode.WX_QRCODE_FAILED);
            }
        }
    }

    /** mock 占位图：本地联调时没有真实 appid 也能跑通海报链路 */
    private byte[] placeholderPng() {
        try {
            BufferedImage image = new BufferedImage(430, 430, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 430, 430);
            g.setColor(new Color(0x33, 0x33, 0x33));
            g.drawRect(10, 10, 409, 409);
            g.drawString("MOCK QRCODE", 150, 215);
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.WX_QRCODE_FAILED);
        }
    }
}
