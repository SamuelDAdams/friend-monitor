package com.friendmonitor;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;

interface SocketMessageReceiverListener {
    void onMessage(String text);
    void onFailure();
}

public class SocketMessageReceiver extends WebSocketListener {
    private final SocketMessageReceiverListener listener;

    public SocketMessageReceiver(SocketMessageReceiverListener listener) {
        this.listener = listener;
    }
    @Override
    public void onOpen(@NotNull WebSocket websocket, @NotNull Response response) {
        System.out.println(response);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        listener.onMessage(text);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, @NotNull String reason) {
        webSocket.close(1000, null);
        System.out.println("Closing: " + code + " " + reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
        listener.onFailure();
    }
}
