package io.neow3j.compiler;

import io.neow3j.compiler.utils.ContractTestRule;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.StringLiteralHelper;
import io.neow3j.devpack.annotations.OnVerification;
import io.neow3j.protocol.core.methods.response.NeoInvokeContractVerify;
import io.neow3j.transaction.Signer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static io.neow3j.contract.ContractParameter.string;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerificationMethodIntegrationTest {

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static ContractTestRule ct = new ContractTestRule(
            VerificationMethodIntegrationTestContract.class.getName());

    @BeforeClass
    public static void setUp() throws Throwable {
        // The RPC method invokecontractverify requires an open wallet on the neo-node.
        ct.getNeow3j().openWallet("wallet.json", "neo").send();
    }

    @Test
    public void callVerifyWithContractOwner() throws Throwable {
        NeoInvokeContractVerify response = ct.getNeow3j()
                .invokeContractVerify(ct.getContract().getScriptHash(),
                        singletonList(string("hello, world!")),
                        Signer.calledByEntry(ct.getDefaultAccount().getScriptHash()))
                .send();

        assertTrue(response.getInvocationResult().getStack().get(0).getBoolean());
    }

    @Test
    public void callVerifyWithContractOwner_fromString() throws Throwable {
        NeoInvokeContractVerify response = ct.getNeow3j()
                .invokeContractVerify(ct.getContract().getScriptHash().toString(),
                        singletonList(string("hello, world!")),
                        Signer.calledByEntry(ct.getDefaultAccount().getScriptHash()))
                .send();

        assertTrue(response.getInvocationResult().getStack().get(0).getBoolean());
    }

    @Test
    public void callVerifyWithOtherSigner() throws Throwable {
        NeoInvokeContractVerify response = ct.getNeow3j().invokeContractVerify(
                ct.getContract().getScriptHash(),
                singletonList(string("hello, world!")),
                Signer.calledByEntry(ct.getCommittee().getScriptHash()))
                .send();

        assertFalse(response.getInvocationResult().getStack().get(0).getBoolean());
    }

    @Test
    public void callVerifyWithOtherSigner_fromString() throws Throwable {
        NeoInvokeContractVerify response = ct.getNeow3j()
                .invokeContractVerify(ct.getContract().getScriptHash().toString(),
                        singletonList(string("hello, world!")),
                        Signer.calledByEntry(ct.getCommittee().getScriptHash()))
                .send();

        assertFalse(response.getInvocationResult().getStack().get(0).getBoolean());
    }

    static class VerificationMethodIntegrationTestContract {

        // default account
        static Hash160 ownerScriptHash =
                StringLiteralHelper.addressToScriptHash("NUrPrFLETzoe7N2FLi2dqTvLwc9L2Em84K");

        @OnVerification
        public static boolean verify(String s) {
            return Runtime.checkWitness(ownerScriptHash);
        }

    }

}
