package com.friendmonitor.models.activityupdate;

import net.runelite.api.Skill;

public class LevelUp extends ActivityUpdate {

    private Skill skill;
    private int level;

    public LevelUp(Skill skill, int level, int accountHash) {
        super(accountHash, ActivityUpdateType.LEVEL_UP);
        this.skill = skill;
        this.level = level;
    }
}
