package com.friendmonitor.activityupdate.models;

public class ItemDrop extends ActivityUpdate {
    int itemId;

    public ItemDrop(int itemId, String accountHash) {
        super(accountHash, ActivityUpdateType.ITEM_DROP);
        this.itemId = itemId;
    }
}
