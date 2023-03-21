package io.neow3jLegacy.protocol.exceptions;

import io.neow3jLegacy.protocol.core.Response.Error;

public class ErrorResponseException extends Exception {

    private Error error;

    public ErrorResponseException(Error error) {
        this.error = error;
    }

    public Error getError() {
        return error;
    }
}
