package com.friendmonitor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.LinkBrowser;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

interface AuthenticationClientListener {
    void onLoggedIn(String idToken, String refreshToken);
    void onLoginFailed();
}

@Slf4j
public class AuthenticationClient {
    private static final String GOOGLE_OAUTH_LOGIN_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_OAUTH_TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";

    private static final String CLIENT_ID = "498003058158-kt4ggbs1ulv8v2aovqjf27qp94p6knrv.apps.googleusercontent.com";

    // Note that this isn't actually secret since it's in the source code. We might not need this.
    private static final String CLIENT_SECRET = "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";

    private static final HttpUrl apiBase = HttpUrl.get("https://localhost:7223/api");

    private final OkHttpClient httpClient;

    private AuthenticationClientListener listener;

    private HttpServer server;

    @Inject
    public AuthenticationClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setListener(AuthenticationClientListener listener) {
        this.listener = listener;
    }

    public void login() throws IOException {
        final String outgoingState = randomBase64(8);
        final String codeVerifier = randomBase64(8);
        final String codeChallenge = base64UrlEncodeNoPadding(sha256(codeVerifier));
        final String codeChallengeMethod = "S256";

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 1);

        server.start();

        final String redirectUrl = String.format("http://localhost:%s/", server.getAddress().getPort());

        server.createContext("/", req ->
        {
            try {
                final HttpUrl url = HttpUrl.get("http://localhost" + req.getRequestURI());
                final String incomingState = url.queryParameter("state");
                final String code = url.queryParameter("code");

                //server.stop(0);
                processOauthResponse(incomingState, outgoingState, code, codeVerifier, redirectUrl);

                req.getResponseHeaders().set("Location", "https://localhost:7223/plugin-oauth-login-success");
                req.sendResponseHeaders(302, 0);
            }
            catch (Exception e) {
                req.sendResponseHeaders(400, 0);
                req.getResponseBody().write(e.getMessage().getBytes(StandardCharsets.UTF_8));
            } finally {
                req.close();
                server.stop(0);
            }
        });

        HttpUrl authUrl = HttpUrl.get(GOOGLE_OAUTH_LOGIN_ENDPOINT).newBuilder()
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", "openid profile")
            .addQueryParameter("redirect_uri", redirectUrl)
            .addQueryParameter("client_id", CLIENT_ID)
            .addQueryParameter("state", outgoingState)
            .addQueryParameter("code_challenge", codeChallenge)
            .addQueryParameter("code_challenge_method", codeChallengeMethod)
            .build();

        LinkBrowser.browse(authUrl.toString());

    }

    private void processOauthResponse(String outgoingState, String incomingState, String code, String codeVerifier, String redirectUrl) {
        if (!incomingState.equals(outgoingState)) {
            handleFailure();
            return;
        }

        HttpUrl exchangeCodeForTokensUrl = HttpUrl.get(GOOGLE_OAUTH_TOKEN_ENDPOINT);

        RequestBody formBody = new FormBody.Builder()
                .addEncoded("code", code)
                .addEncoded("redirect_uri", redirectUrl)
                .addEncoded("client_id", CLIENT_ID)
                .addEncoded("client_secret", CLIENT_SECRET)
                .addEncoded("grant_type", "authorization_code")
                .addEncoded("code_verifier", codeVerifier)
                .build();

        Request r = new Request.Builder()
                .url(exchangeCodeForTokensUrl)
                .post(formBody)
                .build();

        httpClient.newCall(r).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handleFailure();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.body() == null || !response.isSuccessful()) {
                    handleFailure();
                    return;
                }

                JsonObject responseJson = new Gson().fromJson(response.body().string(), JsonObject.class);

                if (!responseJson.has("id_token")) {
                    handleFailure();
                    return;
                }

                if (!responseJson.has("refresh_token")) {
                    handleFailure();
                    return;
                }

                String idToken = responseJson.get("id_token").getAsString();
                String refreshToken = responseJson.get("refresh_token").getAsString();

                listener.onLoggedIn(idToken, refreshToken);
            }
        });
    }

    private String base64UrlEncodeNoPadding(byte[] source) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(source);
    }

    private void handleFailure() {
        if (server != null) {
            server.stop(0);
        }

        listener.onLoginFailed();
    }

    private byte[] sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(source.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    private String randomBase64(int bytes) {
        SecureRandom random = new SecureRandom();

        byte[] randomBytes = new byte[bytes];
        random.nextBytes(randomBytes);

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        return encoder.encodeToString(randomBytes);
    }
}
