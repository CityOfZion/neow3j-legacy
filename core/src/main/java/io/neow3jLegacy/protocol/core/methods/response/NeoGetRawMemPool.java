package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

import java.util.List;

public class NeoGetRawMemPool extends Response<List<String>> {

    public List<String> getAddresses() {
        return getResult();
    }

}
