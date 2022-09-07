package com.friendmonitor.networking;

import com.friendmonitor.AuthenticationClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class AccessTokenProviderImpl implements AccessTokenProvider {
    private String accessToken;
    private String refreshToken;

    private final OkHttpClient httpClient;


    public AccessTokenProviderImpl(String accessToken, String refreshToken, OkHttpClient httpClient) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.httpClient = httpClient.newBuilder()
                .retryOnConnectionFailure(true)
                .build();
    }

    @Override
    public @NotNull String getAccessToken() {
        return accessToken;
    }

    @Override
    public @Nullable String refreshAccessToken() {
        FormBody body = new FormBody.Builder()
                .add("client_id", AuthenticationClient.CLIENT_ID)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build();

        Request r = new Request.Builder()
                .post(body)
                .url(AuthenticationClient.OAUTH_TOKEN_ENDPOINT)
                .build();
        try {
            Response response = httpClient.newCall(r).execute();

            if (response.body() == null || !response.isSuccessful()) {
                return null;
            }

            JsonObject responseJson = new Gson().fromJson(response.body().string(), JsonObject.class);

            if (!responseJson.has("access_token")) {
                return null;
            }

            if (!responseJson.has("refresh_token")) {
                return null;
            }

            accessToken = responseJson.get("access_token").getAsString();
            refreshToken = responseJson.get("refresh_token").getAsString();

            return accessToken;
        } catch (IOException e) {
            return null;
        }
    }
}
