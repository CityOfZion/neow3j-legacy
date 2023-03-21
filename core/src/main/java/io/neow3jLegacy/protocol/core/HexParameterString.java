package io.neow3jLegacy.protocol.core;

import com.fasterxml.jackson.annotation.JsonValue;
import io.neow3jLegacy.utils.Numeric;

import java.nio.charset.StandardCharsets;

/**
 * Represents a parameter as a raw string.
 * This class converts to string hexadecimal.
 */
public class HexParameterString implements HexParameter {

    private String param;

    public HexParameterString(String paramAsString) {
        this.param = paramAsString;
    }

    @Override
    @JsonValue
    public String getHexValue() {
        return Numeric.toHexStringNoPrefix(this.param.getBytes(StandardCharsets.UTF_8));
    }
}
