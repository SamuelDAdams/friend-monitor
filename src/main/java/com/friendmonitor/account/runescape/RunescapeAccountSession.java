package com.friendmonitor.account.runescape;

import com.friendmonitor.activityupdate.ActivityUpdateBroadcaster;

public interface RunescapeAccountSession {
    String getAccountHash();
    ActivityUpdateBroadcaster getActivityUpdateBroadcaster();

    void setWorldMapIsShowing(boolean worldMapIsShowing);

    void setListener(RunescapeAccountSessionListener listener);
    void stop();
}

