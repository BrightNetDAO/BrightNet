package io.brightnet.p2p;

import java.io.Serializable;

public interface Message extends Serializable {
    int networkId();
}
