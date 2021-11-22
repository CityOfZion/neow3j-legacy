package io.neow3j.transaction.witnessrule;

import io.neow3j.serialization.BinaryReader;
import io.neow3j.serialization.BinaryWriter;
import io.neow3j.serialization.NeoSerializable;
import io.neow3j.serialization.exceptions.DeserializationException;

import java.io.IOException;

public abstract class WitnessCondition extends NeoSerializable {

    private static final int MAX_SUBITEMS = 16;
    private static final int MAX_NESTING_DEPTH = 2;

    protected WitnessConditionType type;

    @Override
    public void deserialize(BinaryReader reader) throws DeserializationException {
        try {
            if (!WitnessConditionType.valueOf(reader.readByte()).equals(this.type)) {
                throw new DeserializationException("The deserialized type does not match the type "
                        + "information in the serialized data.");
            }
            deserializeWithoutType(reader);
        } catch (IOException e) {
            throw new DeserializationException(e);
        }
    }

    protected abstract void deserializeWithoutType(BinaryReader reader) throws IOException,
            DeserializationException;

    @Override
    public void serialize(BinaryWriter writer) throws IOException {
        writer.writeByte(type.byteValue());
        serializeWithoutType(writer);
    }

    protected abstract void serializeWithoutType(BinaryWriter writer) throws IOException;

    public WitnessConditionType getType() {
        return type;
    }
}
