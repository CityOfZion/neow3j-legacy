package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetRawBlock extends Response<String> {

    public String getRawBlock() {
        return getResult();
    }

}
