package com.friendmonitor.activityupdate;

import com.friendmonitor.activityupdate.models.ActivityUpdate;
import com.friendmonitor.activityupdate.models.Location;
import com.google.gson.Gson;
import lombok.NonNull;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;

import java.io.IOException;

public interface ActivityUpdateBroadcaster {
    void broadcastActivityUpdate(ActivityUpdate update);
}

