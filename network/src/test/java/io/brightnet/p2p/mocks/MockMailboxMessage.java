package io.brightnet.p2p.mocks;

import io.brightnet.app.Version;
import io.brightnet.p2p.NodeAddress;
import io.brightnet.p2p.messaging.MailboxMessage;
import io.brightnet.p2p.storage.data.ExpirablePayload;

public final class MockMailboxMessage implements MailboxMessage, ExpirablePayload {
    private final int networkId = Version.getNetworkId();
    public final String msg;
    public final NodeAddress senderNodeAddress;
    public long ttl;

    public MockMailboxMessage(String msg, NodeAddress senderNodeAddress) {
        this.msg = msg;
        this.senderNodeAddress = senderNodeAddress;
    }

    @Override
    public int networkId() {
        return networkId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MockMailboxMessage)) return false;

        MockMailboxMessage that = (MockMailboxMessage) o;

        return !(msg != null ? !msg.equals(that.msg) : that.msg != null);

    }

    @Override
    public int hashCode() {
        return msg != null ? msg.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MockData{" +
                "msg='" + msg + '\'' +
                '}';
    }

    @Override
    public long getTTL() {
        return ttl;
    }

    @Override
    public NodeAddress getSenderNodeAddress() {
        return senderNodeAddress;
    }
}
