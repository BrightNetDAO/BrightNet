package io.brightnet.p2p.storage.data;

import java.security.PublicKey;

public interface PubKeyProtectedExpirablePayload extends ExpirablePayload {
    PublicKey getPubKey();
}
