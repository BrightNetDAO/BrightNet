package io.brightnet.p2p.messaging;

public interface SendMailboxMessageListener {
    void onArrived();

    void onStoredInMailbox();

    void onFault();
}
