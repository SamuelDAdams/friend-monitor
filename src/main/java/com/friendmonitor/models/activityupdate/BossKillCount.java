package com.friendmonitor.models.activityupdate;

public class BossKillCount extends ActivityUpdate {

    private int npcId;
    private int count;

    public BossKillCount(int npcId, int count, long accountHash) {
        super(accountHash, ActivityUpdateType.BOSS_KILL_COUNT);
        this.npcId = npcId;
        this.count = count;
    }
}
