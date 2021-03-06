package io.brightnet.p2p.peers.messages.auth;

import io.brightnet.app.Version;
import io.brightnet.p2p.NodeAddress;

public final class AuthenticationRequest extends AuthenticationMessage {
    // That object is sent over the wire, so we need to take care of version compatibility.
    private static final long serialVersionUID = Version.NETWORK_PROTOCOL_VERSION;

    public final long requesterNonce;

    public AuthenticationRequest(NodeAddress senderNodeAddress, long requesterNonce) {
        super(senderNodeAddress);
        this.requesterNonce = requesterNonce;
    }

    @Override
    public String toString() {
        return "AuthenticationRequest{" +
                "senderAddress=" + senderNodeAddress +
                ", requesterNonce=" + requesterNonce +
                super.toString() + "} ";
    }
}
