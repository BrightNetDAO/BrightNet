package io.brightnet.p2p.network;

import io.brightnet.p2p.Message;

public interface MessageListener {
    void onMessage(Message message, Connection connection);
}
