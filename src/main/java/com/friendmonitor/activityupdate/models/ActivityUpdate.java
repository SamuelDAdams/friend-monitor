package com.friendmonitor.activityupdate.models;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.UUID;

enum ActivityUpdateType {
    @SerializedName("LEVEL_UP") LEVEL_UP,
    @SerializedName("QUEST_COMPLETE") QUEST_COMPLETE,
    @SerializedName("LOCATION") LOCATION,
    @SerializedName("ITEM_DROP") ITEM_DROP,
    @SerializedName("BOSS_KILL_COUNT") BOSS_KILL_COUNT,

    @SerializedName("PLAYER_DEATH") PLAYER_DEATH
}

public abstract class ActivityUpdate {
    String accountHash;
    String id;
    long timestamp;
    ActivityUpdateType type;

    public ActivityUpdate(String accountHash, ActivityUpdateType type) {
        this.accountHash = accountHash;
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.timestamp = Instant.now().toEpochMilli();

    }
}
