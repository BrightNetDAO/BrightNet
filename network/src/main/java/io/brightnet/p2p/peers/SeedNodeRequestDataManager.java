package io.brightnet.p2p.peers;

import io.brightnet.common.UserThread;
import io.brightnet.p2p.NodeAddress;
import io.brightnet.p2p.network.Connection;
import io.brightnet.p2p.network.NetworkNode;
import io.brightnet.p2p.storage.P2PDataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SeedNodeRequestDataManager extends RequestDataManager {
    private static final Logger log = LoggerFactory.getLogger(SeedNodeRequestDataManager.class);

    public SeedNodeRequestDataManager(NetworkNode networkNode, P2PDataStorage dataStorage, PeerManager peerManager) {
        super(networkNode, dataStorage, peerManager);
    }

    @Override
    public void onPeerAuthenticated(NodeAddress peerNodeAddress, Connection connection) {
        //TODO not clear which use case is handles here...
        if (dataStorage.getMap().isEmpty()) {
            if (requestDataFromAuthenticatedSeedNodeTimer == null)
                requestDataFromAuthenticatedSeedNodeTimer = UserThread.runAfterRandomDelay(()
                        -> requestDataFromAuthenticatedSeedNode(peerNodeAddress, connection), 2, 5, TimeUnit.SECONDS);
        }
        super.onPeerAuthenticated(peerNodeAddress, connection);
    }
}
