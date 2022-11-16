package com.friendmonitor.activityupdate.models;

public class QuestComplete extends ActivityUpdate {
    private int questId;

    public QuestComplete(int questId, String accountHash) {
        super(accountHash, ActivityUpdateType.QUEST_COMPLETE);
        this.questId = questId;
    }
}
