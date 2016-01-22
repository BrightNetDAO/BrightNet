package io.brightnet.p2p.storage.messages;

import io.brightnet.app.Version;
import io.brightnet.p2p.Message;

public abstract class DataBroadcastMessage implements Message {
    private final int networkId = Version.getNetworkId();

    @Override
    public int networkId() {
        return networkId;
    }

    @Override
    public String toString() {
        return "DataBroadcastMessage{" +
                "networkId=" + networkId +
                '}';
    }
}