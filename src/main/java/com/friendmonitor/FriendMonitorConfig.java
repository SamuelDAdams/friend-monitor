package com.friendmonitor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("example")
public interface FriendMonitorConfig extends Config
{
	@ConfigSection(
			name = "Settings",
			description = "Settings for how location and notifications are handled",
			position = 0
	)
	String settings = "Settings";
	@ConfigItem(
		keyName = "broadcastLocation",
		name = "Broadcast Location",
		description = "Whether or not to broadcast your location to friends you have registered.",
		section = settings,
		position = 0
	)
	default boolean getBroadcastLocationEnabled()
	{
		return false;
	}
	@ConfigItem(
			keyName = "sendUpdates",
			name = "Send activity updates",
			description = "Whether or not to broadcast your skill level-ups and quest completions to friends you have registered.",
			section = settings,
			position = 0
	)
	default boolean getSendActivityUpdatesEnabled()
	{
		return true;
	}
	@ConfigItem(
			keyName = "showUpdates",
			name = "Show activity updates",
			description = "Whether or not to display activity updates of friends you have registered.",
			section = settings,
			position = 0
	)
	default boolean getShowActivityUpdatesEnabled()
	{
		return true;
	}
	@ConfigItem(
			keyName = "privacyMode",
			name = "Privacy mode",
			description = "Disable sending and receiving updates when private chat is off.",
			section = settings,
			position = 0
	)
	default boolean getPrivacyModeEnabled()
	{
		return false;
	}


}
