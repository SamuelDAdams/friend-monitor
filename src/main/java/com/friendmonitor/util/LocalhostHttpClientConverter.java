package com.friendmonitor.util;

import okhttp3.OkHttpClient;

import javax.net.ssl.*;

public final class LocalhostHttpClientConverter {
    private LocalhostHttpClientConverter() {}

    public static OkHttpClient convertToAllowLocalhostConnections(OkHttpClient httpClient) {
        X509TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { TRUST_ALL_CERTS }, new java.security.SecureRandom());

            return httpClient.newBuilder()
                    .sslSocketFactory(sslContext.getSocketFactory(), TRUST_ALL_CERTS)
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .build();
        } catch (Exception e) {
            return null;
        }

    }
}
