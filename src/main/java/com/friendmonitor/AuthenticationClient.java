package com.friendmonitor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.spi.Message;
import com.sun.net.httpserver.HttpServer;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import net.runelite.client.account.AccountSession;
import net.runelite.client.util.LinkBrowser;
import okhttp3.*;
import org.graalvm.compiler.core.GraalCompiler;

import javax.net.ssl.*;
import java.io.Console;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

interface AuthenticationClientListener {
    void onLoggedIn(String token);
}


public class AuthenticationClient {
    private static final String GOOGLE_OAUTH_LOGIN_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_OAUTH_TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";

    private static final String CLIENT_ID = "498003058158-kt4ggbs1ulv8v2aovqjf27qp94p6knrv.apps.googleusercontent.com";

    // Note that this isn't actually secret since it's in the source code. We might not need this.
    private static final String CLIENT_SECRET = "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";

    private final OkHttpClient httpClient;

    private AuthenticationClientListener listener;

    private HttpServer server;

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
            final HttpUrl url = HttpUrl.get("http://localhost" + req.getRequestURI());
            final String incomingState = url.queryParameter("state");
            final String code = url.queryParameter("code");

            server.stop(0);
            processOauthResponse(incomingState, outgoingState, code, codeVerifier, redirectUrl);

//            req.getResponseBody().write("Login successful. Please return to RuneLite.".getBytes(StandardCharsets.UTF_8));
//            req.sendResponseHeaders(200, 0);
            req.close();


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
            public void onFailure(Call call, IOException e) {
                System.out.println("error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                JsonObject responseJson = new Gson().fromJson(response.body().string(), JsonObject.class);
                String idToken = responseJson.get("id_token").getAsString();
                attemptAuthenticatedCall(idToken);
            }
        });
    }

    private String base64UrlEncodeNoPadding(byte[] source) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(source);
    }

    private void attemptAuthenticatedCall(String idToken) {
        TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
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

            OkHttpClient newClient = httpClient.newBuilder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) TRUST_ALL_CERTS)
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .build();
            Request r = new Request.Builder()
                    .url("https://localhost:7223/api/location")
                    .addHeader("Authorization", String.format("Bearer %s", idToken))
                    .build();

            newClient.newCall(r).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    System.out.println(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    System.out.println(response.body().string());
                }
            });
        } catch (Exception ignored) {

        }

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

    private String buildOauthLoginUrl() {
        return ":";
    }
}
