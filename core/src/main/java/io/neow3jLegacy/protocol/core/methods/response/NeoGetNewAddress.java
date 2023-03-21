package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetNewAddress extends Response<String> {

    public String getAddress() {
        return getResult();
    }

}
