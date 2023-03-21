package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoSendRawTransaction extends Response<Boolean> {

    public Boolean getSendRawTransaction() {
        return getResult();
    }

}