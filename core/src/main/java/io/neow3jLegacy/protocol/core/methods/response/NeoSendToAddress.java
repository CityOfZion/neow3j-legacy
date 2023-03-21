package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoSendToAddress extends Response<Transaction> {

    public Transaction getSendToAddress() {
        return getResult();
    }

}