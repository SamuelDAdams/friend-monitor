package com.friendmonitor;

import okhttp3.WebSocket;

public class MessageSender {
    private final WebSocket socket;

    public MessageSender(WebSocket socket) {
        this.socket = socket;
    }

    void sendMessage(String message) {
        socket.send(message);
    }
}
