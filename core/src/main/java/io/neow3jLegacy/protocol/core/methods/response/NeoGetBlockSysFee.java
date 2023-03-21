package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetBlockSysFee extends Response<String> {

    public String getFee() {
        return getResult();
    }

}
