package com.friendmonitor;

import com.friendmonitor.activityupdate.models.ActivityUpdate;
import com.friendmonitor.activityupdate.models.Location;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FriendMonitorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FriendMonitorPlugin.class);
		RuneLite.main(args);
	}
}