package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetTransaction extends Response<Transaction> {

    public Transaction getTransaction() {
        return getResult();
    }

}
