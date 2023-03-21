package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoSendMany extends Response<Transaction> {

    public Transaction getSendMany() {
        return getResult();
    }

}