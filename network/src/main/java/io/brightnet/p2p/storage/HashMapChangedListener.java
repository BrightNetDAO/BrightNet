package io.brightnet.p2p.storage;

import io.brightnet.p2p.storage.data.ProtectedData;

public interface HashMapChangedListener {
    void onAdded(ProtectedData entry);

    void onRemoved(ProtectedData entry);
}
