package com.example.demo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        try {
            // ✅ TẠO SSL CONTEXT ĐỂ BỎ QUA SSL VERIFICATION (cho dev)
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            }, new java.security.SecureRandom());

            // ✅ ÁP DỤNG SSL CONTEXT
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            
            // ✅ CẤU HÌNH TIMEOUT DÀI HƠN
            factory.setConnectTimeout(45000); // 45 giây
            factory.setReadTimeout(45000);    // 45 giây
            
            // ✅ THỬ PROXY NẾU CẦN (bỏ comment nếu behind corporate proxy)
            // factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy-host", 8080)));
            
            RestTemplate restTemplate = new RestTemplate(factory);
            
            // ✅ THÊM HEADERS ĐỂ GIẢ LẬP BROWSER
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().set("User-Agent", 
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                request.getHeaders().set("Accept", "audio/mpeg, audio/*, */*");
                request.getHeaders().set("Accept-Language", "en-US,en;q=0.9");
                request.getHeaders().set("Accept-Encoding", "identity"); // Không nén
                request.getHeaders().set("Connection", "keep-alive");
                return execution.execute(request, body);
            });
            
            return restTemplate;
            
        } catch (Exception e) {
            // Fallback: tạo RestTemplate đơn giản
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(30000);
            return new RestTemplate(factory);
        }
    }
}