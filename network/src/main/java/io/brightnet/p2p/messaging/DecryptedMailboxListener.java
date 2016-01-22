package io.brightnet.p2p.messaging;

import io.brightnet.p2p.NodeAddress;

public interface DecryptedMailboxListener {

    void onMailboxMessageAdded(DecryptedMsgWithPubKey decryptedMsgWithPubKey, NodeAddress senderNodeAddress);
}
