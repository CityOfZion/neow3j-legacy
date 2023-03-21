package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoGetBlock extends Response<NeoBlock> {

    public NeoBlock getBlock() {
        return getResult();
    }

}
