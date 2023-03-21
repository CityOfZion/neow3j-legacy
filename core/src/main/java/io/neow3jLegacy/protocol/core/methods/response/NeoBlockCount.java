package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

import java.math.BigInteger;

public class NeoBlockCount extends Response<BigInteger> {

    public BigInteger getBlockIndex() {
        return getResult();
    }

}
