package com.friendmonitor.activityupdate.models;

public class PlayerDeath extends ActivityUpdate {
    int x;
    int y;
    int plane;
    int world;

    public PlayerDeath(String accountHash, int x, int y, int plane, int world) {
        super(accountHash, ActivityUpdateType.PLAYER_DEATH);

        this.x = x;
        this.y = y;
        this.plane = plane;
        this.world = world;
    }
}
