package io.brightnet.crypto;

import io.brightnet.app.Version;
import io.brightnet.common.crypto.SealedAndSigned;
import io.brightnet.p2p.NodeAddress;
import io.brightnet.p2p.messaging.MailboxMessage;

import java.util.Arrays;

public final class SealedAndSignedMessage implements MailboxMessage {
    // That object is sent over the wire, so we need to take care of version compatibility.
    private static final long serialVersionUID = Version.NETWORK_PROTOCOL_VERSION;

    private final int networkId = Version.getNetworkId();
    public final SealedAndSigned sealedAndSigned;
    public final byte[] addressPrefixHash;

    public SealedAndSignedMessage(SealedAndSigned sealedAndSigned, byte[] addressPrefixHash) {
        this.sealedAndSigned = sealedAndSigned;
        this.addressPrefixHash = addressPrefixHash;
    }

    @Override
    public NodeAddress getSenderNodeAddress() {
        return null;
    }

    @Override
    public int networkId() {
        return networkId;
    }

    @Override
    public String toString() {
        return "SealedAndSignedMessage{" +
                "networkId=" + networkId +
                ", sealedAndSigned=" + sealedAndSigned +
                ", receiverAddressMaskHash.hashCode()=" + Arrays.toString(addressPrefixHash).hashCode() +
                '}';
    }
}
