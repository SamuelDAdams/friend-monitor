package com.friendmonitor.account.runescape.socket.client;

import lombok.Getter;
import lombok.Setter;

public class LocationUpdateSpeedMessage extends ClientSocketMessage {
    @Getter
    @Setter
    private LocationUpdateSpeed speed;

    public LocationUpdateSpeedMessage(LocationUpdateSpeed speed) {
        this.speed = speed;
    }
}
