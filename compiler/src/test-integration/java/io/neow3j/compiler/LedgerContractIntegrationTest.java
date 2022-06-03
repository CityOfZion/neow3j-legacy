package io.neow3j.compiler;

import io.neow3j.contract.NeoToken;
import io.neow3j.devpack.Block;
import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.ECPoint;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Hash256;
import io.neow3j.devpack.Signer;
import io.neow3j.devpack.Transaction;
import io.neow3j.devpack.contracts.LedgerContract;
import io.neow3j.protocol.Neow3jConfig;
import io.neow3j.protocol.core.response.NeoBlock;
import io.neow3j.protocol.core.response.NeoInvokeFunction;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.transaction.WitnessScope;
import io.neow3j.transaction.witnessrule.BooleanCondition;
import io.neow3j.transaction.witnessrule.CalledByEntryCondition;
import io.neow3j.transaction.witnessrule.NotCondition;
import io.neow3j.transaction.witnessrule.OrCondition;
import io.neow3j.transaction.witnessrule.ScriptHashCondition;
import io.neow3j.transaction.witnessrule.WitnessAction;
import io.neow3j.transaction.witnessrule.WitnessConditionType;
import io.neow3j.transaction.witnessrule.WitnessRule;
import io.neow3j.types.NeoVMStateType;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static io.neow3j.test.TestProperties.ledgerContractHash;
import static io.neow3j.test.TestProperties.neoTokenHash;
import static io.neow3j.types.ContractParameter.hash256;
import static io.neow3j.types.ContractParameter.integer;
import static io.neow3j.utils.Numeric.reverseHexString;
import static io.neow3j.utils.Numeric.toHexStringNoPrefix;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LedgerContractIntegrationTest {

    private static NeoBlock blockOfDeployTx;
    private static io.neow3j.types.Hash256 preparedTx;

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static ContractTestRule ct = new ContractTestRule(LedgerContractIntegrationTestContract.class.getName());

    @BeforeClass
    public static void setUp() throws Throwable {
        blockOfDeployTx = ct.getNeow3j().getBlock(ct.getBlockHashOfDeployTx(), true).send().getBlock();
        preparedTx = prepareTxWithWitnessRuleSigner();
    }

    private static io.neow3j.types.Hash256 prepareTxWithWitnessRuleSigner() throws Throwable {
        WitnessRule rule1 = new WitnessRule(WitnessAction.DENY,
                new OrCondition(
                        new ScriptHashCondition(NeoToken.SCRIPT_HASH),
                        new CalledByEntryCondition()
                ));
        WitnessRule rule2 = new WitnessRule(WitnessAction.ALLOW, new NotCondition(new BooleanCondition(false)));
        io.neow3j.transaction.Signer signer = AccountSigner.none(ct.getClient1()).setRules(rule1, rule2);
        return ct.invokeFunctionAndAwaitExecution("setup", asList(), signer);
    }

    @Test
    public void getTransactionHeight() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, hash256(ct.getDeployTxHash()));
        assertThat(response.getInvocationResult().getStack().get(0).getInteger().longValue(),
                is(blockOfDeployTx.getIndex()));
    }

    @Test
    public void getTransactionHeightOfNonExistentTransaction() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction("getTransactionHeight",
                hash256("0000000000000000000000000000000000000000000000000000000000000000"));
        assertThat(response.getInvocationResult().getStack().get(0).getInteger().intValue(), is(-1));
    }

    @Test
    public void getTransactionFromBlock() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName,
                integer(BigInteger.valueOf(blockOfDeployTx.getIndex())), integer(0));
        List<StackItem> tx = response.getInvocationResult().getStack().get(0).getList();
        assertThat(tx.get(0).getHexString(), is(reverseHexString(ct.getDeployTxHash().toString())));
        assertThat(tx.get(1).getInteger().intValue(), is(0)); // version
        assertThat(tx.get(2).getInteger().longValue(), greaterThanOrEqualTo(1L)); // nonce
        assertThat(tx.get(3).getAddress(), is(ct.getCommittee().getAddress())); // sender
        assertThat(tx.get(4).getInteger().intValue(), greaterThanOrEqualTo(1)); // system fee
        assertThat(tx.get(5).getInteger().intValue(), greaterThanOrEqualTo(1)); // network fee
        assertThat(tx.get(6).getInteger().longValue(),
                greaterThanOrEqualTo(new Neow3jConfig().getMaxValidUntilBlockIncrement()));
        assertThat(tx.get(7).getHexString().length(), greaterThanOrEqualTo(1)); // script
    }

    @Test
    public void getTransactionFromBlockWithBlockHash() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, hash256(ct.getBlockHashOfDeployTx()), integer(0));
        List<StackItem> tx = response.getInvocationResult().getStack().get(0).getList();
        assertThat(tx.get(0).getHexString(), is(reverseHexString(ct.getDeployTxHash().toString())));
        assertThat(tx.get(1).getInteger().intValue(), is(0)); // version
        assertThat(tx.get(2).getInteger().longValue(), greaterThanOrEqualTo(1L)); // nonce
        assertThat(tx.get(3).getAddress(), is(ct.getCommittee().getAddress())); // sender
        assertThat(tx.get(4).getInteger().intValue(), greaterThanOrEqualTo(1)); // system fee
        assertThat(tx.get(5).getInteger().intValue(), greaterThanOrEqualTo(1)); // network fee
        assertThat(tx.get(6).getInteger().longValue(),
                greaterThanOrEqualTo(new Neow3jConfig().getMaxValidUntilBlockIncrement()));
        assertThat(tx.get(7).getHexString().length(), greaterThanOrEqualTo(1)); // script
    }

    @Test
    public void getTransactionSigners() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, hash256(preparedTx));
        List<StackItem> stack = response.getInvocationResult().getStack();
        assertThat(stack, hasSize(1));

        List<StackItem> signers = stack.get(0).getList();
        assertThat(signers, hasSize(2));

        List<StackItem> signer1 = signers.get(0).getList();
        assertThat(signer1, hasSize(6));
        List<StackItem> signer2 = signers.get(1).getList();
        assertThat(signer2, hasSize(6));

        String expectedSerialized = reverseHexString(ct.getClient1().getScriptHash().toString()) //  account
                + toHexStringNoPrefix(WitnessScope.WITNESS_RULES.byteValue()) // scope
                + "" // would be allowedContracts
                + "" // would be allowedGroups
                + toHexStringNoPrefix((byte) 2) // numer of rules
                // Rule 1
                + toHexStringNoPrefix(WitnessAction.DENY.byteValue()) // action
                + toHexStringNoPrefix(WitnessConditionType.OR.byteValue()) // condition
                + toHexStringNoPrefix((byte) 2) // size of expressions in or condition
                + toHexStringNoPrefix(WitnessConditionType.SCRIPT_HASH.byteValue()) // scripthash type
                + reverseHexString(neoTokenHash()) // script hash value
                + toHexStringNoPrefix(WitnessConditionType.CALLED_BY_ENTRY.byteValue()) // calledbyentry type
                // Rule 2
                + toHexStringNoPrefix(WitnessAction.ALLOW.byteValue()) // action
                + toHexStringNoPrefix(WitnessConditionType.NOT.byteValue()) // condition
                + toHexStringNoPrefix(WitnessConditionType.BOOLEAN.byteValue()) // boolean type
                + toHexStringNoPrefix((byte) 0); // false

        assertThat(signer2.get(0).getHexString(), is(expectedSerialized));

        assertThat(signer2.get(1).getAddress(), is(ct.getClient1().getAddress()));
        assertThat(signer2.get(2).getInteger().byteValue(), is(WitnessScope.WITNESS_RULES.byteValue()));
        assertThat(signer2.get(3).getList(), hasSize(0));
        assertThat(signer2.get(4).getList(), hasSize(0));
        List<StackItem> rules = signer2.get(5).getList();
        assertThat(rules, hasSize(2));

        List<StackItem> rule1 = rules.get(0).getList();
        assertThat(rule1, hasSize(2));
        assertThat(rule1.get(0).getInteger().byteValue(), is(WitnessAction.DENY.byteValue()));

        List<StackItem> rule1Condition = rule1.get(1).getList();
        assertThat(rule1Condition, hasSize(2));
        byte rule1ConditionType = rule1Condition.get(0).getInteger().byteValue();
        assertThat(rule1ConditionType, is(WitnessConditionType.OR.byteValue()));
        List<StackItem> rule1ConditionValue = rule1Condition.get(1).getList();
        assertThat(rule1ConditionValue, hasSize(2));
        List<StackItem> orConditionExpression1 = rule1ConditionValue.get(0).getList();
        assertThat(orConditionExpression1, hasSize(2));
        assertThat(orConditionExpression1.get(0).getInteger().byteValue(),
                is(WitnessConditionType.SCRIPT_HASH.byteValue()));
        assertThat(orConditionExpression1.get(1).getAddress(), is(NeoToken.SCRIPT_HASH.toAddress()));

        List<StackItem> orConditionExpression2 = rule1ConditionValue.get(1).getList();
        assertThat(orConditionExpression2, hasSize(1));
        assertThat(orConditionExpression2.get(0).getInteger().byteValue(),
                is(WitnessConditionType.CALLED_BY_ENTRY.byteValue()));

        List<StackItem> rule2 = rules.get(1).getList();
        assertThat(rule2, hasSize(2));
        assertThat(rule2.get(0).getInteger().byteValue(), is(WitnessAction.ALLOW.byteValue()));

        List<StackItem> rule2Condition = rule2.get(1).getList();
        assertThat(rule2Condition, hasSize(2));
        byte rule2ConditionType = rule2Condition.get(0).getInteger().byteValue();
        assertThat(rule2ConditionType, is(WitnessConditionType.NOT.byteValue()));
        List<StackItem> rule2ConditionValue = rule2Condition.get(1).getList();
        assertThat(rule2ConditionValue, hasSize(2));
        assertThat(rule2ConditionValue.get(0).getInteger().byteValue(), is(WitnessConditionType.BOOLEAN.byteValue()));
        assertFalse(rule2ConditionValue.get(1).getBoolean());
    }

    @Test
    public void getTransactionSignerValues() throws Throwable {
        NeoInvokeFunction response = ct.callInvokeFunction("getTransactionSigners", hash256(preparedTx));
        List<StackItem> stack = response.getInvocationResult().getStack();
        assertThat(stack, hasSize(1));
        List<StackItem> signerList = stack.get(0).getList();
        assertThat(signerList, hasSize(2));
        assertThat(signerList.get(1).getList().get(2).getInteger().byteValue(),
                is(WitnessScope.WITNESS_RULES.byteValue()));
        assertThat(signerList.get(1).getList().get(5).getList(), hasSize(2)); // 2 rules

        response = ct.callInvokeFunction("getTransactionSigner", hash256(preparedTx), integer(0));
        stack = response.getInvocationResult().getStack();
        List<StackItem> signer = stack.get(0).getList();
        assertThat(signer, hasSize(6));
        assertThat(signer.get(1).getAddress(), is(ct.getDefaultAccount().getAddress()));
        assertThat(signer.get(2).getInteger().byteValue(), is(WitnessScope.GLOBAL.byteValue()));

        response = ct.callInvokeFunction("getTransactionSigner", hash256(preparedTx), integer(1));
        stack = response.getInvocationResult().getStack();
        signer = stack.get(0).getList();
        assertThat(signer, hasSize(6));
        assertThat(signer.get(1).getAddress(), is(ct.getClient1().getAddress()));
        assertThat(signer.get(2).getInteger().byteValue(), is(WitnessScope.WITNESS_RULES.byteValue()));
        assertThat(signer.get(5).getList(), hasSize(2));

        response = ct.callInvokeFunction("getTransactionSignerSerialized", hash256(preparedTx), integer(1));
        stack = response.getInvocationResult().getStack();
        assertNotNull(stack.get(0).getString());

        response = ct.callInvokeFunction("getTransactionSignerAccount", hash256(preparedTx), integer(1));
        stack = response.getInvocationResult().getStack();
        assertThat(stack.get(0).getAddress(), is(ct.getClient1().getAddress()));

        response = ct.callInvokeFunction("getTransactionSignerWitnessScope", hash256(preparedTx), integer(1));
        stack = response.getInvocationResult().getStack();
        assertThat(stack.get(0).getInteger().byteValue(), is(WitnessScope.WITNESS_RULES.byteValue()));

        response = ct.callInvokeFunction("getTransactionSignerAllowedContracts", hash256(preparedTx), integer(1));
        stack = response.getInvocationResult().getStack();
        assertThat(stack.get(0).getList(), hasSize(0));

        response = ct.callInvokeFunction("getTransactionSignerAllowedGroups", hash256(preparedTx), integer(1));
        stack = response.getInvocationResult().getStack();
        assertThat(stack.get(0).getList(), hasSize(0));

        response = ct.callInvokeFunction("getTransactionSignerWitnessRules", hash256(preparedTx), integer(1));
        stack = response.getInvocationResult().getStack();
        List<StackItem> rules = stack.get(0).getList();
        assertThat(rules, hasSize(2));
        assertThat(rules.get(0).getList().get(0).getInteger(), is(BigInteger.ZERO)); // Deny
        assertThat(rules.get(0).getList().get(1).getList().get(0).getInteger().byteValue(),
                is(WitnessConditionType.OR.byteValue()));
        assertThat(rules.get(0).getList().get(1).getList().get(1).getList(), hasSize(2)); // 2 expressions
        assertThat(rules.get(1).getList().get(0).getInteger(), is(BigInteger.ONE)); // Allow
        assertThat(rules.get(1).getList().get(1).getList().get(0).getInteger().byteValue(),
                is(WitnessConditionType.NOT.byteValue()));
        assertThat(rules.get(1).getList().get(1).getList().get(1).getList().get(0).getInteger().byteValue(),
                is(WitnessConditionType.BOOLEAN.byteValue()));
        assertFalse(rules.get(1).getList().get(1).getList().get(1).getList().get(1).getBoolean());
    }

    @Test
    public void getTransaction() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, hash256(ct.getDeployTxHash()));
        List<StackItem> tx = response.getInvocationResult().getStack().get(0).getList();
        assertThat(tx.get(0).getHexString(), is(reverseHexString(ct.getDeployTxHash().toString())));
        assertThat(tx.get(1).getInteger().intValue(), is(0)); // version
        assertThat(tx.get(2).getInteger().longValue(), greaterThanOrEqualTo(1L)); // nonce
        assertThat(tx.get(3).getAddress(), is(ct.getCommittee().getAddress())); // sender
        assertThat(tx.get(4).getInteger().intValue(), greaterThanOrEqualTo(1)); // system fee
        assertThat(tx.get(5).getInteger().intValue(), greaterThanOrEqualTo(1)); // network fee
        assertThat(tx.get(6).getInteger().longValue(),
                greaterThanOrEqualTo(new Neow3jConfig().getMaxValidUntilBlockIncrement()));
        assertThat(tx.get(7).getHexString().length(), greaterThanOrEqualTo(1)); // script
    }

    @Test
    public void getNonExistentTransaction() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName,
                hash256("0000000000000000000000000000000000000000000000000000000000000000"));
        assertThat(response.getInvocationResult().getStack().get(0).getInteger().intValue(), is(1));
    }

    @Test
    public void getTransactionVMState() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, hash256(ct.getDeployTxHash()));
        assertThat(response.getInvocationResult().getStack().get(0).getInteger().intValue(),
                is(NeoVMStateType.HALT.intValue()));
    }

    @Test
    public void getBlockWithBlockHash() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, hash256(ct.getBlockHashOfDeployTx()));

        List<StackItem> block = response.getInvocationResult().getStack().get(0).getList();
        assertThat(block.get(0).getHexString(), is(reverseHexString(ct.getBlockHashOfDeployTx().toString())));
        assertThat(block.get(1).getInteger().intValue(), is(blockOfDeployTx.getVersion()));
        assertThat(block.get(2).getHexString(), is(reverseHexString(blockOfDeployTx.getPrevBlockHash().toString())));
        assertThat(block.get(3).getHexString(), is(reverseHexString(blockOfDeployTx.getMerkleRootHash().toString())));
        assertThat(block.get(4).getInteger().longValue(), is(blockOfDeployTx.getTime()));
        assertThat(block.get(5).getInteger(), is(greaterThanOrEqualTo(BigInteger.ZERO)));
        assertThat(block.get(6).getInteger().longValue(), is(blockOfDeployTx.getIndex()));
        assertThat(block.get(7).getInteger().intValue(), is(blockOfDeployTx.getPrimary()));
        assertThat(block.get(8).getAddress(), is(blockOfDeployTx.getNextConsensus()));
        assertThat(block.get(9).getInteger().intValue(), is(blockOfDeployTx.getTransactions().size()));
    }

    @Test
    public void getBlockWithBlockNumber() throws IOException {
        NeoInvokeFunction response =
                ct.callInvokeFunction(testName, integer(BigInteger.valueOf(blockOfDeployTx.getIndex())));

        List<StackItem> block = response.getInvocationResult().getStack().get(0).getList();
        assertThat(block.get(0).getHexString(), is(reverseHexString(ct.getBlockHashOfDeployTx().toString())));
        assertThat(block.get(1).getInteger().intValue(), is(blockOfDeployTx.getVersion()));
        assertThat(block.get(2).getHexString(), is(reverseHexString(blockOfDeployTx.getPrevBlockHash().toString())));
        assertThat(block.get(3).getHexString(), is(reverseHexString(blockOfDeployTx.getMerkleRootHash().toString())));
        assertThat(block.get(4).getInteger().longValue(), is(blockOfDeployTx.getTime()));
        assertThat(block.get(5).getInteger(), is(greaterThanOrEqualTo(BigInteger.ZERO)));
        assertThat(block.get(6).getInteger().longValue(), is(blockOfDeployTx.getIndex()));
        assertThat(block.get(7).getInteger().intValue(), is(blockOfDeployTx.getPrimary()));
        assertThat(block.get(8).getAddress(), is(blockOfDeployTx.getNextConsensus()));
        assertThat(block.get(9).getInteger().intValue(), is(blockOfDeployTx.getTransactions().size()));
    }

    @Test
    public void currentIndex() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName);
        BigInteger height = response.getInvocationResult().getStack().get(0).getInteger();
        assertThat(height.intValue(), greaterThanOrEqualTo(0));
    }

    @Test
    public void currentHash() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName);
        byte[] hash = response.getInvocationResult().getStack().get(0).getByteArray();
        assertThat(hash.length, is(32));
    }

    @Test
    public void getHash() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName);
        assertThat(response.getInvocationResult().getStack().get(0).getHexString(),
                is(reverseHexString(ledgerContractHash())));
    }

    static class LedgerContractIntegrationTestContract {

        public static int setup() {
            return 1;
        }

        public static byte getTransactionVMState(Hash256 hash) {
            return LedgerContract.getTransactionVMState(hash);
        }

        public static int getTransactionHeight(Hash256 blockHash) {
            return LedgerContract.getTransactionHeight(blockHash);
        }

        public static Object getTransactionFromBlock(int blockNr, int txNr) {
            return LedgerContract.getTransactionFromBlock(blockNr, txNr);
        }

        public static Transaction getTransactionFromBlockWithBlockHash(Hash256 blockHash, int txNr) {
            return LedgerContract.getTransactionFromBlock(blockHash, txNr);
        }

        public static Signer[] getTransactionSigners(Hash256 txHash) {
            return LedgerContract.getTransactionSigners(txHash);
        }

        public static Signer getTransactionSigner(Hash256 txHash, int index) {
            return LedgerContract.getTransactionSigners(txHash)[index];
        }

        public static ByteString getTransactionSignerSerialized(Hash256 txHash, int index) {
            return LedgerContract.getTransactionSigners(txHash)[index].serialized;
        }

        public static Hash160 getTransactionSignerAccount(Hash256 txHash, int index) {
            return LedgerContract.getTransactionSigners(txHash)[index].account;
        }

        public static byte getTransactionSignerWitnessScope(Hash256 txHash, int index) {
            return LedgerContract.getTransactionSigners(txHash)[index].witnessScopes;
        }

        public static Hash160[] getTransactionSignerAllowedContracts(Hash256 txHash, int index) {
            return LedgerContract.getTransactionSigners(txHash)[index].allowedContracts;
        }

        public static ECPoint[] getTransactionSignerAllowedGroups(Hash256 txHash, int index) {
            return LedgerContract.getTransactionSigners(txHash)[index].allowedGroups;
        }

        public static io.neow3j.devpack.WitnessRule[] getTransactionSignerWitnessRules(Hash256 txHash, int index) {
            return LedgerContract.getTransactionSigners(txHash)[index].witnessRules;
        }

        public static Transaction getTransaction(Hash256 txHash) {
            return LedgerContract.getTransaction(txHash);
        }

        public static boolean getNonExistentTransaction(Hash256 txHash) {
            if (LedgerContract.getTransaction(txHash) == null) {
                return true;
            }
            return false;
        }

        public static byte getTransactionState(Hash256 txHash) {
            return LedgerContract.getTransactionVMState(txHash);
        }

        public static Block getBlockWithBlockHash(Hash256 blockHash) {
            return LedgerContract.getBlock(blockHash);
        }

        public static Block getBlockWithBlockNumber(int blockNr) {
            return LedgerContract.getBlock(blockNr);
        }

        public static int currentIndex() {
            return LedgerContract.currentIndex();
        }

        public static Hash256 currentHash() {
            return LedgerContract.currentHash();
        }

        public static Hash160 getHash() {
            return LedgerContract.getHash();
        }
    }

}
