package com.friendmonitor.account.runescape;

import com.friendmonitor.account.runescape.socket.server.FriendDeathMessage;

public interface RunescapeAccountSessionListener {
    void invokeOnClientThread(Runnable r);
    void friendDied(FriendDeathMessage message);
}
