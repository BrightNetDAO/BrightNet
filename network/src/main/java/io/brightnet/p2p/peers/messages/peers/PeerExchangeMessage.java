package io.brightnet.p2p.peers.messages.peers;

import io.brightnet.app.Version;
import io.brightnet.p2p.Message;

public abstract class PeerExchangeMessage implements Message {
    private final int networkId = Version.getNetworkId();

    @Override
    public int networkId() {
        return networkId;
    }

    @Override
    public String toString() {
        return ", networkId=" + networkId +
                '}';
    }
}
