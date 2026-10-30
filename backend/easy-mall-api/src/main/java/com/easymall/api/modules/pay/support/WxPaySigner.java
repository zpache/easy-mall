package com.easymall.api.modules.pay.support;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 微信支付 v3 签名/验签/解密工具。
 *
 * <ul>
 *   <li>请求签名：SHA256withRSA，待签名串 = 方法 + \n + URL 路径 + \n + 时间戳 + \n + 随机串 + \n + 请求体 + \n</li>
 *   <li>回调验签：SHA256withRSA，待验签串 = 时间戳 + \n + 随机串 + \n + 回调报文 + \n</li>
 *   <li>报文解密：AES-256-GCM，密钥为 APIv3 密钥</li>
 * </ul>
 */
public final class WxPaySigner {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private WxPaySigner() {
    }

    /** 商户私钥签名，返回 Base64 字符串 */
    public static String sign(String privateKeyPem, String message) {
        try {
            Signature signer = Signature.getInstance(SIGN_ALGORITHM);
            signer.initSign(parsePrivateKey(privateKeyPem));
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("微信支付签名失败: " + e.getMessage(), e);
        }
    }

    /** 微信支付公钥验签 */
    public static boolean verify(String publicKeyPem, String message, String signature) {
        try {
            Signature verifier = Signature.getInstance(SIGN_ALGORITHM);
            verifier.initVerify(parsePublicKey(publicKeyPem));
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            return false;
        }
    }

    /** AES-256-GCM 解密回调 resource.ciphertext */
    public static String decrypt(String apiV3Key, String nonce, String associatedData, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            SecretKeySpec key = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (associatedData != null && !associatedData.isEmpty()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("微信支付回调解密失败: " + e.getMessage(), e);
        }
    }

    private static PrivateKey parsePrivateKey(String pem) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static PublicKey parsePublicKey(String pem) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    /** 去掉 PEM 头尾与空白，只保留 Base64 正文 */
    private static String stripPem(String pem) {
        return pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
    }
}
