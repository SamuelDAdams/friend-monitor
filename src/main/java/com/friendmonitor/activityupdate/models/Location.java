package com.friendmonitor.activityupdate.models;

public class Location extends ActivityUpdate {
    int x;
    int y;
    int plane;
    int world;

    public Location(int x, int y, int plane, int world, String accountHash) {
        super(accountHash, ActivityUpdateType.LOCATION);
        this.x = x;
        this.y = y;
        this.world = world;
        this.plane = plane;
    }

    public boolean isSameLocationAndAccountAs(Location other) {
        if (this == other) {
            return true;
        } else if (other == null) {
            return false;
        } else {
            return accountHash.equals(other.accountHash) &&
                    x == other.x &&
                    y == other.y &&
                    plane == other.plane &&
                    world == other.world;
        }
    }
}
