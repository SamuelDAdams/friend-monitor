package com.friendmonitor.account;

public class CreateRunescapeAccountModel {
    String accountHash;
    String displayName;

    public CreateRunescapeAccountModel(String accountHash, String displayName) {
        this.accountHash = accountHash;
        this.displayName = displayName;
    }
}
