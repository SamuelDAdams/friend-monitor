package com.friendmonitor;

import com.friendmonitor.networking.AccessTokenAuthenticator;
import com.friendmonitor.networking.AccessTokenInterceptor;
import com.friendmonitor.networking.AccessTokenProvider;
import com.friendmonitor.networking.AccessTokenProviderImpl;
import com.friendmonitor.util.LocalhostHttpClientConverter;
import okhttp3.OkHttpClient;

public class AccountSession {
    private final AccessTokenProvider tokenProvider;
    private final OkHttpClient httpClient;

    public AccountSession(OkHttpClient httpClient, String idToken, String refreshToken) {
        tokenProvider = new AccessTokenProviderImpl(idToken, refreshToken, httpClient);
        AccessTokenInterceptor tokenInterceptor = new AccessTokenInterceptor(tokenProvider);
        AccessTokenAuthenticator tokenAuthenticator = new AccessTokenAuthenticator(tokenProvider);

        this.httpClient = LocalhostHttpClientConverter.convertToAllowLocalhostConnections(
                httpClient.newBuilder()
                .addInterceptor(tokenInterceptor)
                .authenticator(tokenAuthenticator)
                .build()
        );
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }
}
