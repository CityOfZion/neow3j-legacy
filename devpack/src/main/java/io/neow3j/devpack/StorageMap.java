package io.neow3j.devpack;

import io.neow3j.script.InteropService;
import io.neow3j.script.OpCode;
import io.neow3j.devpack.annotations.Instruction;
import io.neow3j.types.StackItemType;

import static io.neow3j.devpack.Helper.toByteArray;

/**
 * A key-value view on the entries of smart contract's storage with a specific prefix.
 * <p>
 * Note that the storage size limit is 64 bytes for prefix + key and 65535 bytes for the value.
 */
public class StorageMap {

    private final StorageContext context;
    private final byte[] prefix;

    // region constructors

    /**
     * Constructs a new {@code StorageMap} from entries with the given prefix in the given {@link
     * StorageContext}.
     *
     * @param context The storage to look for the entries.
     * @param prefix  The prefix.
     */
    public StorageMap(StorageContext context, ByteString prefix) {
        this.context = context;
        this.prefix = prefix.toByteArray();
    }

    /**
     * Constructs a new {@code StorageMap} from entries with the given prefix in the given {@link
     * StorageContext}.
     *
     * @param context The storage to look for the entries.
     * @param prefix  The prefix.
     */
    public StorageMap(StorageContext context, byte[] prefix) {
        this.context = context;
        this.prefix = prefix;
    }

    /**
     * Constructs a new {@code StorageMap} from entries with the given prefix in the given {@link
     * StorageContext}.
     *
     * @param context The storage to look for the entries.
     * @param prefix  The prefix.
     */
    public StorageMap(StorageContext context, String prefix) {
        this.context = context;
        this.prefix = toByteArray(prefix);
    }

    /**
     * Constructs a new {@code StorageMap} from entries with the given prefix in the given {@link
     * StorageContext}.
     *
     * @param context The storage to look for the entries.
     * @param prefix  The prefix.
     */
    public StorageMap(StorageContext context, int prefix) {
        this.context = context;
        this.prefix = toByteArray(prefix);
    }

    /**
     * Constructs a new {@code StorageMap} from entries with the given prefix in the given {@link
     * StorageContext}.
     *
     * @param context The storage to look for the entries.
     * @param prefix  The prefix.
     */
    public StorageMap(StorageContext context, byte prefix) {
        this.context = context;
        this.prefix = toByteArray(prefix);
    }

    // endregion constructors
    // region delete

    /**
     * Deletes the entry with a key equal to {@code prefix + key} from the underlying storage
     * context.
     *
     * @param key The key to delete.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_DELETE)
    public native void delete(byte[] key);

    /**
     * Deletes the entry with a key equal to {@code prefix + key} from the underlying storage
     * context.
     *
     * @param key The key to delete.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_DELETE)
    public native void delete(ByteString key);

    /**
     * Deletes the entry with a key equal to {@code prefix + key} from the underlying storage
     * context.
     *
     * @param key The key to delete.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_DELETE)
    public native void delete(String key);

    /**
     * Deletes the entry with a key equal to {@code prefix + key} from the underlying storage
     * context.
     *
     * @param key The key to delete.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_DELETE)
    public native void delete(int key);

    // endregion delete
    // region get bytearray key

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native ByteString get(byte[] key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to a byte array.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to a byte array.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BUFFER_CODE)
    public native byte[] getByteArray(byte[] key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a string.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a string.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native String getString(byte[] key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a boolean.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a boolean.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BOOLEAN_CODE)
    public native Boolean getBoolean(byte[] key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getInt(byte[] key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * Returns 0, if no value is found for the provided key.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.DUP)
    @Instruction(opcode = OpCode.ISNULL)
    @Instruction(opcode = OpCode.JMPIFNOT, operand = 0x06)
    @Instruction(opcode = OpCode.DROP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.JMP, operand = 0x04)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getIntOrZero(byte[] key);

    // endregion get bytearray key
    // region get bytestring key

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native ByteString get(ByteString key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to a byte array.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to a byte array.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BUFFER_CODE)
    public native byte[] getByteArray(ByteString key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a string.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a string.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native String getString(ByteString key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a boolean.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a boolean.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BOOLEAN_CODE)
    public native Boolean getBoolean(ByteString key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getInt(ByteString key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * Returns 0, if no value is found for the provided key.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.DUP)
    @Instruction(opcode = OpCode.ISNULL)
    @Instruction(opcode = OpCode.JMPIFNOT, operand = 0x06)
    @Instruction(opcode = OpCode.DROP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.JMP, operand = 0x04)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getIntOrZero(ByteString key);

    // endregion get bytestring key
    // region get string key

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native ByteString get(String key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to a byte array.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to a byte array.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BUFFER_CODE)
    public native byte[] getByteArray(String key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a string.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a string.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native String getString(String key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a boolean.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a boolean.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BOOLEAN_CODE)
    public native Boolean getBoolean(String key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getInt(String key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * Returns 0, if no value is found for the provided key.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.DUP)
    @Instruction(opcode = OpCode.ISNULL)
    @Instruction(opcode = OpCode.JMPIFNOT, operand = 0x06)
    @Instruction(opcode = OpCode.DROP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.JMP, operand = 0x04)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getIntOrZero(String key);

    // endregion get string key
    // region get integer key

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native ByteString get(int key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to a byte array.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to a byte array.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BUFFER_CODE)
    public native byte[] getByteArray(int key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a string.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a string.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    public native String getString(int key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * as a boolean.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key as a boolean.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BOOLEAN_CODE)
    public native Boolean getBoolean(int key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getInt(int key);

    /**
     * Gets the value with a key equal to {@code prefix + key} from the underlying storage context
     * and converts it to an integer. The bytes are read in little-endian format. E.g., the byte
     * string {@code 0102} (in hexadecimal representation) is converted to 513.
     * <p>
     * Returns 0, if no value is found for the provided key.
     * <p>
     * This incurs the GAS cost of converting the {@code ByteString} value to an integer.
     *
     * @param key The key of the value to retrieve.
     * @return the value corresponding to the given key converted to an integer.
     */
    @Instruction(opcode = OpCode.OVER)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.SWAP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_GET)
    @Instruction(opcode = OpCode.DUP)
    @Instruction(opcode = OpCode.ISNULL)
    @Instruction(opcode = OpCode.JMPIFNOT, operand = 0x06)
    @Instruction(opcode = OpCode.DROP)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.JMP, operand = 0x04)
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.INTEGER_CODE)
    public native Integer getIntOrZero(int key);

    // endregion get integer key
    // region put bytearray key

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(byte[] key, byte[] value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(byte[] key, int value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(byte[] key, ByteString value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(byte[] key, String value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(byte[] key, Hash160 value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(byte[] key, Hash256 value);

    // endregion put bytearray key
    // region put bytestring key

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(ByteString key, byte[] value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(ByteString key, int value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(ByteString key, ByteString value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(ByteString key, String value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(ByteString key, Hash160 value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(ByteString key, Hash256 value);

    // endregion put bytestring key
    // region put string key

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(String key, String value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(String key, byte[] value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(String key, int value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(String key, ByteString value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(String key, Hash160 value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(String key, Hash256 value);

    // endregion put string key
    // region put integer key

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(int key, byte[] value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(int key, int value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(int key, ByteString value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(int key, String value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(int key, Hash160 value);

    /**
     * Stores the given key-value pair prefixed with this {@code StorageMap}'s prefix ({@code
     * prefix + key}) into the underlying storage context.
     *
     * @param key   The key of the entry.
     * @param value The value of the entry.
     */
    @Instruction(opcode = OpCode.PUSH2)
    @Instruction(opcode = OpCode.PICK)
    @Instruction(opcode = OpCode.PUSH1)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.CAT)
    @Instruction(opcode = OpCode.ROT)
    @Instruction(opcode = OpCode.PUSH0)
    @Instruction(opcode = OpCode.PICKITEM)
    @Instruction(interopService = InteropService.SYSTEM_STORAGE_PUT)
    public native void put(int key, Hash256 value);

    // endregion put integer key

    /**
     * Compares this {@code StorageMap} to the given object. The comparison happens by reference
     * only.
     *
     * @param other the object to compare with.
     * @return true if this and {@code other} reference the same storage map. False otherwise.
     */
    @Override
    @Instruction(opcode = OpCode.EQUAL)
    public native boolean equals(Object other);

    /**
     * Compares this and the given storage map by value, i.e., checks if they have the same context
     * and prefix.
     *
     * @param map Other storage map to compare to.
     * @return True if the two storage maps have the same context and prefix. False otherwise.
     */
    public boolean equals(StorageMap map) {
        if (this == map) {
            return true;
        }
        return context.equals(map.context)
                // The prefix is converted to a byte string for comparison because the neo-vm
                // does not compare Buffers (which byte[] is on the neo-vm) by value.
                && new ByteString(prefix).equals(new ByteString(map.prefix));
    }

}
