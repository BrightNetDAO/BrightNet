package io.brightnet.p2p;


import io.brightnet.p2p.network.SetupListener;

public interface P2PServiceListener extends SetupListener {

    void onRequestingDataCompleted();

    void onNoSeedNodeAvailable();

    void onNoPeersAvailable();

    void onFirstPeerAuthenticated();
}
