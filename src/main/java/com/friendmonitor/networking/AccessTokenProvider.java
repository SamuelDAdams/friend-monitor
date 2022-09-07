package com.friendmonitor.networking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AccessTokenProvider {
    @NotNull String getAccessToken();
    @Nullable String refreshAccessToken();
}
