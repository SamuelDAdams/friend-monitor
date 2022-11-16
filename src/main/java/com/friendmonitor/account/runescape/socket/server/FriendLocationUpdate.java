package com.friendmonitor.account.runescape.socket.server;

import lombok.Getter;
import lombok.Setter;

public class FriendLocationUpdate {
    @Getter
    @Setter
    int x;

    @Getter
    @Setter
    int y;

    @Getter
    @Setter
    int plane;

    @Getter
    @Setter
    String displayName;

    @Getter
    @Setter
    String accountHash;
}
