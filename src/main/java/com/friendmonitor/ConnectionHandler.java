package com.friendmonitor;

import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

interface ConnectionListener {
    void onMessage(String message);
}

class ConnectionHandler implements MessageReceiverListener {

    @Getter
    private WebSocket server;

    private MessageReceiver receiver;

    private MessageSender sender;

    private final ConnectionListener connectionListener;

    private final Long accountHash;

    ConnectionHandler(Long accountHash, ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
        this.accountHash = accountHash;

        startSocketConnection();
    }

    void close() {
        server.close(1000, null);
    }

    void sendMessage(String message) {
        sender.sendMessage(message);
    }

    @Override
    public void onMessage(String message) {
        connectionListener.onMessage(message);
    }

    @Override
    public void onFailure() {
        startSocketConnection();
    }

    private void startSocketConnection() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("ws://localhost:7223/").addHeader("account-hash", accountHash.toString()).build();
        receiver = new MessageReceiver(this);
        server = client.newWebSocket(request, receiver);
        client.dispatcher().executorService().shutdown();
        sender = new MessageSender(server);
    }
}
