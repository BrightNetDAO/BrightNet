package io.brightnet.p2p.storage.data;

import java.io.Serializable;

public interface ExpirablePayload extends Serializable {
    long getTTL();
}
