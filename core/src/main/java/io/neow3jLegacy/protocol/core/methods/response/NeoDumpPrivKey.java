package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoDumpPrivKey extends Response<String> {

    public String getDumpPrivKey() {
        return getResult();
    }

}