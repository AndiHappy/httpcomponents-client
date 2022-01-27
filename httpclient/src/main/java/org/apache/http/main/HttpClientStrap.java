package org.apache.http.main;

import com.google.gson.Gson;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class HttpClientStrap {

    public static Logger logger = LoggerFactory.getLogger(HttpClientStrap.class);
    // 字符集编码
    public static final String ENCODE_UTF8 = "UTF-8";
    // 请求方式
    public static final String POST_MODE = "POST";
    public static final String GET_MODE = "GET";

    public static final int DEFAULT_POOL = 0;
    public static final int SHARED_POOL_1 = 1;

    private static final HttpClient defaultHttpClient;

    private static final HttpClient sharedHttpClient1;

    private static final RequestConfig defaultRequestConfig;

    private static final RequestConfig sharedRequestConfig1;


    static {
        defaultHttpClient = HttpClients.custom().setConnectionManager(HttpClientConnectionManagerHolder.defaultPool).build();
        sharedHttpClient1 = HttpClients.custom()
                .setConnectionManager(HttpClientConnectionManagerHolder.sharedPool1).build();
        defaultRequestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(3000)
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .build();
        sharedRequestConfig1 = RequestConfig.custom()
                .setConnectionRequestTimeout(1000)
                .setConnectTimeout(3000)
                .setSocketTimeout(5000)
                .build();
    }

    public static HttpClient getHttpClient(int mark) {
        switch (mark) {
            case SHARED_POOL_1:
                return sharedHttpClient1;
            default:
                return defaultHttpClient;
        }
    }

    public static void configHttpRequest(HttpRequestBase httpRequestBase, int poolMark) {
        if (httpRequestBase == null) return;
        RequestConfig overrideConfig = defaultRequestConfig;
        switch (poolMark) {
            case SHARED_POOL_1:
                overrideConfig = sharedRequestConfig1;
                break;
            default:
                overrideConfig = defaultRequestConfig;
                break;
        }
        httpRequestBase.setConfig(overrideConfig);
    }

    public static String execute(HttpRequestBase httpRequestBase, int poolMark) throws IOException {
        HttpClient httpClient = getHttpClient(poolMark);
        configHttpRequest(httpRequestBase, poolMark);
        HttpResponse response = httpClient.execute(httpRequestBase);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        EntityUtils.consumeQuietly(entity);
        return result;
    }

    public static HttpPost getHttpPost(String url, HttpEntity entity) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(entity);
        return httpPost;
    }

    private static HttpEntity getStringEntity(Map<String, String> params, ContentType contentType)  {
        if (params == null || params.isEmpty())  return null;
        Gson gson = new Gson();
        String cont = gson.toJson(params);
        StringEntity entity = new StringEntity(cont, contentType);
        return entity;
    }


    public static HttpEntity getUrlEncodedFormEntity(Map<String, String> params) {
        if (params == null || params.isEmpty())  return null;
        List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>(params.size());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String val = String.valueOf(entry.getValue());
            nameValuePairList.add(new BasicNameValuePair(entry.getKey(), val));
        }
        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairList, Charset.defaultCharset());
        return urlEncodedFormEntity;
    }

    private static HttpGet getGetMethod(String url, Map<String, String> params) {
        HttpGet httpGet = null;
        try {
            List<NameValuePair> nvps = new ArrayList<>();
            if (params != null && params.size() > 0) {
                Set<Map.Entry<String, String>> set = params.entrySet();
                for (Iterator<Map.Entry<String, String>> it = set.iterator(); it.hasNext(); ) {
                    Map.Entry<String, String> entry = it.next();
                    NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry.getValue());
                    nvps.add(pair);
                }
            }
            // 设置请求参数
            String reUrl = url;
            if (!nvps.isEmpty()){
                reUrl = url + "?" + urlEncodedFormEntityToString(new UrlEncodedFormEntity(nvps, "utf-8"));
            }

            httpGet = new HttpGet(reUrl);
        } catch (Exception e) {
            logger.error(String.format("build HttpGet error,url:%s,AdapterSelectParam:%s", url, params), e);
        }
        return httpGet;
    }

    private static String urlEncodedFormEntityToString(UrlEncodedFormEntity urlEncodedFormEntity) throws IOException {
        InputStream stream = urlEncodedFormEntity.getContent();
        long length = urlEncodedFormEntity.getContentLength();
        byte[] con = new byte[(int) length];
        stream.read(con,0, (int) length);
        return new String(con);
    }

    public static String connect(String url,
                                 Map<String, String> params,
                                 String charSet,
                                 boolean useHttps,
                                 String requestMode,
                                 String contentType,
                                 String fileKey, String filePath,
                                 int poolMark) throws Exception {
        return connect(url, params, charSet, useHttps, requestMode, contentType, fileKey, filePath, poolMark, null);
    }

    public static String connect(String url,
                                 Map<String, String> params,
                                 String charSet,
                                 boolean useHttps,
                                 String requestMode,
                                 String contentType,
                                 String fileKey, String filePath,
                                 int poolMark, Map<String, String> headers) throws Exception {



        String paramsStr = params != null ? params.toString() : null;
        long start = System.currentTimeMillis();
        String result = "";
        try {
            // http客户端
            HttpClient httpclient = getHttpClient(poolMark);
            // 设置请求参数
            HttpRequestBase httpreq = null;
            if (POST_MODE.equalsIgnoreCase(requestMode)) {
                HttpEntity entity;
                if (contentType!=null&&("application/json".equalsIgnoreCase(contentType.trim()))) {
                    entity = getStringEntity(params, ContentType.APPLICATION_JSON);
                }else{
                    entity = getUrlEncodedFormEntity(params);
                }
                httpreq = getHttpPost(url, entity);
            }
            else if (GET_MODE.equalsIgnoreCase(requestMode))
                httpreq = getGetMethod(url, params);

            //设置http头
            if (headers != null && headers.size() > 0) {
                configHeaders(httpreq, headers);
            }
            httpreq.setHeader("Content-Type",contentType);
            configHttpRequest(httpreq, poolMark);
            // 发送请求,获取返回
            HttpResponse response = httpclient.execute(httpreq);
            // 获取返回状态
            StatusLine line = response.getStatusLine();
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                if (HttpStatus.SC_OK == line.getStatusCode()) {
                    result = EntityUtils.toString(resEntity);
                }
                // 关闭资源(否则有资源泄漏导致连接池占满)
                EntityUtils.consume(resEntity);
                return result;
            }
            throw new HttpResponseException(line.getStatusCode(), line.getReasonPhrase());
        } catch (Exception e) {
            logger.error("http request exception fail||url={}||params={}||result={}", url, paramsStr, result, e);
            throw new Exception(e);
        } finally {
            logger.error("http request cost||url={}||params={}||result={}||cost={}", url, paramsStr, result, System.currentTimeMillis() - start);
        }
    }

    private static void configHeaders(HttpRequestBase httpRequestBase, Map<String, String> headers) {
        if (headers != null) {
            Iterator it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                httpRequestBase.setHeader(key, value);
            }
        }
    }

    public static String get(String url, Map<String, String> reqBody, String contentType, boolean useHttps, Map<String, String> headers) throws Exception {
        return connect(url, reqBody, "utf-8", useHttps, GET_MODE, contentType, null, null, DEFAULT_POOL, headers);
    }
    public static String get(String url, Map<String, String> reqBody, String contentType, boolean useHttps, int poolMark, Map<String, String> headers) throws Exception {
        return connect(url, reqBody, "utf-8", useHttps, GET_MODE, contentType, null, null, poolMark, headers);
    }

    public static String post(String url, Map<String, String> reqBody, String contentType, boolean useHttps, Map<String, String> headers) throws Exception {
        return connect(url, reqBody, "utf-8", useHttps, POST_MODE, contentType, null, null, DEFAULT_POOL, headers);
    }

    public static String post(String url, String jsonParams, Map<String, String> headers) throws Exception {
        return connect(url, jsonParams, ENCODE_UTF8, DEFAULT_POOL, headers);
    }



    public static String connect(String url,
                                 String jsonParams,
                                 String charSet,
                                 int poolMark, Map<String, String> headers) throws Exception {


        HttpClient httpclient;
        long start = System.currentTimeMillis();
        String result = "";
        try {
            // http客户端
            httpclient = getHttpClient(poolMark);

            //请求体
            StringEntity entity = new StringEntity(jsonParams, "utf-8");
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");

            // 设置请求参数
            HttpRequestBase httpreq = getHttpPost(url, entity);

            //设置http头
            if (headers != null && headers.size() > 0) {
                configHeaders(httpreq, headers);
            }

            configHttpRequest(httpreq, poolMark);
            // 发送请求,获取返回
            HttpResponse response = httpclient.execute(httpreq);

            // 获取返回状态
            StatusLine line = response.getStatusLine();
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                if (HttpStatus.SC_OK == line.getStatusCode()) {
                    result = EntityUtils.toString(resEntity);
                }else{
                    throw new HttpResponseException(line.getStatusCode(), line.getReasonPhrase());
                }
                // 关闭资源(否则有资源泄漏导致连接池占满)
                EntityUtils.consume(resEntity);
                return result;
            }
            throw new HttpResponseException(line.getStatusCode(), line.getReasonPhrase());
        } catch (HttpResponseException e) {
            logger.error("http post request exception fail||url={}||jsonParams={}||result={}", url, jsonParams, result, e);
            throw e;
        } finally {
            logger.info("http post request cost||url={}||jsonParams={}||result={}||cost={}", url, jsonParams, result, System.currentTimeMillis() - start);
        }
    }

}

class HttpClientConnectionManagerHolder {

    public static Logger logger = LoggerFactory.getLogger(HttpClientStrap.class);
    public static PoolingHttpClientConnectionManager defaultPool;
    public static PoolingHttpClientConnectionManager sharedPool1;


    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setName("http-client-idle-connection-monitor-thread");
                    thread.setDaemon(true);
                    return thread;
                }
            });


    static {

        SSLContext sslContext = null;
        try {
            sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
        } catch(Exception e){
            logger.error("HttpClientUtils init error:{}",e);
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https",
                        new SSLConnectionSocketFactory(sslContext,
                                NoopHostnameVerifier.INSTANCE))
                .build();

        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingHttpClientConnectionManager.setMaxTotal(1000);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(200);
        defaultPool = poolingHttpClientConnectionManager;

        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager2 =new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingHttpClientConnectionManager2.setMaxTotal(120);
        poolingHttpClientConnectionManager2.setDefaultMaxPerRoute(100);
        sharedPool1 = poolingHttpClientConnectionManager2;

        scheduler.scheduleAtFixedRate(new IdleConnectionMonitor(), 10, 60, TimeUnit.SECONDS);
    }

    private static class IdleConnectionMonitor implements Runnable {
        @Override
        public void run() {
            defaultPool.closeExpiredConnections();
            defaultPool.closeIdleConnections(60, TimeUnit.SECONDS);
            sharedPool1.closeExpiredConnections();
            sharedPool1.closeIdleConnections(60, TimeUnit.SECONDS);
        }
    }
}
