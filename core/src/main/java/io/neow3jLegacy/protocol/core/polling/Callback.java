package io.neow3jLegacy.protocol.core.polling;

/**
 * Filter callback interface.
 */
public interface Callback<T> {
    void onEvent(T value);
}
