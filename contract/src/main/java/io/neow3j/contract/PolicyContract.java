package io.neow3j.contract;

import io.neow3j.constants.InteropServiceCode;
import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.model.types.StackItemType;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.methods.response.NeoSendRawTransaction;
import io.neow3j.protocol.core.methods.response.StackItem;
import io.neow3j.wallet.Wallet;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Policy contract and provides methods to invoke it.
 */
public class PolicyContract extends SmartContract{

    private static final ScriptHash SCRIPT_HASH = ScriptHash.fromScript(
            new ScriptBuilder().sysCall(InteropServiceCode.NEO_NATIVE_POLICY).toArray());

    private static final String GET_MAX_TRANSACTIONS_PER_BLOCK = "getMaxTransactionsPerBlock";
    private static final String GET_FEE_PER_BYTE = "getFeePerByte";
    private static final String GET_BLOCKED_ACCOUNTS = "getBlockedAccounts";
    private static final String SET_FEE_PER_BYTE = "setFeePerByte";
    private static final String SET_MAX_TX_PER_BLOCK = "setMaxTransactionsPerBlock";
    private static final String BLOCK_ACCOUNT = "blockAccount";
    private static final String UNBLOCK_ACCOUNT = "unblockAccount";

    /**
     * Constructs a new <tt>PolicyContract</tt> that uses the given {@link Neow3j} instance for
     * invocations.
     *
     * @param neow The {@link Neow3j} instance to use for invocations.
     */
    public PolicyContract(Neow3j neow) {
        super(SCRIPT_HASH, neow);
    }

    /**
     * Returns the maximal amount of transactions allowed per block.
     *
     * @return the maximal amount of transactions allowed per block.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public Integer getMaxTransactionsPerBlock() throws IOException {
        return callFuncReturningInt(GET_MAX_TRANSACTIONS_PER_BLOCK).intValue();
    }

    /**
     * Gets the system fee per byte.
     *
     * @return the system fee per byte.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public Integer getFeePerByte() throws IOException {
        return callFuncReturningInt(GET_FEE_PER_BYTE).intValue();
    }

    /**
     * Gets the list of Accounts that are blocked.
     *
     * @return list of blocked Accounts.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public List<ScriptHash> getBlockedAccounts() throws IOException {
        StackItem arrayItem = invokeFunction(GET_BLOCKED_ACCOUNTS).getInvocationResult().getStack().get(0);
        if (!arrayItem.getType().equals(StackItemType.ARRAY)) {
            throw new UnexpectedReturnTypeException(arrayItem.getType(), StackItemType.ARRAY);
        }

        return arrayItem.asArray().getValue()
                .stream().map(ac -> ScriptHash.fromAddress(ac.asByteString().getAsAddress()))
                .collect(Collectors.toList());
    }

    /**
     * Creates and sends a transaction that sets the fee per byte.
     *
     * @param fee       The fee per byte.
     * @param wallet    The wallet that contains the account authorised to invoke the policy contract.
     * @param sender    The authorised account.
     * @return the response from the neo-node.
     * @throws IOException if something goes wrong when communicating with the neo-node.
     */
    public NeoSendRawTransaction setFeePerByte(Integer fee, Wallet wallet, ScriptHash sender)
            throws IOException {

        return buildSetFeePerByteInvocation(fee, wallet, sender).send();
    }

    // Method extracted for testability.
    Invocation buildSetFeePerByteInvocation(Integer fee, Wallet wallet, ScriptHash sender)
            throws IOException {

        return invoke(SET_FEE_PER_BYTE)
                .withSender(sender)
                .withWallet(wallet)
                .withParameters(ContractParameter.integer(fee))
                .build()
                .sign();
    }

    /**
     * Creates and sends a transaction that sets the maximal allowed number of transactions per block.
     *
     * @param maxTxPerBlock The maximal allowed number of transactions per block.
     * @param wallet        The wallet that contains the account authorised to invoke the policy contract.
     * @param sender        The authorised account.
     * @return the response from the neo-node.
     * @throws IOException if something goes wrong when communicating with the neo-node.
     */
    public NeoSendRawTransaction setMaxTransactionsPerBlock(Integer maxTxPerBlock,
            Wallet wallet, ScriptHash sender)
            throws IOException {

        return buildSetMaxTxPerBlockInvocation(maxTxPerBlock, wallet, sender).send();
    }

    // Method extracted for testability.
    Invocation buildSetMaxTxPerBlockInvocation(Integer maxTxPerBlock, Wallet wallet, ScriptHash sender)
            throws IOException {

        return invoke(SET_MAX_TX_PER_BLOCK)
                .withSender(sender)
                .withWallet(wallet)
                .withParameters(ContractParameter.integer(maxTxPerBlock))
                .build()
                .sign();
    }

    /**
     * Creates and sends a transaction that adds an Account to the blacklist of the neo-network.
     *
     * @param scriptHash    The account that is blocked.
     * @param wallet        The wallet that contains the account authorised to invoke the policy contract.
     * @param sender        The authorised account.
     * @return the response from the neo-node.
     * @throws IOException if something goes wrong when communicating with the neo-node.
     */
    public NeoSendRawTransaction blockAccount(ScriptHash scriptHash, Wallet wallet, ScriptHash sender)
            throws IOException {

        return buildBlockAccountInvocation(scriptHash, wallet, sender).send();
    }

    // Method extracted for testability.
    Invocation buildBlockAccountInvocation(ScriptHash scriptHash, Wallet wallet, ScriptHash sender)
            throws IOException {

        return invoke(BLOCK_ACCOUNT)
                .withSender(sender)
                .withWallet(wallet)
                .withParameters(ContractParameter.hash160(scriptHash))
                .build()
                .sign();
    }

    /**
     * Creates and sends a transaction that removes an Account from the blacklist of the neo-network.
     *
     * @param scriptHash    The account that is unblocked.
     * @param wallet        The wallet that contains the account authorised to invoke the policy contract.
     * @param sender        The authorised account.
     * @return the response from the neo-node.
     * @throws IOException if something goes wrong when communicating with the neo-node.
     */
    public NeoSendRawTransaction unblockAccount(ScriptHash scriptHash, Wallet wallet, ScriptHash sender)
            throws IOException {

        return buildUnblockAccountInvocation(scriptHash, wallet, sender).send();
    }

    // Method extracted for testability.
    Invocation buildUnblockAccountInvocation(ScriptHash scriptHash, Wallet wallet, ScriptHash sender)
            throws IOException {

        return invoke(UNBLOCK_ACCOUNT)
                .withSender(sender)
                .withWallet(wallet)
                .withParameters(ContractParameter.hash160(scriptHash))
                .build()
                .sign();
    }
}