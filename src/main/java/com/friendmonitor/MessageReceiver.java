package com.friendmonitor;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.Console;

interface MessageReceiverListener {
    void onMessage(String text);
    void onFailure();
}

public class MessageReceiver extends WebSocketListener {
    private final MessageReceiverListener listener;

    public MessageReceiver(MessageReceiverListener listener) {
        this.listener = listener;
    }
    @Override
    public void onOpen(WebSocket websocket, Response response) {
        System.out.println(response);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {

    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(1000, null);
        System.out.println("Closing: " + code + " " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        listener.onFailure();
    }
}
