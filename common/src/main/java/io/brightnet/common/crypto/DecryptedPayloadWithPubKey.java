/*
 * This file is part of BrightNet.
 *
 * BrightNet is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * BrightNet is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with BrightNet. If not, see <http://www.gnu.org/licenses/>.
 */

package io.brightnet.common.crypto;

import java.io.Serializable;
import java.security.PublicKey;

public final class DecryptedPayloadWithPubKey implements Serializable {
    public final Serializable payload;
    public final PublicKey sigPublicKey;

    public DecryptedPayloadWithPubKey(Serializable payload, PublicKey sigPublicKey) {
        this.payload = payload;
        this.sigPublicKey = sigPublicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecryptedPayloadWithPubKey)) return false;

        DecryptedPayloadWithPubKey that = (DecryptedPayloadWithPubKey) o;

        if (payload != null ? !payload.equals(that.payload) : that.payload != null) return false;
        return !(sigPublicKey != null ? !sigPublicKey.equals(that.sigPublicKey) : that.sigPublicKey != null);

    }

    @Override
    public int hashCode() {
        int result = payload != null ? payload.hashCode() : 0;
        result = 31 * result + (sigPublicKey != null ? sigPublicKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DecryptedPayloadWithPubKey{" +
                "payload=" + payload +
                ", sigPublicKey.hashCode()=" + sigPublicKey.hashCode() +
                '}';
    }
}
