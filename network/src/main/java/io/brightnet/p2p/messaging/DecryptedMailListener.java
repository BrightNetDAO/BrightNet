package io.brightnet.p2p.messaging;

import io.brightnet.p2p.NodeAddress;

public interface DecryptedMailListener {

    void onMailMessage(DecryptedMsgWithPubKey decryptedMsgWithPubKey, NodeAddress peerNodeAddress);
}
