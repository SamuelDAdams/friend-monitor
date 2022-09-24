package com.friendmonitor.panel;

import com.friendmonitor.FriendMonitorPlugin;
import net.runelite.client.ui.PluginPanel;

public class FriendMonitorPanel extends PluginPanel {

    FriendMonitorPlugin plugin;


    public FriendMonitorPanel(FriendMonitorPlugin plugin) {
        super(false);
        this.plugin = plugin;

    }
}
