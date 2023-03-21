package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetRawTransaction extends Response<String> {

    public String getRawTransaction() {
        return getResult();
    }

}
