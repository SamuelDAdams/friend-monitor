package com.friendmonitor.account.runescape;

import com.friendmonitor.SocketConnectionHandler;
import com.friendmonitor.SocketConnectionListener;
import com.friendmonitor.account.runescape.socket.client.ClientSocketMessage;
import com.friendmonitor.account.runescape.socket.client.LocationUpdateSpeed;
import com.friendmonitor.account.runescape.socket.client.LocationUpdateSpeedMessage;
import com.friendmonitor.account.runescape.socket.server.FriendDeathMessage;
import com.friendmonitor.account.runescape.socket.server.LocationUpdateMessage;
import com.friendmonitor.account.runescape.socket.server.ServerSocketMessage;
import com.friendmonitor.activityupdate.ActivityUpdateBroadcaster;
import com.friendmonitor.map.WorldMapFriendManager;
import com.friendmonitor.util.RuntimeTypeAdapterFactory;
import com.google.gson.Gson;
import net.runelite.api.Friend;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class RunescapeAccountSessionImpl implements RunescapeAccountSession, SocketConnectionListener {
    private final String accountHash;
    private final ActivityUpdateBroadcaster activityUpdateBroadcaster;
    private final SocketConnectionHandler socketConnection;
    private final WorldMapFriendManager worldMapFriendManager;

    private final Gson gson;

    private RunescapeAccountSessionListener listener;
    public RunescapeAccountSessionImpl(String accountHash, ActivityUpdateBroadcaster activityUpdateBroadcaster, WorldMapFriendManager worldMapFriendManager, OkHttpClient httpClient, HttpUrl socketUrl, Gson gson) {
        this.accountHash = accountHash;
        this.activityUpdateBroadcaster = activityUpdateBroadcaster;
        this.socketConnection = new SocketConnectionHandler(accountHash, httpClient, socketUrl);
        this.worldMapFriendManager = worldMapFriendManager;

        RuntimeTypeAdapterFactory<ServerSocketMessage> serverSocketMessageFactory = RuntimeTypeAdapterFactory
            .of(ServerSocketMessage.class, "type")
            .registerSubtype(LocationUpdateMessage.class, "LOCATION")
            .registerSubtype(FriendDeathMessage.class, "FRIEND_DEATH");

        RuntimeTypeAdapterFactory<ClientSocketMessage> clientSocketMessageFactory = RuntimeTypeAdapterFactory
            .of(ClientSocketMessage.class, "type")
            .registerSubtype(LocationUpdateSpeedMessage.class, "LOCATION_UPDATE_SPEED");

        this.gson = gson
            .newBuilder()
            .registerTypeAdapterFactory(serverSocketMessageFactory)
            .registerTypeAdapterFactory(clientSocketMessageFactory)
            .create();

        socketConnection.setConnectionListener(this);
        socketConnection.start();
    }

    @Override
    public void setListener(RunescapeAccountSessionListener listener) {
        this.listener = listener;
    }

    @Override
    public String getAccountHash() {
        return accountHash;
    }

    @Override
    public void stop() {
        socketConnection.close();
        worldMapFriendManager.stop();
    }

    @Override
    public ActivityUpdateBroadcaster getActivityUpdateBroadcaster() {
        return activityUpdateBroadcaster;
    }

    @Override
    public void onSocketMessage(String message) {
        try {
            ServerSocketMessage socketMessage = gson.fromJson(message, ServerSocketMessage.class);

            if (socketMessage.getClass() == LocationUpdateMessage.class) {
                LocationUpdateMessage locationUpdateMessage = (LocationUpdateMessage) socketMessage;

                listener.invokeOnClientThread(() -> worldMapFriendManager.updateFriendLocations(locationUpdateMessage.updates));
            } else if (socketMessage.getClass() == FriendDeathMessage.class) {
                FriendDeathMessage friendDeathMessage = (FriendDeathMessage) socketMessage;
                listener.friendDied(friendDeathMessage);
            }

            else {
                // ?????
            }


        } catch (Exception ignored) {

        }
    }

    @Override
    public void setWorldMapIsShowing(boolean worldMapIsShowing) {
        LocationUpdateSpeed speed = worldMapIsShowing ? LocationUpdateSpeed.FAST : LocationUpdateSpeed.SLOW;

        ClientSocketMessage message = new LocationUpdateSpeedMessage(speed);

        String json = gson.toJson(message, ClientSocketMessage.class);

        socketConnection.sendMessage(json);
    }
}
