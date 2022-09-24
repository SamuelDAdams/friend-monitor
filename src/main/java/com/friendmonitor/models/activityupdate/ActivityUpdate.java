package com.friendmonitor.models.activityupdate;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

enum ActivityUpdateType {
    @SerializedName("LEVEL_UP") LEVEL_UP,
    @SerializedName("QUEST_COMPLETE") QUEST_COMPLETE,
    @SerializedName("LOCATION") LOCATION,
    @SerializedName("ITEM_DROP") ITEM_DROP,
    @SerializedName("BOSS_KILL_COUNT") BOSS_KILL_COUNT
}

public abstract class ActivityUpdate {
    long accountHash;
    long timestamp;
    ActivityUpdateType type;

    public ActivityUpdate(long accountHash, ActivityUpdateType type) {
        this.accountHash = accountHash;
        this.type = type;
        this.timestamp = Instant.now().getEpochSecond();
    }
}
