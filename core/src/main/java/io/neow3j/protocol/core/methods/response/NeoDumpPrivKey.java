package io.neow3j.protocol.core.methods.response;

import io.neow3j.protocol.core.Response;

public class NeoDumpPrivKey extends Response<String> {

    public String getDumpPrivKey() {
        return getResult();
    }

}