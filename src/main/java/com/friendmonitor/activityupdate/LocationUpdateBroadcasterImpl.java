package com.friendmonitor.activityupdate;

import com.friendmonitor.activityupdate.models.Location;
import com.google.gson.Gson;
import lombok.NonNull;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;

import java.io.IOException;

public class LocationUpdateBroadcasterImpl implements LocationUpdateBroadcaster {
    private final HttpUrl activityUpdateUrl;
    private int updateCountAtCurrentLocation = 0;
    private Location previousUpdate;

    private final OkHttpClient httpClient;
    private final Gson gson;

    private Call currentCall;

    public LocationUpdateBroadcasterImpl(OkHttpClient httpClient, Gson gson, HttpUrl activityUpdateUrl) {
        this.httpClient = httpClient;
        this.activityUpdateUrl = activityUpdateUrl;
        this.gson = gson;
    }

    @Override
    public synchronized void broadcastLocationUpdate(Location update) {
        if (previousUpdate != null && previousUpdate.isSameLocationAndAccountAs(update) && updateCountAtCurrentLocation < 13) {
            updateCountAtCurrentLocation += 1;
            System.out.println("Skipping location update");
        } else {
            if (currentCall != null) {
                currentCall.cancel();
                currentCall = null;
            }
            System.out.println("Sending location update now");

            previousUpdate = update;
            updateCountAtCurrentLocation = 0;

            Request r = new Request.Builder()
                    .url(activityUpdateUrl)
                    .post(RequestBody.create(RuneLiteAPI.JSON, gson.toJson(update)))
                    .build();

            currentCall = httpClient.newCall(r);

            currentCall.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callFinished(false);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    callFinished(response.isSuccessful());
                }
            });
        }
    }

    private synchronized void callFinished(boolean succeeded) {
        if (!succeeded) {
            previousUpdate = null;
            updateCountAtCurrentLocation = 0;
        }

        System.out.println("call finished");
        currentCall = null;
    }
}
