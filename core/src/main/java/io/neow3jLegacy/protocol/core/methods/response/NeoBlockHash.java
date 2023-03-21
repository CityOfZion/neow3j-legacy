package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoBlockHash extends Response<String> {

    public String getBlockHash() {
        return getResult();
    }

}