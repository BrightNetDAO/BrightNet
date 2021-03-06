package io.brightnet.p2p;

import io.brightnet.app.Log;
import io.brightnet.p2p.peers.PeerManager;
import io.brightnet.p2p.peers.RequestDataManager;
import io.brightnet.p2p.peers.SeedNodePeerManager;
import io.brightnet.p2p.peers.SeedNodeRequestDataManager;
import io.brightnet.p2p.seed.SeedNodesRepository;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.monadic.MonadicBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SeedNodeP2PService extends P2PService {
    private static final Logger log = LoggerFactory.getLogger(SeedNodeP2PService.class);

    public SeedNodeP2PService(SeedNodesRepository seedNodesRepository,
                              NodeAddress mySeedNodeNodeAddress,
                              File torDir,
                              boolean useLocalhost,
                              int networkId,
                              File storageDir) {
        super(seedNodesRepository, mySeedNodeNodeAddress.port, torDir, useLocalhost, networkId, storageDir, null, null);

        // we remove ourselves from the list of seed nodes
        seedNodeNodeAddresses.remove(mySeedNodeNodeAddress);
    }

    @Override
    protected PeerManager getNewPeerManager() {
        return new SeedNodePeerManager(networkNode);
    }

    @Override
    protected RequestDataManager getNewRequestDataManager() {
        return new SeedNodeRequestDataManager(networkNode, dataStorage, peerManager);
    }

    @Override
    protected MonadicBinding<Boolean> getNewReadyForAuthenticationBinding() {
        return EasyBind.combine(hiddenServicePublished, notAuthenticated,
                (hiddenServicePublished, notAuthenticated) -> hiddenServicePublished && notAuthenticated);
    }

    @Override
    public void onTorNodeReady() {
        Log.traceCall();
        p2pServiceListeners.stream().forEach(e -> e.onTorNodeReady());
    }

    @Override
    protected void authenticateToSeedNode() {
        Log.traceCall();
        ((SeedNodePeerManager) peerManager).authenticateToSeedNode();
    }

}
