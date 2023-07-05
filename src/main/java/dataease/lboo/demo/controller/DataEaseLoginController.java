package dataease.lboo.demo.controller;

import com.alibaba.fastjson.JSONObject;
import dataease.lboo.demo.util.EncryptUtils;
import dataease.lboo.demo.util.HttpClientConfig;
import dataease.lboo.demo.util.HttpClientUtil;
import dataease.lboo.demo.util.RsaUtil;
import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.util.Base64Util;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class DataEaseLoginController {

    // 根域名
    private static String domain = "fit2cloud.com";
    // DataEase 访问地址，如 http://edu.fit2cloud.com
    private static String dataeaseEndpoint = "http://edu.fit2cloud.com";
    // 取自 DataEase 源码 application.yml 中的 rsa.public_key
    private static String publicKey = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANL378k3RiZHWx5AfJqdH9xRNBmD9wGD2iRe41HdTNF8RUhNnHit5NpMNtGL0NPTSSpPjjI1kJfVorRvaQerUgkCAwEAAQ==";
    private static String accessKey = "<access key>";
    private static String secretKey = "<secret key>";
    private static String signature;
    // 模拟登陆要使用的用户名
    private static String USERNAME = "<username>";
    // 模拟登陆使用的密码，建议实际使用时生成随机密码
    private static String PASSWORD = "<password>";
    private static HttpClientConfig config;

    private static void initSignature() {
        signature = EncryptUtils.aesEncrypt(accessKey + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(), secretKey, accessKey);

        config = new HttpClientConfig();
        config.addHeader("accessKey", accessKey);
        config.addHeader("signature", signature);
    }

    /**
     * 输出 signature 和用户名密码加密字符串，可用于在 API 查看界面调试
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        initSignature();
        System.out.println("signature：" + signature);
        String username = RsaUtil.encryptByPublicKey(publicKey, USERNAME);
        String password = RsaUtil.encryptByPublicKey(publicKey, PASSWORD);
        System.out.println("username：" + username);
        System.out.println("password：" + password);

        System.out.println("password Base64：" + Base64Util.encode(PASSWORD));
    }

    // 同域跳转，适用于同一根域名不同系统之间跳转
    @RequestMapping("/dataease")
    public String toDataEase(HttpServletResponse response) throws Exception {
        Cookie cookie = new Cookie("Authorization", getToken());
        cookie.setDomain(domain);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:" + dataeaseEndpoint;
    }

    // 同域跳转不需要使用此方法，也不需要配置 Nginx
    @RequestMapping("/front-login")
    public String toDataEaseLogin(HttpServletResponse response) throws Exception {
        String token = getToken();
        return "redirect:" + dataeaseEndpoint + "/sso/login-template.html?token=" + token;
    }

    /**
     * 登出时需要清除 cookie，否则会出现一直登录中问题
     */
    @RequestMapping("/dataease/logout")
    public String logoutDataEase(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Cookie cookie = new Cookie("Authorization", "");
        cookie.setDomain(domain);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:" + dataeaseEndpoint;
    }

    /**
     * 登出时需要清除 cookie，否则会出现一直登录中问题
     */
    @RequestMapping("/dataease/crossorigin/logout")
    public String logoutCrossOriginDataEase(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return "redirect:" + dataeaseEndpoint + "/sso/logout-template.html";
    }

    /**
     * 模拟登录并获取 Token
     *
     * @return
     * @throws Exception
     */
    private String getToken() throws Exception {
        initSignature();
        String username = RsaUtil.encryptByPublicKey(publicKey, USERNAME);
        String password = RsaUtil.encryptByPublicKey(publicKey, PASSWORD);
        updatePassword(USERNAME, PASSWORD);

        String body = "{\n" +
                "  \"loginType\": 0,\n" +
                "  \"password\": \"" + password + "\",\n" +
                "  \"username\": \"" + username + "\"\n" +
                "}";
        String result = HttpClientUtil.post(dataeaseEndpoint + "/api/auth/login", body, config);
        return JSONObject.parseObject(result).getJSONObject("data").getString("token");
    }

    /**
     * 修改用户密码
     * 1.18.8 以后的版本需要使用 Base64 加密 password
     * 小于等于 1.18.7 的版本请移除 Base64 加密方法
     *
     * @param username
     * @param password
     */
    private void updatePassword(String username, String password) {
        String userId = getUserId(username);
        String body = "{\n" +
                "  \"newPassword\": \"" + Base64Util.encode(password)/* 1.18.8 以后的版本需要使用 Base64 加密 password, 小于等于 1.18.7 的版本请移除 Base64 加密方法 */ + "\",\n" +
                "  \"userId\": " + userId + "\n" +
                "}";
        String result = HttpClientUtil.post(dataeaseEndpoint + "/api/user/adminUpdatePwd", body, config);
        JSONObject jsonObject = JSONObject.parseObject(result);
        Boolean success = jsonObject.getBoolean("success");
        if (BooleanUtils.isNotTrue(success)) {
            throw new RuntimeException("密码修改失败！");
        }
    }


    /**
     * 根据用户名获取用户 ID
     *
     * @param username 用户名(唯一标识)
     * @return
     */
    private String getUserId(String username) {
        String body = "{\n" +
                "    \"conditions\": [\n" +
                "        {\n" +
                "            \"field\": \"username\",\n" +
                "            \"operator\": \"eq\",\n" +
                "            \"value\": \"" + username + "\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"orders\": []\n" +
                "}";
        String result = HttpClientUtil.post(dataeaseEndpoint + "/api/user/userGrid/1/1", body, config);
        JSONObject jsonObject = JSONObject.parseObject(result);
        return jsonObject.getJSONObject("data").getJSONArray("listObject").getJSONObject(0).getString("userId");
    }
}
