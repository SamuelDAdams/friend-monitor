package com.friendmonitor.activityupdate.models;

import net.runelite.api.Skill;

public class LevelUp extends ActivityUpdate {

    private Skill skill;
    private int level;

    public LevelUp(Skill skill, int level, String accountHash) {
        super(accountHash, ActivityUpdateType.LEVEL_UP);
        this.skill = skill;
        this.level = level;
    }
}
