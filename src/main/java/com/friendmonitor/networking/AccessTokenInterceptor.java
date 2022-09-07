package com.friendmonitor.networking;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AccessTokenInterceptor implements Interceptor {
    private final AccessTokenProvider tokenProvider;

    public AccessTokenInterceptor(AccessTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public @NotNull Response intercept(@NotNull Chain chain) throws IOException {
        String token = tokenProvider.getAccessToken();

        Request authenticatedRequest = chain.request()
                .newBuilder()
                .addHeader("Authorization", String.format("Bearer %s", token))
                .build();

        return chain.proceed(authenticatedRequest);
    }
}
