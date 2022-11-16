package com.friendmonitor.activityupdate;

import com.friendmonitor.activityupdate.models.ActivityUpdate;
import com.friendmonitor.activityupdate.models.Location;
import com.google.gson.Gson;
import lombok.NonNull;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;

import java.io.IOException;

public class ActivityUpdateBroadcasterImpl implements ActivityUpdateBroadcaster {

    private final LocationUpdateBroadcaster locationUpdateBroadcaster;
    private final OkHttpClient httpClient;
    private final HttpUrl activityUpdateUrl;
    private final Gson gson;

    public ActivityUpdateBroadcasterImpl(LocationUpdateBroadcaster locationUpdateBroadcaster, OkHttpClient httpClient, HttpUrl activityUpdateUrl, Gson gson) {
        this.locationUpdateBroadcaster = locationUpdateBroadcaster;
        this.httpClient = httpClient;
        this.activityUpdateUrl = activityUpdateUrl;
        this.gson = gson;
    }

    @Override
    public void broadcastActivityUpdate(ActivityUpdate update) {
        if (update.getClass() == Location.class) {
            locationUpdateBroadcaster.broadcastLocationUpdate((Location) update);
        } else {
            Request r = new Request.Builder()
                    .url(activityUpdateUrl)
                    .post(RequestBody.create(RuneLiteAPI.JSON, gson.toJson(update)))
                    .build();

            httpClient.newCall(r).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    // TODO failure?
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        // TODO failure?
                    }
                }
            });
        }
    }
}
