package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoConnectionCount extends Response<Integer> {

    public Integer getCount() {
        return getResult();
    }

}
