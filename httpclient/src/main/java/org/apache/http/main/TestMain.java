package org.apache.http.main;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class TestMain {

    public final static void main(String[] args) throws Exception {
        // 实现了Closeable的 httpclient 的抽象类
        // HttpClients.createDefault() = HttpClientBuilder.create().build();
        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            HttpGet httpget = new HttpGet("http://127.0.0.1:8080/c");

            System.out.println("Executing request " + httpget.getURI());
            CloseableHttpResponse response = httpclient.execute(httpget);
            try {
                System.out.println("----------------------------------------");
                System.out.println("http status: "+ response.getStatusLine());
                String value = EntityUtils.toString(response.getEntity());
                System.out.println("http response: "+ value);

                // Do not feel like reading the response body
                // Call abort on the request object
                httpget.abort();
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }
}