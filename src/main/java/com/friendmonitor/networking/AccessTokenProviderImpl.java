package com.friendmonitor.networking;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class AccessTokenProviderImpl implements AccessTokenProvider {
    private String idToken;
    private final String refreshToken;

    private final OkHttpClient httpClient;

    private final HttpUrl googleRefreshUrl = HttpUrl.get("https://www.googleapis.com/oauth2/v4/token");

    public AccessTokenProviderImpl(String idToken, String refreshToken, OkHttpClient httpClient) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.httpClient = httpClient.newBuilder()
                .retryOnConnectionFailure(true)
                .build();
    }

    @Override
    public @NotNull String getToken() {
        return idToken;
    }

    @Override
    public @Nullable String refreshToken() {
        FormBody body = new FormBody.Builder()
                .add("client_id", "")
                .add("client_secret", "")
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build();

        Request r = new Request.Builder()
                .post(body)
                .url(googleRefreshUrl)
                .build();
        try {
            Response response = httpClient.newCall(r).execute();

            if (response.body() == null || !response.isSuccessful()) {
                return null;
            }

            JsonObject responseJson = new Gson().fromJson(response.body().string(), JsonObject.class);

            if (!responseJson.has("id_token")) {
                return null;
            }

            idToken = responseJson.get("id_token").getAsString();

            return idToken;
        } catch (IOException e) {
            return null;
        }
    }
}
