package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetApplicationLog extends Response<NeoApplicationLog> {

    public NeoApplicationLog getApplicationLog() {
        return getResult();
    }

}
