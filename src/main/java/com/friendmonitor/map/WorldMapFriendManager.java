package com.friendmonitor.map;

import com.friendmonitor.account.runescape.socket.server.FriendLocationUpdate;

public interface WorldMapFriendManager {
    void stop();
    void updateFriendLocations(FriendLocationUpdate[] updates);
}
