package io.brightnet.p2p.storage.messages;

import io.brightnet.app.Version;
import io.brightnet.p2p.storage.data.ProtectedData;

public final class AddDataMessage extends DataBroadcastMessage {
    // That object is sent over the wire, so we need to take care of version compatibility.
    private static final long serialVersionUID = Version.NETWORK_PROTOCOL_VERSION;

    public final ProtectedData data;

    public AddDataMessage(ProtectedData data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddDataMessage)) return false;

        AddDataMessage that = (AddDataMessage) o;

        return !(data != null ? !data.equals(that.data) : that.data != null);
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "AddDataMessage{" +
                "data=" + data +
                "} " + super.toString();
    }
}
