package io.brightnet.p2p.peers;

import io.brightnet.p2p.NodeAddress;
import io.brightnet.p2p.network.Connection;

public interface AuthenticationListener {
    void onPeerAuthenticated(NodeAddress peerNodeAddress, Connection connection);
}
