package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

import java.math.BigInteger;

public class NeoGetWalletHeight extends Response<BigInteger> {

    public BigInteger getHeight() {
        return getResult();
    }

}
