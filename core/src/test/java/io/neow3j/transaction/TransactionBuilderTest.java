package io.neow3j.transaction;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.neow3j.constants.NeoConstants;
import io.neow3j.crypto.Base64;
import io.neow3j.crypto.ECKeyPair;
import io.neow3j.crypto.ECKeyPair.ECPublicKey;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.Neow3jConfig;
import io.neow3j.protocol.core.response.NeoApplicationLog;
import io.neow3j.protocol.core.response.NeoBlock;
import io.neow3j.protocol.core.response.NeoGetBlock;
import io.neow3j.protocol.core.response.NeoInvokeFunction;
import io.neow3j.protocol.core.response.NeoInvokeScript;
import io.neow3j.protocol.core.response.NeoSendRawTransaction;
import io.neow3j.protocol.http.HttpService;
import io.neow3j.script.ScriptBuilder;
import io.neow3j.test.TestProperties;
import io.neow3j.transaction.exceptions.TransactionConfigurationException;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.wallet.Account;
import io.reactivex.Observable;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.neow3j.test.WireMockTestHelper.setUpWireMockForBalanceOf;
import static io.neow3j.test.WireMockTestHelper.setUpWireMockForCall;
import static io.neow3j.test.WireMockTestHelper.setUpWireMockForGetBlockCount;
import static io.neow3j.transaction.AccountSigner.calledByEntry;
import static io.neow3j.transaction.AccountSigner.global;
import static io.neow3j.transaction.AccountSigner.none;
import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.types.ContractParameter.string;
import static io.neow3j.utils.Numeric.hexStringToByteArray;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static io.neow3j.wallet.Account.createMultiSigAccount;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransactionBuilderTest {

    private static final Hash160 NEO_TOKEN_SCRIPT_HASH = new Hash160(TestProperties.neoTokenHash());
    private static final Hash160 GAS_TOKEN_SCRIPT_HASH = new Hash160(TestProperties.gasTokenHash());
    private static final String NEP17_TRANSFER = "transfer";

    private static final String SCRIPT_INVOKEFUNCTION_NEO_SYMBOL = toHexStringNoPrefix(
            new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, "symbol", new ArrayList<>())
                    .toArray());
    private static final byte[] SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY =
            hexStringToByteArray(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL);

    private Account account1;
    private Account account2;
    private Hash160 recipient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private Neow3j neow;

    @Before
    public void setUp() {
        // Configuring WireMock to use default host and the dynamic port set in WireMockRule.
        int port = this.wireMockRule.port();
        WireMock.configureFor(port);
        neow = Neow3j.build(new HttpService("http://127.0.0.1:" + port),
                new Neow3jConfig().setNetworkMagic(769));
        account1 = new Account(ECKeyPair.create(hexStringToByteArray(
                "e6e919577dd7b8e97805151c05ae07ff4f752654d6d8797597aca989c02c4cb3")));
        account2 = new Account(ECKeyPair.create(hexStringToByteArray(
                "b4b2b579cac270125259f08a5f414e9235817e7637b9a66cfeb3b77d90c8e7f9")));
        recipient = new Hash160("969a77db482f74ce27105f760efa139223431394");
    }

    @Test
    public void buildTransactionWithCorrectNonce() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_necessary_mock.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        long nonce = ThreadLocalRandom.current().nextLong((long) Math.pow(2, 32));
        TransactionBuilder b = new TransactionBuilder(neow)
                .validUntilBlock(1L)
                .script(new byte[]{1, 2, 3})
                .signers(calledByEntry(account1));

        Transaction t = b.nonce(nonce).buildTransaction();
        assertThat(t.getNonce(), is(nonce));

        nonce = 0L;
        t = b.nonce(0L).buildTransaction();
        assertThat(t.getNonce(), is(nonce));

        nonce = (long) Math.pow(2, 32) - 1;
        t = b.nonce(nonce).buildTransaction();
        assertThat(t.getNonce(), is(nonce));

        nonce = Integer.toUnsignedLong(-1);
        t = b.nonce(nonce).buildTransaction();
        assertThat(t.getNonce(), is(nonce));
    }

    @Test
    public void failBuildingTransactionWithIncorrectNonce() {
        TransactionBuilder b = new TransactionBuilder(neow)
                .validUntilBlock(1L)
                .script(new byte[]{1, 2, 3})
                .signers(calledByEntry(account1.getScriptHash()));
        try {
            long nonce = Integer.toUnsignedLong(-1) + 1;
            b.nonce(nonce);
            fail();
        } catch (TransactionConfigurationException ignored) {
        }

        try {
            long nonce = (long) Math.pow(2, 32);
            b.nonce(nonce);
            fail();
        } catch (TransactionConfigurationException ignored) {
        }

        try {
            long nonce = -1L;
            b.nonce(nonce);
            fail();
        } catch (TransactionConfigurationException ignored) {
        }
    }

    @Test
    public void failBuildingTransactionWithNegativeValidUntilBlockNumber() {
        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("cannot be less than zero");
        new TransactionBuilder(neow)
                .validUntilBlock(-1L)
                .script(new byte[]{1, 2, 3})
                .signers(calledByEntry(account1));
    }

    @Test
    public void failBuildingTransactionWithTooHighValidUntilBlockNumber() {
        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("cannot be less than zero or more than 2^32");
        new TransactionBuilder(neow)
                .validUntilBlock((long) Math.pow(2, 32))
                .script(new byte[]{1, 2, 3})
                .signers(calledByEntry(account1));
    }

    @Test
    public void automaticallySetNonce() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_necessary_mock.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Transaction transaction = new TransactionBuilder(neow)
                .script(new byte[]{1, 2, 3})
                .signers(calledByEntry(account1))
                .buildTransaction();

        assertThat(transaction.getNonce(), greaterThanOrEqualTo(0L));
        assertThat(transaction.getNonce(), lessThanOrEqualTo((long) Math.pow(2, 32)));
    }

    @Test
    public void failBuildingTxWithoutAnySigner() throws Throwable {
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Cannot create a transaction without signers.");
        new TransactionBuilder(neow)
                .validUntilBlock(100L)
                .script(new byte[]{1, 2, 3})
                .buildTransaction();
    }

    @Test
    public void failAddingMultipleSignersConcerningTheSameAccount() {
        TransactionBuilder b = new TransactionBuilder(neow);
        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("concerning the same account");
        b.signers(global(account1),
                calledByEntry(account1));
    }

    @Test
    public void overrideSigner() {
        TransactionBuilder b = new TransactionBuilder(neow);
        b.signers(global(account1));
        assertThat(b.getSigners(), hasSize(1));
        assertThat(b.getSigners().get(0), is(global(account1)));

        b.signers(calledByEntry(account2));
        assertThat(b.getSigners(), hasSize(1));
        assertThat(b.getSigners().get(0), is(calledByEntry(account2)));
    }

    @Test
    public void attributes_highPriority() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        setUpWireMockForCall("getcommittee", "getcommittee.json");
        setUpWireMockForGetBlockCount(1000);

        HighPriorityAttribute attr = new HighPriorityAttribute();

        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .attributes(attr)
                .signers(none(account1))
                .buildTransaction();

        assertThat(tx.getAttributes(), hasSize(1));
        assertThat(tx.getAttributes().get(0).getType(),
                is(TransactionAttributeType.HIGH_PRIORITY));
    }

    @Test
    public void attributes_highPriority_multiSigContainingCommitteeMember() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        setUpWireMockForCall("getcommittee", "getcommittee.json");
        setUpWireMockForGetBlockCount(1000);

        Account multiSigAccount = createMultiSigAccount(
                asList(account2.getECKeyPair().getPublicKey(),
                        account1.getECKeyPair().getPublicKey()),
                1);
        HighPriorityAttribute attr = new HighPriorityAttribute();

        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .attributes(attr)
                .signers(none(multiSigAccount))
                .buildTransaction();

        assertThat(tx.getAttributes(), hasSize(1));
        assertThat(tx.getAttributes().get(0).getType(),
                is(TransactionAttributeType.HIGH_PRIORITY));
    }

    @Test
    public void attributes_highPriority_noCommitteeMember() throws Throwable {
        setUpWireMockForCall("getcommittee", "getcommittee.json");
        setUpWireMockForGetBlockCount(1000);

        HighPriorityAttribute attr = new HighPriorityAttribute();

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Only committee members can send transactions with high " +
                "priority.");

        new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .attributes(attr)
                .signers(none(account2))
                .buildTransaction();
    }

    @Test
    public void attributes_highPriority_onlyAddedOnce() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        setUpWireMockForCall("getcommittee", "getcommittee.json");
        setUpWireMockForGetBlockCount(1000);

        HighPriorityAttribute attr1 = new HighPriorityAttribute();
        HighPriorityAttribute attr2 = new HighPriorityAttribute();

        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(account1))
                .attributes(attr1)
                .attributes(attr2)
                .buildTransaction();

        assertThat(tx.getAttributes(), hasSize(1));
    }

    @Test
    public void attributes_failAddingMoreThanMaxToTxBuilder() {
        List<TransactionAttribute> attrs = new ArrayList<>();
        for (int i = 0; i <= NeoConstants.MAX_TRANSACTION_ATTRIBUTES; i++) {
            attrs.add(new HighPriorityAttribute());
        }
        TransactionAttribute[] attrArray = attrs.toArray(new TransactionAttribute[0]);

        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("A transaction cannot have more than " +
                NeoConstants.MAX_TRANSACTION_ATTRIBUTES + " attributes");

        new TransactionBuilder(neow).attributes(attrArray);
    }

    @Test
    public void attributes_failAddingMoreThanMaxToTxBuilder_attributes() {
        List<TransactionAttribute> attrs = new ArrayList<>();
        TransactionBuilder b = new TransactionBuilder(neow);
        b.signers(calledByEntry(Account.create()), calledByEntry(Account.create()),
                calledByEntry(Account.create()));

        for (int i = 0; i <= NeoConstants.MAX_TRANSACTION_ATTRIBUTES - 3; i++) {
            attrs.add(new HighPriorityAttribute());
        }
        TransactionAttribute[] attrArray = attrs.toArray(new TransactionAttribute[0]);

        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("A transaction cannot have more than " +
                NeoConstants.MAX_TRANSACTION_ATTRIBUTES + " attributes");

        b.attributes(attrArray);
    }

    @Test
    public void attributes_failAddingMoreThanMaxToTxBuilder_signers() {
        TransactionBuilder b = new TransactionBuilder(neow);
        b.attributes(new HighPriorityAttribute());

        List<Signer> signers = new ArrayList<>();
        for (int i = 0; i < NeoConstants.MAX_TRANSACTION_ATTRIBUTES; i++) {
            signers.add(calledByEntry(Account.create()));
        }
        Signer[] signerArr = signers.toArray(new Signer[0]);

        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("A transaction cannot have more than " +
                NeoConstants.MAX_TRANSACTION_ATTRIBUTES + " attributes");

        assertThat(signerArr.length + 1, greaterThan(NeoConstants.MAX_TRANSACTION_ATTRIBUTES));
        b.signers(signerArr);
    }

    @Test
    public void testAutomaticSettingOfValidUntilBlockVariable() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        setUpWireMockForGetBlockCount(1000);

        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(Account.create()))
                .buildTransaction();

        assertThat(tx.getValidUntilBlock(), is(neow.getMaxValidUntilBlockIncrement() + 1000 - 1));
    }

    @Test
    public void testAutomaticSettingOfSystemFeeAndNetworkFee() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(Account.create()))
                .validUntilBlock(1000)
                .buildTransaction();

        assertThat(tx.getSystemFee(), is(984060L));
        assertThat(tx.getNetworkFee(), is(1230610L));
    }

    @Test
    public void failTryingToSignTransactionWithAccountMissingAPrivateKey() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        TransactionBuilder builder = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(Account.fromAddress(account1.getAddress())))
                .validUntilBlock(1000);

        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("Cannot create transaction signature because account "
                 + account1.getAddress() + " does not hold a private key.");
        builder.sign();
    }

    @Test
    public void failAutomaticallySigningWithMultiSigAccountSigner() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Transactions with multi-sig signers cannot be " +
                "signed automatically.");
        new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(
                        createMultiSigAccount(asList(account1.getECKeyPair().getPublicKey()), 1)))
                .sign();
    }

    @Test
    public void failWithNoSigningAccount() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");

        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("transaction requires at least one signing account");
        new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(ContractSigner.calledByEntry(Account.create().getScriptHash()))
                .sign();
    }

    @Test
    public void failSigningWithAccountWithoutECKeyPair() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Account accountWithoutKeyPair =
                Account.fromVerificationScript(account1.getVerificationScript());

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("does not hold a private key");
        new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(accountWithoutKeyPair))
                .sign();
    }

    @Test
    public void signTransactionWithAdditionalSigners() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(calledByEntry(account1), calledByEntry(account2))
                .validUntilBlock(1000)
                .sign();

        List<Witness> witnesses = tx.getWitnesses();
        assertThat(witnesses, hasSize(2));
        List<ECPublicKey> signers = witnesses.stream()
                .map(wit -> wit.getVerificationScript().getPublicKeys().get(0))
                .collect(Collectors.toList());
        assertThat(signers, containsInAnyOrder(
                account1.getECKeyPair().getPublicKey(),
                account2.getECKeyPair().getPublicKey()));
    }

    @Test
    public void failSendingTransactionBecauseItDoesntContainTheRightNumberOfWitnesses()
            throws Throwable {

        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(calledByEntry(Account.create()))
                .validUntilBlock(1000) // Setting explicitly so that no RPC call is necessary.
                .buildTransaction();
        // Don't add any witnesses, so it has one signer but no witness.
        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage(new StringContains("The transaction does not have the same " +
                "number of signers and witnesses."));
        tx.send();
    }

    @Test
    public void testContractWitness() throws Throwable {
        Hash160 contractHash = new Hash160("e87819d005b730645050f89073a4cd7bf5f6bd3c");
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        TransactionBuilder b = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(
                        ContractSigner.global(contractHash, string("iamgroot"), integer(2)),
                        AccountSigner.calledByEntry(Account.create()))
                .validUntilBlock(1000); // Setting explicitly so that no RPC call is necessary.
        Transaction tx = b.sign();

        byte[] invocScript = new ScriptBuilder().pushData("iamgroot").pushInteger(2).toArray();
        assertThat(tx.getWitnesses(), hasItem(new Witness(invocScript, new byte[]{})));
    }

    @Test
    public void sendInvokeFunction() throws Throwable {
        setUpWireMockForCall("invokescript",
                "invokescript_transfer_with_fixed_sysfee.json");
        setUpWireMockForCall("sendrawtransaction", "sendrawtransaction.json");
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(recipient),
                        integer(5),
                        any(null))).toArray();

        Transaction tx = new TransactionBuilder(neow)
                .script(script)
                .signers(none(account1))
                .sign();

        NeoSendRawTransaction response = tx.send();

        assertThat(response.getError(), nullValue());
        // This is not the actual transaction id of the above built transaction but merely the
        // one used in the file `responses/sendrawtransaction.json`.
        assertThat(response.getSendRawTransaction().getHash(), is(
                new Hash256("0x830816f0c801bcabf919dfa1a90d7b9a4f867482cb4d18d0631a5aa6daefab6a")));
    }

    @Test
    public void transferNeoFromNormalAccount() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_transfer_with_fixed_sysfee.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        byte[] expectedScript = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH,
                        NEP17_TRANSFER, asList(
                                hash160(account1.getScriptHash()),
                                hash160(recipient),
                                integer(5),
                                any(null)))
                .toArray();
        byte[] expectedVerificationScript = account1.getVerificationScript().getScript();

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(recipient),
                        integer(5),
                        any(null))).toArray();

        Transaction tx = new TransactionBuilder(neow)
                .script(script)
                .signers(none(account1))
                .validUntilBlock(100)
                .sign();

        assertThat(tx.getScript(), is(expectedScript));
        List<Witness> witnesses = tx.getWitnesses();
        assertThat(witnesses, hasSize(1));
        assertThat(witnesses.get(0).getVerificationScript().getScript(),
                is(expectedVerificationScript));
    }

    // This tests if the `invokeFunction()` method produces the right request.
    @Test
    public void invokingWithParamsShouldProduceTheCorrectRequest() throws IOException {
        setUpWireMockForCall("invokefunction",
                "invokefunction_transfer_neo.json",
                NEO_TOKEN_SCRIPT_HASH.toString(), NEP17_TRANSFER,
                account1.getScriptHash().toString(), recipient.toString(), "5"); // the params

        NeoInvokeFunction i = neow.invokeFunction(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER, asList(
                        hash160(account1.getScriptHash()), hash160(recipient), integer(5),
                        any(null)))
                .send();

        // The script that's in the `invokefunction_transfer_neo.json` response file.
        String scriptInResponse =
                "CxUMFJQTQyOSE/oOdl8QJ850L0jbd5qWDBQGSl3MDxYsg0c9Aok46V" +
                        "+3dhMechTAHwwIdHJhbnNmZXIMFIOrBnmtVcBQoTrUP1k26nP16x72QWJ9W1I=";
        assertThat(i.getResult().getScript(), is(scriptInResponse));
    }

    @Test
    public void doIfSenderCannotCoverFees() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_transfer_with_fixed_sysfee.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        setUpWireMockForBalanceOf(account1.getScriptHash().toString(),
                "invokefunction_balanceOf_1000000.json");

        long netFee = 1230610;
        // The system fee found in the `invokescript_transfer_with_fixed_sysfee.json` file.
        long sysFee = 9999510;
        BigInteger expectedFees = BigInteger.valueOf(netFee + sysFee);
        BigInteger expectedBalance = BigInteger.valueOf(1_000_000L);
        AtomicBoolean tested = new AtomicBoolean(false);

        byte[] script = new ScriptBuilder().contractCall(GAS_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(recipient),
                        integer(2_000_000),
                        any(null))).toArray();

        new TransactionBuilder(neow)
                .script(script)
                .signers(calledByEntry(account1))
                .validUntilBlock(2000000)
                .doIfSenderCannotCoverFees((fee, balance) -> {
                    assertThat(fee, is(expectedFees));
                    assertThat(balance, is(expectedBalance));
                    tested.set(true);
                })
                .buildTransaction();

        assertTrue(tested.get()); // Assert that the test actually called the lambda function.
    }

    @Test
    public void doIfSenderCannotCoverFees_alreadySpecifiedASupplier() {
        TransactionBuilder b = new TransactionBuilder(neow)
                .throwIfSenderCannotCoverFees(IllegalStateException::new);
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Cannot handle a consumer for this case, since an exception");
        b.doIfSenderCannotCoverFees((fee, balance) -> System.out.println(fee));
    }

    @Test
    public void throwIfSenderCannotCoverFees() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_transfer_with_fixed_sysfee.json");
        setUpWireMockForCall("invokefunction",
                "invokefunction_balanceOf_1000000.json",
                GAS_TOKEN_SCRIPT_HASH.toString(),
                "balanceOf",
                account1.getScriptHash().toString());
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(recipient),
                        integer(5),
                        any(null))).toArray();

        TransactionBuilder b = new TransactionBuilder(neow)
                .script(script)
                .validUntilBlock(2000000)
                .signers(calledByEntry(account1))
                .throwIfSenderCannotCoverFees(
                        () -> new IllegalStateException("test throwIfSenderCannotCoverFees"));

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("test throwIfSenderCannotCoverFees");
        b.buildTransaction();
    }

    @Test
    public void throwIfSenderCannotCoverFees_alreadySpecifiedAConsumer() {
        TransactionBuilder b = new TransactionBuilder(neow)
                .doIfSenderCannotCoverFees((fee, balance) -> System.out.println(fee));
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Cannot handle a supplier for this case, since a consumer");
        b.throwIfSenderCannotCoverFees(IllegalStateException::new);
    }

    @Test
    public void invokeScript() throws IOException {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json",
                Base64.encode(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL),
                "[\"721e1376b75fe93889023d47832c160fcc5d4a06\"]"); // witness (sender script hash)
        String privateKey = "e6e919577dd7b8e97805151c05ae07ff4f752654d6d8797597aca989c02c4cb3";
        ECKeyPair senderPair = ECKeyPair.create(hexStringToByteArray(privateKey));

        NeoInvokeScript response = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .callInvokeScript();
        assertThat(response.getInvocationResult().getStack().get(0).getString(), is("NEO"));
    }

    @Test
    public void invokeScriptWithoutSettingScript() throws IOException {
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json",
                SCRIPT_INVOKEFUNCTION_NEO_SYMBOL,
                "[\"721e1376b75fe93889023d47832c160fcc5d4a06\"]"); // witness (sender script hash)
        String privateKey = "e6e919577dd7b8e97805151c05ae07ff4f752654d6d8797597aca989c02c4cb3";
        ECKeyPair senderPair = ECKeyPair.create(hexStringToByteArray(privateKey));
        Account sender = new Account(senderPair);

        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("Cannot make an 'invokescript' call");
        new TransactionBuilder(neow).callInvokeScript();
    }

    @Test
    public void buildWithoutSettingScript() throws Throwable {
        TransactionBuilder b = new TransactionBuilder(neow);

        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("script");
        b.buildTransaction();
    }

    @Test
    public void buildWithInvalidScript() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_invalidscript.json",
                "DAASDBSTrRVy");
        TransactionBuilder b = new TransactionBuilder(neow)
                .script(hexStringToByteArray("0c00120c1493ad1572"))
                .signers(calledByEntry(account1));
        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("Instruction out of bounds.");
        b.buildTransaction();
    }

    @Test
    public void buildWithScript_vmFaults() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript",
                "invokescript_exception.json",
                "DA5PcmFjbGVDb250cmFjdEEa93tn");
        TransactionBuilder b = new TransactionBuilder(neow)
                .script(hexStringToByteArray("0c0e4f7261636c65436f6e7472616374411af77b67"))
                .signers(calledByEntry(account1));
        exceptionRule.expect(TransactionConfigurationException.class);
        exceptionRule.expectMessage("The vm exited due to the following exception: Value was " +
                "either too large or too small for an Int32.");
        b.buildTransaction();
    }

    @Test
    public void testGetUnsignedTransaction() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(calledByEntry(account1))
                .getUnsignedTransaction();

        assertThat(tx.getVersion(), is((byte) 0));
        assertThat(tx.getSigners(), hasSize(1));
        assertThat(tx.getSigners().get(0), is(calledByEntry(account1)));
        assertThat(tx.getWitnesses(), hasSize(0));
    }

    @Test
    public void testVersion() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Transaction tx = new TransactionBuilder(neow)
                .version((byte) 1)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(calledByEntry(account1))
                .getUnsignedTransaction();

        assertThat(tx.getVersion(), is((byte) 1));
    }

    @Test
    public void testAdditionalNetworkFee() throws Throwable {
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_symbol_neo.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Account account = Account.create();
        Transaction tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(Account.create()))
                .buildTransaction();

        long baseNetworkFee = 1230610L;
        assertThat(tx.getNetworkFee(), is(baseNetworkFee));

        tx = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(none(account))
                .additionalNetworkFee(2000L)
                .buildTransaction();

        assertThat(tx.getNetworkFee(), is(baseNetworkFee + 2000L));
    }

    @Test
    public void testSetFirstSigner() {
        Signer s1 = global(account1);
        Signer s2 = calledByEntry(account2);
        TransactionBuilder b = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(s1, s2);
        assertThat(b.getSigners().get(0), is(s1));
        assertThat(b.getSigners().get(1), is(s2));

        b.firstSigner(s2.getScriptHash());
        assertThat(b.getSigners().get(0), is(s2));
        assertThat(b.getSigners().get(1), is(s1));
    }

    @Test
    public void testSetFirstSigner_account() {
        Signer s1 = global(account1);
        Signer s2 = calledByEntry(account2);
        TransactionBuilder b = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(s1, s2);
        assertThat(b.getSigners().get(0), is(s1));
        assertThat(b.getSigners().get(1), is(s2));

        b.firstSigner(account2);
        assertThat(b.getSigners().get(0), is(s2));
        assertThat(b.getSigners().get(1), is(s1));
    }

    @Test
    public void testSetFirstSigner_feeOnlyPresent() {
        Signer s1 = AccountSigner.none(account1);
        Signer s2 = calledByEntry(account2);
        TransactionBuilder b = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(s1, s2);
        assertThat(b.getSigners().get(0), is(s1));
        assertThat(b.getSigners().get(1), is(s2));

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("contains a signer with fee-only witness scope");
        b.firstSigner(s2.getScriptHash());
    }

    @Test
    public void testSetFirstSigner_notPresent() {
        Signer s1 = global(account1);
        TransactionBuilder b = new TransactionBuilder(neow)
                .script(SCRIPT_INVOKEFUNCTION_NEO_SYMBOL_BYTEARRAY)
                .signers(s1);
        assertThat(b.getSigners().get(0), is(s1));

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Could not find a signer with script hash");
        b.firstSigner(account2.getScriptHash());
    }

    @Test
    public void trackingTransactionShouldReturnCorrectBlock() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_transfer_with_fixed_sysfee.json");
        setUpWireMockForCall("sendrawtransaction", "sendrawtransaction.json");
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        Neow3j neowSpy = Mockito.spy(neow);
        Hash256 txHash =
                new Hash256("1bf80f98084ede43fba9e347b0af546e2e7da9038e019baf0258f09b59f019f0");
        neowSpy = Mockito.when(neowSpy.catchUpToLatestAndSubscribeToNewBlocksObservable(
                        new BigInteger("1000"), true))
                .thenReturn(Observable.fromArray(createBlock(1000), createBlock(1001),
                        createBlock(1002, createTx(txHash)))).getMock();

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(recipient),
                        integer(5),
                        any(null))).toArray();

        Transaction tx = new TransactionBuilder(neowSpy)
                .script(script)
                .nonce(0L)
                .signers(calledByEntry(account1))
                .sign();

        tx.send();
        CountDownLatch completedLatch = new CountDownLatch(1);
        AtomicLong receivedBlockNr = new AtomicLong();
        tx.track().subscribe(
                receivedBlockNr::set,
                throwable -> fail(throwable.getMessage()),
                completedLatch::countDown);

        completedLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(receivedBlockNr.get(), is(1002L));
    }

    @Test
    public void trackingTransaction_txNotSent() throws Throwable {
        setUpWireMockForCall("invokescript", "invokescript_transfer_with_fixed_sysfee.json");
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(recipient),
                        integer(5),
                        any(null))).toArray();

        Transaction tx = new TransactionBuilder(neow)
                .script(script)
                .nonce(0L)
                .signers(none(account1))
                .sign();

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("subscribe before transaction has been sent");
        tx.track();
    }

    private NeoGetBlock createBlock(int number) {
        NeoGetBlock neoGetBlock = new NeoGetBlock();
        NeoBlock block = new NeoBlock(null, 0L, 0, null, null, 123456789, number, 0, "nonce", null,
                new ArrayList<>(), 1, null);
        neoGetBlock.setResult(block);
        return neoGetBlock;
    }

    private NeoGetBlock createBlock(int number,
            io.neow3j.protocol.core.response.Transaction tx) {

        NeoGetBlock neoGetBlock = new NeoGetBlock();
        NeoBlock block = new NeoBlock(null, 0L, 0, null, null, 123456789, number, 0, "nonce", null,
                singletonList(tx), 1, null);
        neoGetBlock.setResult(block);
        return neoGetBlock;
    }

    private io.neow3j.protocol.core.response.Transaction createTx(Hash256 txHash) {
        return new io.neow3j.protocol.core.response.Transaction(txHash, 0, 0, 0L, "", "",
                "", 0L, null, null, null, null);
    }

    @Test
    public void getApplicationLog() throws Throwable {
        setUpWireMockForBalanceOf(account1.getScriptHash().toString(),
                "invokefunction_balanceOf_1000000.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_transfer.json");
        setUpWireMockForCall("sendrawtransaction", "sendrawtransaction.json");
        setUpWireMockForCall("getapplicationlog", "getapplicationlog.json");

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(account1),
                        integer(1),
                        any(null))).toArray();

        Transaction tx = new TransactionBuilder(neow)
                .script(script)
                .signers(calledByEntry(account1))
                .sign();

        tx.send();
        NeoApplicationLog applicationLog = tx.getApplicationLog();
        assertThat(applicationLog.getTransactionId(),
                is(new Hash256(
                        "0xeb52f99ae5cf923d8905bdd91c4160e2207d20c0cb42f8062f31c6743770e4d1")));
    }

    @Test
    public void getApplicationLog_txNotSent() throws Throwable {
        setUpWireMockForBalanceOf(account1.getScriptHash().toString(),
                "invokefunction_balanceOf_1000000.json");
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_transfer.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(account1),
                        integer(1),
                        any(null))).toArray();

        Transaction tx = new TransactionBuilder(neow)
                .script(script)
                .signers(calledByEntry(account1))
                .sign();

        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("application log before transaction has been sent");
        tx.getApplicationLog();
    }

    @Test
    public void getApplicationLog_notExisting() throws Throwable {
        setUpWireMockForBalanceOf(account1.getScriptHash().toString(),
                "invokefunction_balanceOf_1000000.json");
        setUpWireMockForCall("getblockcount", "getblockcount_1000.json");
        setUpWireMockForCall("invokescript", "invokescript_transfer.json");
        setUpWireMockForCall("calculatenetworkfee", "calculatenetworkfee.json");
        setUpWireMockForCall("sendrawtransaction", "sendrawtransaction.json");
        setUpWireMockForCall("getapplicationlog", "getapplicationlog_unknowntx" +
                ".json");

        byte[] script = new ScriptBuilder().contractCall(NEO_TOKEN_SCRIPT_HASH, NEP17_TRANSFER,
                asList(hash160(account1.getScriptHash()),
                        hash160(account1),
                        integer(1),
                        any(null))).toArray();

        Transaction tx = new TransactionBuilder(neow)
                .script(script)
                .signers(calledByEntry(account1))
                .sign();
        tx.send();

        assertNull(tx.getApplicationLog());
    }

}
