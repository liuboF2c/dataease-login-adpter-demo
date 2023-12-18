package dataease.lboo.demo.util;

import cn.hutool.http.HttpRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClientUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static final String HTTPS = "https";

    /**
     * 根据url构建HttpClient（区分http和https）
     *
     * @param url 请求地址
     * @return CloseableHttpClient实例
     */
    private static CloseableHttpClient buildHttpClient(String url) {
        try {
            if (url.startsWith(HTTPS)) {
                SSLContextBuilder builder = new SSLContextBuilder();
                builder.loadTrustMaterial(null, (X509Certificate[] x509Certificates, String s) -> true);
                SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(builder.build(), new String[]{"TLSv1.1", "TLSv1.2", "SSLv3"}, null, NoopHostnameVerifier.INSTANCE);
                Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new PlainConnectionSocketFactory())
                        .register("https", socketFactory).build();
                HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
                CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();
                return httpClient;
            } else {
                // http
                return HttpClientBuilder.create().build();
            }
        } catch (Exception e) {
            throw new RuntimeException("HttpClient构建失败", e);
        }
    }

    /**
     * Get http请求
     *
     * @param url    请求地址
     * @param config 配置项，如果null则使用默认配置
     * @return 响应结果字符串
     */
    public static String get(String url, HttpClientConfig config) {
        CloseableHttpClient httpClient = buildHttpClient(url);
        HttpGet httpGet = new HttpGet(url);

        if (config == null) {
            config = new HttpClientConfig();
        }
        try {
            httpGet.setConfig(config.buildRequestConfig());

            Map<String, String> header = config.getHeader();
            for (String key : header.keySet()) {
                httpGet.addHeader(key, header.get(key));
            }

            httpGet.addHeader(HTTP.CONTENT_ENCODING, config.getCharset());

            HttpResponse response = httpClient.execute(httpGet);
            return getResponseStr(response, config);
        } catch (Exception e) {
            logger.error("HttpClient查询失败", e);
            throw new RuntimeException("HttpClient查询失败: " + e.getMessage());
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                logger.error("HttpClient关闭连接失败", e);
            }
        }
    }

    /**
     * Get http请求，携带请求体
     *
     * @param url    请求地址
     * @param config 配置项，如果null则使用默认配置
     * @return 响应结果字符串
     */
    public static String sendGetJson(String url, String json, HttpClientConfig config) {
        try {
            Map<String, String> headers = config.getHeader();//存放请求头，可以存放多个请求头
            String result = HttpRequest.get(url)
                    .header("Content-Type", "application/json")//头信息，多个头信息多次调用此方法即可
                    .addHeaders(headers)
                    .body(json)
                    .execute().body();
            return result;
        } catch (Exception e) {
            logger.error("HttpClient查询失败", e);
            throw new RuntimeException("HttpClient查询失败: " + e.getMessage());
        }
    }

    /**
     * Post请求，请求内容必须为JSON格式的字符串
     *
     * @param url    请求地址
     * @param config 配置项，如果null则使用默认配置
     * @param json   JSON格式的字符串
     * @return 响应结果字符串
     */
    public static String post(String url, String json, HttpClientConfig config) {
        CloseableHttpClient httpClient = buildHttpClient(url);
        HttpPost httpPost = new HttpPost(url);
        if (config == null) {
            config = new HttpClientConfig();
        }
        try {
            httpPost.setConfig(config.buildRequestConfig());

            Map<String, String> header = config.getHeader();
            for (String key : header.keySet()) {
                httpPost.addHeader(key, header.get(key));
            }
            httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
            httpPost.addHeader(HTTP.CONTENT_ENCODING, config.getCharset());

            EntityBuilder entityBuilder = EntityBuilder.create();
            entityBuilder.setText(json);
            entityBuilder.setContentType(ContentType.APPLICATION_JSON);
            entityBuilder.setContentEncoding(config.getCharset());
            HttpEntity requestEntity = entityBuilder.build();
            httpPost.setEntity(requestEntity);

            HttpResponse response = httpClient.execute(httpPost);
            return getResponseStr(response, config);
        } catch (Exception e) {
            logger.error("HttpClient查询失败", e);
            throw new RuntimeException("HttpClient查询失败: " + e.getMessage());
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                logger.error("HttpClient关闭连接失败", e);
            }
        }
    }

    /**
     * Post请求，请求内容必须为JSON格式的字符串
     *
     * @param url  请求地址
     * @param json JSON格式的字符串
     * @return 响应结果字符串
     */
    public static String post(String url, String json) {
        return HttpClientUtil.post(url, json, null);
    }

    /**
     * Post请求，请求内容必须为键值对参数
     *
     * @param url    请求地址
     * @param config 配置项，如果null则使用默认配置
     * @param body   请求内容键值对参数
     * @return 响应结果字符串
     */
    public static String post(String url, Map<String, String> body, HttpClientConfig config) {
        CloseableHttpClient httpClient = buildHttpClient(url);
        HttpPost httpPost = new HttpPost(url);
        if (config == null) {
            config = new HttpClientConfig();
        }
        try {
            httpPost.setConfig(config.buildRequestConfig());

            Map<String, String> header = config.getHeader();
            for (String key : header.keySet()) {
                httpPost.addHeader(key, header.get(key));
            }
            httpPost.addHeader(HTTP.CONTENT_ENCODING, config.getCharset());

            if (body != null && body.size() > 0) {
                List<NameValuePair> nvps = new ArrayList<>();
                for (String key : body.keySet()) {
                    nvps.add(new BasicNameValuePair(key, body.get(key)));
                }
                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(nvps, config.getCharset()));
                } catch (Exception e) {
                    logger.error("HttpClient转换编码错误", e);
                    throw new RuntimeException("HttpClient转换编码错误", e);
                }
            }

            HttpResponse response = httpClient.execute(httpPost);
            return getResponseStr(response, config);
        } catch (Exception e) {
            logger.error("HttpClient查询失败", e);
            throw new RuntimeException("HttpClient查询失败: " + e.getMessage());
        } finally {
            try {
                httpClient.close();
            } catch (Exception e) {
                logger.error("HttpClient关闭连接失败", e);
            }
        }
    }

    private static String getResponseStr(HttpResponse response, HttpClientConfig config) throws Exception{
        if(response.getStatusLine().getStatusCode() >= 400){
            String msg = EntityUtils.toString(response.getEntity(), config.getCharset());
            if(StringUtils.isEmpty(msg)){
                msg = "StatusCode: " + response.getStatusLine().getStatusCode();
            }
            throw new Exception(msg);
        }
        return EntityUtils.toString(response.getEntity(), config.getCharset());
    }
}
