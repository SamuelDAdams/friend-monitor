package com.friendmonitor;

import com.friendmonitor.account.AccountSession;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
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

import static okhttp3.internal.Util.EMPTY_REQUEST;

interface AuthenticationClientListener {
    void onLoggedIn(AccountSession session);
    void onLoginFailed();
}

@Slf4j
public class AuthenticationClient {
    private static final String OAUTH_AUTHORIZE_ENDPOINT = "https://osrsfriendmonitorlogin.b2clogin.com/osrsfriendmonitorlogin.onmicrosoft.com/B2C_1_email/oauth2/v2.0/authorize";
    public static final String OAUTH_TOKEN_ENDPOINT = "https://osrsfriendmonitorlogin.b2clogin.com/osrsfriendmonitorlogin.onmicrosoft.com/B2C_1_email/oauth2/v2.0/token";

    public static final String CLIENT_ID = "8dd9839d-f3e1-4499-a441-cb65837541f8";

    public static final String API_SCOPE = "https://osrsfriendmonitorlogin.onmicrosoft.com/9aae51ed-fbc5-4acd-ae86-509c3e17b83b/activity.update";

    private static final HttpUrl serverBase = HttpUrl.get("https://localhost:7223");
    private static final HttpUrl apiBase =  serverBase.newBuilder().addPathSegment("api").build();

    private final OkHttpClient httpClient;

    private AuthenticationClientListener listener;

    private HttpServer server;
    private final Gson gson;
    @Inject
    public AuthenticationClient(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public void setListener(AuthenticationClientListener listener) {
        this.listener = listener;
    }

    public void login() throws IOException {
        final String outgoingState = randomBase64(32);
        final String codeVerifier = randomBase64(32);
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

                processOauthResponse(incomingState, outgoingState, code, codeVerifier, redirectUrl);

                req.getResponseHeaders().set("Location", serverBase.newBuilder().addPathSegment("plugin-oauth-login-success").build().toString());
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

        HttpUrl authUrl = HttpUrl.get(OAUTH_AUTHORIZE_ENDPOINT).newBuilder()
            .addQueryParameter("response_type", "code")
            .addQueryParameter("scope", String.format("openid offline_access %s", API_SCOPE))
            .addQueryParameter("redirect_uri", redirectUrl)
            .addQueryParameter("client_id", CLIENT_ID)
            .addQueryParameter("response_mode", "query")
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

        HttpUrl exchangeCodeForTokensUrl = HttpUrl.get(OAUTH_TOKEN_ENDPOINT);

        RequestBody formBody = new FormBody.Builder()
                .addEncoded("code", code)
                .addEncoded("redirect_uri", redirectUrl)
                .addEncoded("client_id", CLIENT_ID)
                .addEncoded("grant_type", "authorization_code")
                .addEncoded("scope", String.format("%s offline_access", API_SCOPE))
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

                if (!responseJson.has("access_token")) {
                    handleFailure();
                    return;
                }

                if (!responseJson.has("refresh_token")) {
                    handleFailure();
                    return;
                }

                String accessToken = responseJson.get("access_token").getAsString();
                String refreshToken = responseJson.get("refresh_token").getAsString();

                makeLoginCall(accessToken, refreshToken);
            }
        });
    }

    private void makeLoginCall(String accessToken, String refreshToken) {
        AccountSession session = new AccountSession(httpClient, accessToken, refreshToken, gson, apiBase);

        HttpUrl url = apiBase.newBuilder().addEncodedPathSegment("account").build();

        Request r = new Request.Builder()
                .url(url)
                .post(EMPTY_REQUEST)
                .build();

        session.getHttpClient().newCall(r).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) { handleFailure(); }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    handleFailure();
                    return;
                }

                listener.onLoggedIn(session);
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
