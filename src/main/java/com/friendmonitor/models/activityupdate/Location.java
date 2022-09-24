package com.friendmonitor.models.activityupdate;

public class Location extends ActivityUpdate {
    int x;
    int y;
    int plane;

    public Location(int x, int y, int plane, long accountHash) {
        super(accountHash, ActivityUpdateType.LOCATION);
        this.x = x;
        this.y = y;
        this.plane = plane;
    }
}
