package com.friendmonitor.account.runescape.socket.server;

import lombok.Getter;
import lombok.Setter;

public class LocationUpdateMessage extends ServerSocketMessage {
    @Getter
    @Setter
    public FriendLocationUpdate[] updates;
}
