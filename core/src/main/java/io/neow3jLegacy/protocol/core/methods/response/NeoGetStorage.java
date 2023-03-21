package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetStorage extends Response<String> {

    public String getStorage() {
        return getResult();
    }

}
