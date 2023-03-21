package io.neow3jLegacy.protocol.core.methods.response;

import io.neow3jLegacy.model.types.StackItemType;

import java.math.BigInteger;

public class IntegerStackItem extends StackItem {

    public IntegerStackItem(BigInteger value) {
        super(StackItemType.INTEGER, value);
    }

    @Override
    public BigInteger getValue() {
        return (BigInteger) this.value;
    }
}
