package io.brightnet.p2p.network;

public interface SetupListener {
    void onTorNodeReady();

    void onHiddenServicePublished();

    void onSetupFailed(Throwable throwable);
}
