package com.friendmonitor.networking;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


public class AccessTokenAuthenticator implements Authenticator {
    private final AccessTokenProvider tokenProvider;

    public AccessTokenAuthenticator(AccessTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    private static int responseCount(Response response) {
        int result = 1;

        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
        if (responseCount(response) > 3) {
            return null;
        }

        String originalToken = tokenProvider.getToken();

        synchronized(this) {
            String currentToken = tokenProvider.getToken();

            if (response.request().header("Authorization") == null) {
                return null;
            }

            // Token may have already been refreshed before we entered this synchronized
            // block.
            if (!currentToken.equals(originalToken)) {
                return response.request()
                        .newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", String.format("Bearer %s", currentToken))
                        .build();
            }

            currentToken = tokenProvider.refreshToken();

            if (currentToken == null || currentToken.equals(originalToken)) {
                return null;
            }

            return response.request()
                    .newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", String.format("Bearer %s", currentToken))
                    .build();
        }
    }
}
