package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetTxOut extends Response<TransactionOutput> {

    public TransactionOutput getTransaction() {
        return getResult();
    }

}
