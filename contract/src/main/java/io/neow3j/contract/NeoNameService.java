package io.neow3j.contract;

import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.RecordType;
import io.neow3j.protocol.core.methods.response.InvocationResult;
import io.neow3j.protocol.core.methods.response.MapStackItem;
import io.neow3j.protocol.core.methods.response.NameState;
import io.neow3j.protocol.core.methods.response.StackItem;
import io.neow3j.utils.Numeric;
import io.neow3j.wallet.Wallet;
import org.bouncycastle.util.IPAddress;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static io.neow3j.contract.ContractParameter.byteArray;
import static io.neow3j.contract.ContractParameter.hash160;
import static io.neow3j.contract.ContractParameter.integer;
import static io.neow3j.contract.ContractParameter.string;
import static io.neow3j.model.types.StackItemType.MAP;
import static java.util.Collections.singletonList;

/**
 * Represents the NameService native contract and provides methods to invoke its functions.
 */
public class NeoNameService extends NonFungibleToken {

    public final static String NAME = "NameService";
    public final static long NEF_CHECKSUM = 3740064217L;
    public static final ScriptHash SCRIPT_HASH = getScriptHashOfNativeContract(NEF_CHECKSUM, NAME);

    private static final String ADD_ROOT = "addRoot";
    private static final String SET_PRICE = "setPrice";
    private static final String GET_PRICE = "getPrice";
    private static final String IS_AVAILABLE = "isAvailable";
    private static final String REGISTER = "register";
    private static final String RENEW = "renew";
    private static final String SET_ADMIN = "setAdmin";
    private static final String SET_RECORD = "setRecord";
    private static final String GET_RECORD = "getRecord";
    private static final String DELETE_RECORD = "deleteRecord";
    private static final String RESOLVE = "resolve";

    private static final String PROPERTIES = "properties";

    private static final BigInteger MAXIMAL_PRICE = new BigInteger("1000000000000");
    private static final Pattern ROOT_REGEX_PATTERN = Pattern.compile("^[a-z][a-z0-9]{0,15}$");
    private static final Pattern NAME_REGEX_PATTERN =
            Pattern.compile("^(?=.{3,255}$)([a-z0-9]{1,62}\\.)+[a-z][a-z0-9]{0,15}$");

    /**
     * Constructs a new {@code NeoToken} that uses the given {@link Neow3j} instance for
     * invocations.
     *
     * @param neow the {@link Neow3j} instance to use for invocations.
     */
    public NeoNameService(Neow3j neow) {
        super(SCRIPT_HASH, neow);
    }

    /**
     * Returns the name of the NeoToken contract. Doesn't require a call to the Neo node.
     *
     * @return the name.
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Creates a transaction script to add a root domain and initializes a
     * {@link TransactionBuilder} based on this script.
     * <p>
     * Only committee members are allowed to add a new root domain.
     *
     * @param root the new root domain.
     * @return a transaction builder.
     */
    public TransactionBuilder addRoot(String root) {
        if (!rootRegexMatches(root)) {
            throw new IllegalArgumentException("The provided root domain is not allowed.");
        }
        return invokeFunction(ADD_ROOT, string(root));
    }

    private boolean rootRegexMatches(String root) {
        return ROOT_REGEX_PATTERN.matcher(root).matches();
    }

    /**
     * Creates a transaction script to set the price for registering a domain and initializes a
     * {@link TransactionBuilder} based on this script.
     * <p>
     * Only committee members are allowed to set the price.
     *
     * @param price the price for registering a domain.
     * @return a transaction builder.
     */
    public TransactionBuilder setPrice(BigInteger price) {
        if (!isValidPrice(price)) {
            throw new IllegalArgumentException("The price needs to be greater than 0 and smaller " +
                    "than 1_000_000_000_000.");
        }
        return invokeFunction(SET_PRICE, integer(price));
    }

    // true if the price is in the allowed range, false otherwise.
    private boolean isValidPrice(BigInteger price) {
        return price.compareTo(BigInteger.ZERO) > 0 &&
                price.compareTo(MAXIMAL_PRICE) < 0;
    }

    /**
     * Gets the price to register a domain.
     *
     * @return the price to register a domain.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public BigInteger getPrice() throws IOException {
        return callFuncReturningInt(GET_PRICE);
    }

    /**
     * Checks if the specified second domain name is available.
     *
     * @param name the domain name.
     * @return true if the domain name is available, false otherwise.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public boolean isAvailable(String name) throws IOException {
        checkRegexMatch(NAME_REGEX_PATTERN, name);
        return callFuncReturningBool(IS_AVAILABLE, string(name));
    }

    private boolean nameRegexMatches(String name) {
        return NAME_REGEX_PATTERN.matcher(name).matches();
    }

    /**
     * Creates a transaction script to register a new domain and initializes a
     * {@link TransactionBuilder} based on this script.
     *
     * @param name  the domain name.
     * @param owner the address of the domain owner.
     * @return a transaction builder.
     */
    public TransactionBuilder register(String name, ScriptHash owner) {
        return invokeFunction(REGISTER, string(name), hash160(owner));
    }

    /**
     * Creates a transaction script to update the TTL of the domain name and initializes a
     * {@link TransactionBuilder} based on this script.
     * <p>
     * Each call will extend the validity period of the domain name by one year.
     * <p>
     * Only supports to renew the second domain name.
     *
     * @param name the domain name.
     * @return a transaction builder.
     */
    public TransactionBuilder renew(String name) {
        return invokeFunction(RENEW, string(name));
    }

    /**
     * Creates a transaction script to set the admin for the specified domain name and
     * initializes a {@link TransactionBuilder} based on this script.
     *
     * @param name  the domain name.
     * @param admin the hash of the admin address. TODO: check what address exactly
     * @return a transaction builder.
     */
    public TransactionBuilder setAdmin(String name, ScriptHash admin) {
        // only committee allowed?
        return invokeFunction(SET_ADMIN, string(name), hash160(admin));
    }

    /**
     * Creates a transaction script to set the type of the specified domain name and the
     * corresponding type data and initializes a {@link TransactionBuilder} based on this script.
     *
     * @param name the domain name.
     * @param type the record type.
     * @param data the corresponding data.
     * @return a transaction builder.
     */
    // TODO: 25.02.21 Michael: Needs to verify the signature of the admin or the owner of the
    //  domain.
    //  -> check if anything further is necessary for this.
    public TransactionBuilder setRecord(String name, RecordType type, String data) {
        // everyone allowed?
        if (!dataMatchesRecordTypeRegex(type, data)) {
            throw new IllegalArgumentException("The provided name is not allowed.");
        }
        return invokeFunction(SET_RECORD, string(name), integer(type.byteValue()), string(data));
    }

    private boolean dataMatchesRecordTypeRegex(RecordType type, String data) {
        if (type.equals(RecordType.A)) {
            return IPAddress.isValidIPv4(data);
        } else if (type.equals(RecordType.CNAME)) {
            return nameRegexMatches(data);
        } else if (type.equals(RecordType.TXT)) {
            return data.length() <= 255;
        } else {
            return IPAddress.isValidIPv6(data);
        }
    }

    /**
     * Gets the type data of the domain.
     *
     * @param name the domain name.
     * @param type the record type.
     * @return a transaction builder.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public String getRecord(String name, RecordType type) throws IOException {
        return callFuncReturningString(GET_RECORD, string(name), integer(type.byteValue()));
        // returns a Base64-encoded string of the corresponding type data of the domain name.
        // TODO: 25.02.21 Michael: check if Base64 is parsed.
    }

    /**
     * Creates a transaction script to delete the record data initializes a
     * {@link TransactionBuilder} based on this script.
     *
     * @param name the domain name.
     * @param type the record type.
     * @return a transaction builder.
     */
    // Needs to verify the signature of the admin or the owner of the domain name.
    public TransactionBuilder deleteRecord(String name, RecordType type) {
        return invokeFunction(DELETE_RECORD, string(name), integer(type.byteValue()));
    }

    /**
     * Resolves a domain name.
     *
     * @param domain the domain.
     * @param type   the record type.
     * @return the resolution result.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public String resolve(String domain, RecordType type) throws IOException {
        return "";
        // returns the Base64-encoded string of the resolution result.
        // TODO: 25.02.21 Michael: check if Base64 is parsed.
    }

    /**
     * Gets the owner of the domain name.
     *
     * @param domain the domain name.
     * @return the owner of the domain name.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public ScriptHash ownerOf(String domain) throws IOException {
        return ownerOf(domain.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gets the properties of the domain name.
     *
     * @param domain the domain name.
     * @return the properties of the domain name as {@link NameState}.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public NameState properties(String domain) throws IOException {
        return properties(domain.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gets the properties of the domain name.
     *
     * @param domain the domain name.
     * @return the properties of the domain name as {@link NameState}.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    @Override
    public NameState properties(byte[] domain) throws IOException {
        String domainAsString = Numeric.hexToString(Numeric.toHexString(domain));
        if (!nameRegexMatches(domainAsString)) {
            throw new IllegalArgumentException("The provided domain, '" + domainAsString + "'," +
                    "does not match the required regex.");
        }

        InvocationResult invocationResult =
                callInvokeFunction(PROPERTIES, singletonList(byteArray(domain)))
                        .getInvocationResult();

        if (invocationResult.getException() != null) {
            throw new IllegalArgumentException("The properties for the domain '" + domainAsString +
                    "' could not be fetched. The vm returned the exception '" +
                    invocationResult.getException() + "'.");
        }
        return deserializeProperties(invocationResult);
    }

    private NameState deserializeProperties(InvocationResult invocationResult) {
        try {
            StackItem stackItem = invocationResult.getStack().get(0);

            if (!stackItem.getType().equals(MAP)) {
                throw new UnexpectedReturnTypeException(stackItem.getType(), MAP);
            }

            MapStackItem map = stackItem.asMap();
            StackItem name = map.get("name");
            StackItem description = map.get("description");
            StackItem expiration = map.get("expiration");

            return new NameState(name.asByteString().getAsString(),
                    description.asByteString().getAsString(),
                    expiration.asInteger().getValue().intValue());

        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param wallet the wallet.
     * @param to     the receiver of the domain.
     * @param domain the domain.
     * @return a transaction builder.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public TransactionBuilder transfer(Wallet wallet, ScriptHash to, String domain)
            throws IOException {
        checkRegexMatch(NAME_REGEX_PATTERN, domain);
        return transfer(wallet, to, domain.getBytes(StandardCharsets.UTF_8));
    }

    // checks if an input matches the provided regex pattern.
    private void checkRegexMatch(Pattern pattern, byte[] input) {
        checkRegexMatch(pattern, Numeric.hexToString(Numeric.toHexString(input)));
    }

    // checks if an input matches the provided regex pattern.
    private void checkRegexMatch(Pattern pattern, String input) {
        if (!nameRegexMatches(input)) {
            throw new IllegalArgumentException("The provided input, '" + input + "'," +
                    "does not match the required regex.");
        }
    }
}
