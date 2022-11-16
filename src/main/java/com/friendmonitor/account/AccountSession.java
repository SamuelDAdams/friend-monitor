package com.friendmonitor.account;

import com.friendmonitor.account.runescape.RunescapeAccountSessionCreationListener;
import com.friendmonitor.account.runescape.RunescapeAccountSessionImpl;
import com.friendmonitor.activityupdate.ActivityUpdateBroadcasterImpl;
import com.friendmonitor.activityupdate.LocationUpdateBroadcaster;
import com.friendmonitor.activityupdate.LocationUpdateBroadcasterImpl;
import com.friendmonitor.map.WorldMapFriendManagerImpl;
import com.friendmonitor.networking.AccessTokenAuthenticator;
import com.friendmonitor.networking.AccessTokenInterceptor;
import com.friendmonitor.networking.AccessTokenProvider;
import com.friendmonitor.networking.AccessTokenProviderImpl;
import com.friendmonitor.util.LocalhostHttpClientConverter;
import com.google.gson.Gson;
import lombok.NonNull;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;

import java.io.IOException;

public class AccountSession {
    private final AccessTokenProvider tokenProvider;
    private final OkHttpClient httpClient;

    private final Gson gson;
    private final HttpUrl apiBase;

    public AccountSession(OkHttpClient httpClient, String idToken, String refreshToken, Gson gson, HttpUrl apiBase) {
        this.apiBase = apiBase;
        tokenProvider = new AccessTokenProviderImpl(idToken, refreshToken, httpClient);
        AccessTokenInterceptor tokenInterceptor = new AccessTokenInterceptor(tokenProvider);
        AccessTokenAuthenticator tokenAuthenticator = new AccessTokenAuthenticator(tokenProvider);

        this.httpClient = LocalhostHttpClientConverter.convertToAllowLocalhostConnections(
                httpClient.newBuilder()
                .addInterceptor(tokenInterceptor)
                .authenticator(tokenAuthenticator)
                .build()
        );

        this.gson = gson;
    }

    public void startRunescapeAccountSession(String accountHash, String displayName, RunescapeAccountSessionCreationListener listener, Gson gson, WorldMapPointManager worldMapPointManager) {
        CreateRunescapeAccountModel model = new CreateRunescapeAccountModel(accountHash, displayName);
        Request r = new Request.Builder()
                .url(apiBase.newBuilder().addPathSegment("account").addPathSegment("runescape").build())
                .post(RequestBody.create(RuneLiteAPI.JSON, gson.toJson(model)))
                .build();

        httpClient.newCall(r).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                HttpUrl activityUpdateUrl = apiBase.newBuilder().addPathSegment("activity").build();
                HttpUrl socketUrl = apiBase.newBuilder().addPathSegment("socket").build();

                LocationUpdateBroadcaster location = new LocationUpdateBroadcasterImpl(httpClient, gson, activityUpdateUrl);

                listener.onSuccess(
                    new RunescapeAccountSessionImpl(
                        accountHash,
                        new ActivityUpdateBroadcasterImpl(location, httpClient, activityUpdateUrl, gson),
                        new WorldMapFriendManagerImpl(worldMapPointManager),
                        httpClient,
                        socketUrl,
                        gson
                    )
                );
            }
        });
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }
}
