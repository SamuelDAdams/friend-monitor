package com.friendmonitor.models.activityupdate;

public class QuestComplete extends ActivityUpdate {

    private int questId;

    public QuestComplete(int questId, long accountHash) {
        super(accountHash, ActivityUpdateType.QUEST_COMPLETE);
        this.questId = questId;
    }
}
