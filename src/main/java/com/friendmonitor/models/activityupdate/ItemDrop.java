package com.friendmonitor.models.activityupdate;

public class ItemDrop extends ActivityUpdate {
    int itemId;

    public ItemDrop(int itemId, int accountHash) {
        super(accountHash, ActivityUpdateType.ITEM_DROP);
        this.itemId = itemId;
    }
}
