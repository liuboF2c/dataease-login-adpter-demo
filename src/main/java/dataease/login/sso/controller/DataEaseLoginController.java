package dataease.login.sso.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import dataease.login.sso.util.HttpClientConfig;
import dataease.login.sso.util.HttpClientUtil;
import dataease.login.sso.util.RsdUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author limin
 */
@RestController
public class DataEaseLoginController {

    /**
     * 填写DataEase的访问地址
     */
    private static String dataeaseEndpoint = "http://10.1.13.137:8000/";
    /**
     * 填写被嵌入系统的会回调地址
     */
    private static String redictUrlEndpoint = "https://www.fit2cloud.com/";
    /**
     * 模拟登陆要使用的用户
     */
    private static String USERNAME = "admin";
    /**
     * 模拟登陆用户密码
     */
    private static String PASSWORD = "DataEase@123456";

    private static final String PK_SEPARATOR = "-pk_separator-";
    private static HttpClientConfig config;


    @Autowired
    @GetMapping("/dataease")
    public String dataease() throws Exception {
        JSONObject result = getToken();
        String token = result.getString("token");
        String exp = result.getString("exp");
        return dataeaseEndpoint + "/sso/login-template.html?token=" + token + "&exp=" + exp + "&logout_url=" + redictUrlEndpoint;
    }

    @RequestMapping("/front-login")
    public String toDataEaseLogin(HttpServletResponse response) throws Exception {
        JSONObject result = getToken();
        String token = result.getString("token");
        String exp = result.getString("exp");
        return "redirect:" + dataeaseEndpoint + "/sso/login-template.html?token=" + token + "&exp=" + exp + "&logout_url=" + redictUrlEndpoint;
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
     * 当前使用的是默认配置的用户/密码进行登陆的，如果需要使用动态的用户进行登陆认证，则需要调用获取用户接口和修改用户密码接口来保证模拟登录成功
     * @return
     * @throws Exception
     */
    private JSONObject getToken() throws Exception {
        // TODO: 此处逻辑可修改为使用动态用户账号密码进行登陆，需要调用获取用户接口和修改用户密码接口来保证模拟登录成功
        // TODO：参考知识库接口调用指南 https://kb.fit2cloud.com/?p=90307bd3-9dd5-4626-b808-e7efa4159508
        String publicKey = getDataEaseKey();
        String username = RsdUtil.encrypt(USERNAME, RsdUtil.getPublicKey(publicKey));
        String password = RsdUtil.encrypt(PASSWORD, RsdUtil.getPublicKey(publicKey));

        String body = "{\n" +
                "  \"name\": \"" + username + "\",\n" +
                "  \"pwd\": \"" + password + "\"\n" +
                "}";
        String result = HttpClientUtil.post(dataeaseEndpoint + "/de2api/login/localLogin", body);
        return JSONObject.parseObject(result).getJSONObject("data");
    }

    private static String getDataEaseKey() throws Exception {
        String result = HttpClientUtil.get(dataeaseEndpoint + "/de2api/dekey",config);
        String key = JSONObject.parseObject(result).getString("data");
        Base64.Encoder urlEncoder = Base64.getUrlEncoder();
        String separator = urlEncoder.encodeToString(PK_SEPARATOR.getBytes(StandardCharsets.UTF_8));
        String[] strings = key.split(separator);
        String k1 = strings[0];
        String k2 = strings[1];
        return RsdUtil.aesDecrypt(k1, k2);
    }

    public static void main(String[] args) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(dataeaseEndpoint + "/de2api/dekey");
            CloseableHttpResponse res = client.execute(httpGet);
            String str = EntityUtils.toString(res.getEntity());
            Gson gson = new Gson();
            HashMap<String, String> hashMap = gson.fromJson(str, HashMap.class);
            String key = (String) hashMap.get("data");
            Base64.Encoder urlEncoder = Base64.getUrlEncoder();
            String separator = urlEncoder.encodeToString(PK_SEPARATOR.getBytes(StandardCharsets.UTF_8));
            String[] strings = key.split(separator);
            String k1 = strings[0];
            String k2 = strings[1];
            String pk = RsdUtil.aesDecrypt(k1, k2);
            String usernameStr = RsdUtil.encrypt(USERNAME, RsdUtil.getPublicKey(pk));
            String pwdStr = RsdUtil.encrypt(PASSWORD, RsdUtil.getPublicKey(pk));
            HttpPost httpPost = new HttpPost(dataeaseEndpoint + "/de2api/login/localLogin");
            httpPost.setHeader("Content-Type", "application/json");
            Map<String, String> form = new HashMap<>();
            form.put("name", usernameStr);
            form.put("pwd", pwdStr);
            String json = gson.toJson(form);
            System.out.println(json);
            StringEntity stringEntity = new StringEntity(json);
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse token = client.execute(httpPost);
            String tokenStr = EntityUtils.toString(token.getEntity());
            HashMap tokenMap = gson.fromJson(tokenStr, HashMap.class);
            Map data = (Map) tokenMap.get("data");
            System.out.println(data.get("token"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
