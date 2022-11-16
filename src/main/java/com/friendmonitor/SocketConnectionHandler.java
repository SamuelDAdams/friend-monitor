package com.friendmonitor;

import lombok.Getter;
import lombok.Setter;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

public class SocketConnectionHandler implements SocketMessageReceiverListener {
    private WebSocket server;

    @SuppressWarnings("FieldCanBeLocal")
    private SocketMessageReceiver receiver;

    private MessageSender sender;

    @Getter
    @Setter
    SocketConnectionListener connectionListener;

    private final String accountHash;

    private final OkHttpClient httpClient;

    private final HttpUrl socketUrl;

    public SocketConnectionHandler(String accountHash, OkHttpClient httpClient, HttpUrl socketUrl) {
        this.accountHash = accountHash;
        this.httpClient = httpClient;
        this.socketUrl = socketUrl;
    }

    public void close() {
        server.close(1000, null);
    }

    public void sendMessage(@NotNull String message) {
        if (sender != null) {
            sender.sendMessage(message);
        }
    }

    @Override
    public void onMessage(@NotNull String message) {
        connectionListener.onSocketMessage(message);
    }

    @Override
    public void onFailure() {
        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                start();
                t.cancel();
            }
        };

        t.schedule(task, 5000);
    }

    public void start() {
        Request request = new Request.Builder()
                .url(socketUrl.newBuilder().addPathSegment(accountHash).build())
                .build();

        receiver = new SocketMessageReceiver(this);
        server = httpClient.newWebSocket(request, receiver);
        sender = new MessageSender(server);
    }
}
