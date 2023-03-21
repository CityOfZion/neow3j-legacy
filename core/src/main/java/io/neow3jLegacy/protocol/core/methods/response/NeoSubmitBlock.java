package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoSubmitBlock extends Response<Boolean> {

    public Boolean getSubmitBlock() {
        return getResult();
    }

}