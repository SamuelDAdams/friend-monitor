package com.friendmonitor.activityupdate;

import com.friendmonitor.activityupdate.models.Location;
import com.google.gson.Gson;
import lombok.NonNull;
import lombok.Synchronized;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface LocationUpdateBroadcaster {
    void broadcastLocationUpdate(Location update);
}
