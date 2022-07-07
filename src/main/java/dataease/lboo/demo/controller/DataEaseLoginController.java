package dataease.lboo.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.util.JSONPObject;
import dataease.lboo.demo.util.EncryptUtils;
import dataease.lboo.demo.util.HttpClientConfig;
import dataease.lboo.demo.util.HttpClientUtil;
import dataease.lboo.demo.util.RsaUtil;
import org.apache.commons.lang.BooleanUtils;
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

    static {
        signature = EncryptUtils.aesEncrypt(accessKey + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(), secretKey, accessKey);

        config = new HttpClientConfig();
        config.addHeader("accessKey", accessKey);
        config.addHeader("signature", signature);
    }

    @RequestMapping("/dataease")
    public String toDataEase(HttpServletResponse response) throws Exception {
        Cookie cookie = new Cookie("Authorization", getToken());
        cookie.setDomain(domain);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:" + dataeaseEndpoint;
    }

    /**
     * 登出时需要清除 cookie，否则会出现一直登录中问题
     */
    @RequestMapping("/dataease/logout")
    public String logoutDataEase(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Cookie cookie = new Cookie("Authorization", "");
        cookie.setDomain(domain);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:https://fit2cloud.com";
    }

    private String getToken() throws Exception {
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

    private void updatePassword(String username, String password) {
        String userId = getUserId(username);
        String body = "{\n" +
                "  \"newPassword\": \"" + password + "\",\n" +
                "  \"userId\": " + userId + "\n" +
                "}";
        String result = HttpClientUtil.post(dataeaseEndpoint + "/api/user/adminUpdatePwd", body, config);
        JSONObject jsonObject = JSONObject.parseObject(result);
        Boolean success = jsonObject.getBoolean("success");
        if (BooleanUtils.isNotTrue(success)) {
            throw new RuntimeException("密码修改失败！");
        }
    }


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
