package com.friendmonitor;

import com.friendmonitor.models.activityupdate.ActivityUpdate;
import com.friendmonitor.models.activityupdate.Location;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

import java.io.Console;

public class FriendMonitorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		testJson();
		//ExternalPluginManager.loadBuiltin(FriendMonitorPlugin.class);
		//RuneLite.main(args);
	}

	static void testJson() {
		Gson gson = new GsonBuilder()
				.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
				//.registerTypeAdapter(ActivityUpdate.class, new PolymorphicDeserializer<ActivityUpdate>())
				.create();

		ActivityUpdate update = new Location(0, 0, 0, 0);

		String json = gson.toJson(update);

		System.out.println(json);
	}
}