package com.friendmonitor.map;

import com.friendmonitor.FriendMonitorPlugin;
import com.friendmonitor.account.runescape.socket.server.FriendLocationUpdate;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;


import java.awt.image.BufferedImage;
import java.util.HashMap;

public class WorldMapFriendManagerImpl implements WorldMapFriendManager{
    static final BufferedImage ARROW = ImageUtil.loadImageResource(FriendMonitorPlugin.class, "clue_arrow.png");
    private final HashMap<String, WorldMapPoint> friendLocations;
    private final WorldMapPointManager worldMapManager;

    public WorldMapFriendManagerImpl(WorldMapPointManager worldMapManager) {
        this.worldMapManager = worldMapManager;
        this.friendLocations = new HashMap<>();
    }

    @Override
    public void stop() {
        for (WorldMapPoint point : friendLocations.values()) {
            worldMapManager.remove(point);
        }
    }

    @Override
    public void updateFriendLocations(FriendLocationUpdate[] updates) {
        for (FriendLocationUpdate update : updates) {
            if (friendLocations.containsKey(update.getAccountHash())) {
                friendLocations
                    .get(update.getAccountHash())
                    .setWorldPoint(
                        new WorldPoint(
                            update.getX(),
                            update.getY(),
                            update.getPlane()
                        )
                    );
            } else {

                WorldMapPoint point = new WorldMapPoint(
                    new WorldPoint(
                        update.getX(),
                        update.getY(),
                        update.getPlane()
                    ),
                    ARROW
                );

                point.setName(update.getDisplayName());
                point.setTooltip(update.getDisplayName());

                friendLocations.put(update.getAccountHash(), point);

                worldMapManager.add(point);
            }
        }
    }
}
