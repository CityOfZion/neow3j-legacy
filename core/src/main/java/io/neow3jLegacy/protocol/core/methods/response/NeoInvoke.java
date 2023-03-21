package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.protocol.core.Response;

public class NeoInvoke extends Response<InvocationResult> {

    public InvocationResult getInvocationResult() {
        return getResult();
    }

}
