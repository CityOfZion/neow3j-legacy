package io.neow3j.contract;

import io.neow3j.contract.exceptions.UnexpectedReturnTypeException;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.methods.response.NeoSendRawTransaction;
import io.neow3j.transaction.Cosigner;
import io.neow3j.transaction.VerificationScript;
import io.neow3j.wallet.Account;
import io.neow3j.wallet.Wallet;
import io.neow3j.wallet.exceptions.InsufficientFundsException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a NEP-5 token contract and provides methods to invoke it.
 */
public class Nep5Token extends SmartContract {

    private static final String NEP5_NAME = "name";
    private static final String NEP5_TOTAL_SUPPLY = "totalSupply";
    private static final String NEP5_SYMBOL = "symbol";
    private static final String NEP5_DECIMALS = "decimals";
    private static final String NEP5_BALANCE_OF = "balanceOf";
    private static final String NEP5_TRANSFER = "transfer";

    private String name;
    // It is expected that Nep5 contracts return the total supply in fractions of their token.
    // Therefore an integer is used here instead of a decimal number.
    private BigInteger totalSupply;
    private Integer decimals;
    private String symbol;

    /**
     * Constructs a new {@code Nep5Token} representing the token contract with the given script
     * hash. Uses the given {@link Neow3j} instance for all invocations.
     *
     * @param scriptHash The token contract's script hash
     * @param neow       The {@link Neow3j} instance to use for invocations.
     */
    public Nep5Token(ScriptHash scriptHash, Neow3j neow) {
        super(scriptHash, neow);
    }

    /**
     * Gets the name of this token.
     * <p>
     * The return value is retrieved form the neo-node only once and then cached.
     *
     * @return the name.
     * @throws IOException                   if there was a problem fetching information from the
     *                                       Neo node.
     * @throws UnexpectedReturnTypeException if the contract invocation did not return something
     *                                       interpretable as a string.
     */
    public String getName() throws IOException, UnexpectedReturnTypeException {
        if (this.name == null) {
            this.name = callFuncReturningString(NEP5_NAME);
        }
        return this.name;
    }

    /**
     * Gets the symbol of this token.
     * <p>
     * The return value is retrieved form the neo-node only once and then cached.
     *
     * @return the symbol.
     * @throws IOException                   if there was a problem fetching information from the
     *                                       Neo node.
     * @throws UnexpectedReturnTypeException if the contract invocation did not return something
     *                                       interpretable as a string.
     */
    public String getSymbol() throws IOException, UnexpectedReturnTypeException {
        if (this.symbol == null) {
            this.symbol = callFuncReturningString(NEP5_SYMBOL);
        }
        return this.symbol;
    }

    /**
     * Gets the total supply of this token in fractions.
     * <p>
     * The return value is retrieved form the neo-node only once and then cached.
     *
     * @return the total supply.
     * @throws IOException                   if there was a problem fetching information from the
     *                                       Neo node.
     * @throws UnexpectedReturnTypeException if the contract invocation did not return something
     *                                       interpretable as a number.
     */
    public BigInteger getTotalSupply() throws IOException, UnexpectedReturnTypeException {
        if (this.totalSupply == null) {
            this.totalSupply = callFuncReturningInt(NEP5_TOTAL_SUPPLY);
        }
        return this.totalSupply;
    }

    /**
     * Gets the number of fractions that one unit of this token can be divided into.
     * <p>
     * The return value is retrieved form the neo-node only once and then cached.
     *
     * @return the the number of fractions.
     * @throws IOException                   if there was a problem fetching information from the
     *                                       Neo node.
     * @throws UnexpectedReturnTypeException if the contract invocation did not return something
     *                                       interpretable as a number.
     */
    public int getDecimals() throws IOException, UnexpectedReturnTypeException {
        if (this.decimals == null) {
            this.decimals = callFuncReturningInt(NEP5_DECIMALS).intValue();
        }
        return this.decimals;
    }

    /**
     * Gets the token balance for the given account script hash.
     * <p>
     * The token amount is returned in token fractions. E.g., an amount of 1 GAS is returned as
     * 1*10^8 GAS fractions.
     * <p>
     * The balance is not cached locally. Every time this method is called requests are send to the
     * neo-node.
     *
     * @param scriptHash The script hash of the account to fetch the balance for.
     * @return the token balance.
     * @throws IOException                   if there was a problem fetching information from the
     *                                       Neo node.
     * @throws UnexpectedReturnTypeException if the contract invocation did not return something
     *                                       interpretable as a number.
     */
    public BigInteger getBalanceOf(ScriptHash scriptHash) throws IOException,
            UnexpectedReturnTypeException {

        ContractParameter ofParam = ContractParameter.hash160(scriptHash);
        return callFuncReturningInt(NEP5_BALANCE_OF, ofParam);
    }

    /**
     * Gets the token balance for the given wallet, i.e., all accounts in the wallet.
     * <p>
     * The token amount is returned in token fractions. E.g., an amount of 1 GAS is returned as
     * 1*10^8 GAS fractions.
     * <p>
     * The balance is not cached locally. Every time this method is called requests are send to the
     * neo-node.
     *
     * @param wallet The wallet to fetch the balance for.
     * @return the token balance.
     * @throws IOException                   if there was a problem fetching information from the
     *                                       Neo node.
     * @throws UnexpectedReturnTypeException if the contract invocation did not return something
     *                                       interpretable as a number.
     */
    public BigInteger getBalanceOf(Wallet wallet) throws IOException,
            UnexpectedReturnTypeException {

        BigInteger sum = BigInteger.ZERO;
        for (Account a : wallet.getAccounts()) {
            sum = sum.add(getBalanceOf(a.getScriptHash()));
        }
        return sum;
    }

    /**
     * Creates and sends a transfer transaction that uses all accounts in the wallet to cover the amount.
     * <p>
     * The default account is used first to cover the amount. If it cannot cover the full amount, the other accounts in
     * the wallet are iterated one by one to cover the remaining amount. If the amount can be covered, all necessary
     * transfers are sent in one transaction.
     *
     * @param wallet The wallet from which to send the tokens from.
     * @param to     The script hash of the receiver.
     * @param amount The amount to transfer as a decimal number (not token fractions).
     * @return The transaction id.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public NeoSendRawTransaction transfer(Wallet wallet, ScriptHash to, BigDecimal amount)
            throws IOException {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("The parameter amount must be greater than or equal to 0");
        }

        return buildTransactionScript(wallet, to, amount).send();
    }

    // Method extracted for testability.
    Invocation buildTransactionScript(Wallet wallet, ScriptHash to, BigDecimal amount)
            throws IOException {
        BigInteger amountStillToCover = getAmountAsBigInteger(amount);

        // Use default account first.
        Account defaultAccount = wallet.getDefaultAccount();
        BigInteger balanceDefaultAcc = getBalanceOf(defaultAccount.getScriptHash());

        // List of the individual invocation scripts.
        List<byte[]> scripts = new ArrayList<>();
        List<ScriptHash> signers = new ArrayList<>();
        // If the amount is covered by the wallet's default account, build and send the transaction.
        if (balanceDefaultAcc.signum() > 0) {
            if (!defaultAccount.isMultiSig() ||
                    privateKeysArePresentForMultiSig(wallet, defaultAccount.getScriptHash())) {
                signers.add(defaultAccount.getScriptHash());
                if (balanceDefaultAcc.subtract(amountStillToCover).signum() >= 0) {
                    // Full amount can be covered by default account.
                    scripts.add(buildSingleTransferScript(defaultAccount, to, amountStillToCover));
                    return buildTransferInvocation(wallet, scripts, signers);
                } else {
                    // Amount exceeds balance, therefore, full balance of default account is used.
                    scripts.add(buildSingleTransferScript(defaultAccount, to, balanceDefaultAcc));
                    amountStillToCover = amountStillToCover.subtract(balanceDefaultAcc);
                }
            }
        }

        // If there is still an amount to cover, use other accounts in the wallet until the amount is covered.
        List<Account> accounts = wallet.getAccounts();
        for (Account acc : accounts) {
            // If acc is a multi-sig account, verify that it can be used. Otherwise, skip this account.
            if (acc.isMultiSig()) {
                if (!privateKeysArePresentForMultiSig(wallet, acc.getScriptHash())) {
                    continue;
                }
            }
            BigInteger balance = getBalanceOf(acc.getScriptHash());
            if (acc.isDefault() || balance.signum() <= 0) {
                continue;
            }
            signers.add(acc.getScriptHash());
            if (balance.subtract(amountStillToCover).signum() >= 0) {
                // Full remaining amount can be covered by current account.
                scripts.add(buildSingleTransferScript(acc, to, amountStillToCover));
                return buildTransferInvocation(wallet, scripts, signers);
            } else {
                scripts.add(buildSingleTransferScript(acc, to, balance));
                amountStillToCover = amountStillToCover.subtract(balance);
            }
        }
        if (scripts.size() == 0) {
            throw new IllegalArgumentException("Can't create transaction signature. Wallet does not contain" +
                    " enough accounts (with decrypted private keys) that are part of the multi-sig account(s)" +
                    " in the wallet.");
        }

        BigInteger maxCoverPotential = getAmountAsBigInteger(amount).subtract(amountStillToCover);
        throw new InsufficientFundsException("The wallet does not hold enough tokens." +
                " The transfer amount is " + getAmountAsBigInteger(amount).toString() + " " + getSymbol() +
                " but the wallet only holds " + maxCoverPotential.toString() + " " + getSymbol() +
                " (in token fractions).");
    }

    /**
     * Creates and sends a transfer transaction that uses the provided accounts.
     * <p>
     * The accounts are used in the order provided to cover the transaction amount.
     * If the first account cannot cover the full amount, the second account is used to cover the remaining
     * amount and so on. If the amount can be covered, all necessary transfers are sent in one transaction.
     *
     * @param wallet The wallet from which to send the tokens from.
     * @param to     The script hash of the receiver.
     * @param amount The amount to transfer as a decimal number (not token fractions).
     * @param from   The script hashes of the accounts in the wallet that should be used to cover the amount.
     * @return The transaction id.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public NeoSendRawTransaction transferFromSpecificAccounts(Wallet wallet, ScriptHash to,
            BigDecimal amount, ScriptHash... from)
            throws IOException {
        if (from.length == 0) {
            throw new IllegalArgumentException("An account address must be provided to build an invocation.");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("The parameter amount must be greater than or equal to 0");
        }
        // Verify that potential multi-sig accounts can be used.
        for (ScriptHash fromScriptHash : from) {
            if (wallet.getAccount(fromScriptHash).isMultiSig()) {
                if (!privateKeysArePresentForMultiSig(wallet, fromScriptHash)) {
                    throw new IllegalArgumentException("Can't create transaction signature. Wallet does not contain" +
                            "enough accounts (with decrypted private keys) that are part of the multi-sig account" +
                            "with script hash " + fromScriptHash + ".");
                }
            }
        }

        return buildTransactionScript(wallet, to, amount, from).send();
    }

    // Method extracted for testability.
    Invocation buildTransactionScript(Wallet wallet, ScriptHash to,
            BigDecimal amount, ScriptHash... from) throws IOException {

        BigInteger amountStillToCover = getAmountAsBigInteger(amount);
        // List of the individual invocation scripts.
        List<byte[]> scripts = new ArrayList<>();
        List<ScriptHash> signers = new ArrayList<>();

        // If there is still an amount to cover, use other accounts in the wallet until the amount is covered.
        for (ScriptHash scriptHash : from) {
            Account acc = wallet.getAccount(scriptHash);
            BigInteger balance = getBalanceOf(acc.getScriptHash());
            if (balance.signum() <= 0) {
                continue;
            }
            signers.add(acc.getScriptHash());

            if (balance.subtract(amountStillToCover).signum() >= 0) {
                // Full remaining amount can be covered by current account.
                scripts.add(buildSingleTransferScript(acc, to, amountStillToCover));
                return buildTransferInvocation(wallet, scripts, signers);
            } else {
                scripts.add(buildSingleTransferScript(acc, to, balance));
                amountStillToCover = amountStillToCover.subtract(balance);
            }
        }

        BigInteger maxCoverPotential = getAmountAsBigInteger(amount).subtract(amountStillToCover);
        throw new InsufficientFundsException("The provided accounts do not hold enough tokens." +
                " The transfer amount is " + getAmountAsBigInteger(amount).toString() + " " + getSymbol() +
                " but the provided accounts only hold " + maxCoverPotential.toString() + " " + getSymbol() +
                " (in token fractions).");
    }

    private byte[] buildSingleTransferScript(Account acc, ScriptHash to, BigInteger amount) {
        List<ContractParameter> params = Arrays.asList(
                ContractParameter.hash160(acc.getScriptHash()),
                ContractParameter.hash160(to),
                ContractParameter.integer(amount));

        return new ScriptBuilder().contractCall(scriptHash, NEP5_TRANSFER, params).toArray();
    }

    private Invocation buildTransferInvocation(Wallet wallet, List<byte[]> scripts, List<ScriptHash> signers)
            throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] script : scripts) {
            byteArrayOutputStream.write(script);
        }
        byte[] concatenatedScript = byteArrayOutputStream.toByteArray();

        Invocation.Builder invocationBuilder = new Invocation.Builder(neow)
                .withWallet(wallet)
                .withScript(concatenatedScript)
                .withSender(signers.get(0))
                .failOnFalse();

        for (ScriptHash signer : signers) {
            invocationBuilder.withAttributes(Cosigner.calledByEntry(signer));
        }

        return invocationBuilder.build().sign();
    }

    private boolean privateKeysArePresentForMultiSig(Wallet wallet, ScriptHash multiSig) {
        VerificationScript multiSigVerifScript = wallet.getAccount(multiSig).getVerificationScript();
        int signers = 0;
        Account account;
        for (ECPublicKey pubKey : multiSigVerifScript.getPublicKeys()) {
            ScriptHash scriptHash = ScriptHash.fromPublicKey(pubKey.getEncoded(true));
            if (wallet.holdsAccount(scriptHash)) {
                account = wallet.getAccount(scriptHash);
                if (account != null && account.getECKeyPair() != null) {
                    signers += 1;
                }
            }
        }
        int signingThreshold = multiSigVerifScript.getSigningThreshold();
        return signers >= signingThreshold;
    }

    /**
     * Creates and sends a transfer transaction.
     * <p>
     * Uses only the wallet's default account to cover the token amount.
     *
     * @param wallet The wallet from which to send the tokens from.
     * @param to     The script hash of the receiver.
     * @param amount The amount to transfer as a decimal number (not token fractions).
     * @return The transaction id.
     * @throws IOException if there was a problem fetching information from the Neo node.
     */
    public NeoSendRawTransaction transferFromDefaultAccount(Wallet wallet, ScriptHash to, BigDecimal amount)
            throws IOException {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("The parameter amount must be greater than or equal to 0");
        }

        return buildTransferInvocation(wallet, to, amount).send();
    }

    // Method extracted for testability.
    Invocation buildTransferInvocation(Wallet wallet, ScriptHash to, BigDecimal amount)
            throws IOException {

        Account acc = wallet.getDefaultAccount();
        BigInteger fractions = getAmountAsBigInteger(amount);
        BigInteger accBalance = getBalanceOf(acc.getScriptHash());
        if (accBalance.compareTo(fractions) < 0) {
            throw new InsufficientFundsException("The wallet's default account does not hold enough"
                    + " tokens. Transfer amount is " + fractions.toString() + " but account"
                    + " only holds " + accBalance.toString() + " (in token fractions).");
        }
        return invoke(NEP5_TRANSFER)
                .withWallet(wallet)
                .withParameters(
                        ContractParameter.hash160(acc.getScriptHash()),
                        ContractParameter.hash160(to),
                        ContractParameter.integer(fractions)
                )
                .failOnFalse()
                .build()
                .sign();
    }

    private BigInteger getAmountAsBigInteger(BigDecimal amount) throws IOException {
        BigDecimal factor = BigDecimal.TEN.pow(getDecimals());
        return amount.multiply(factor).toBigInteger();
    }
}