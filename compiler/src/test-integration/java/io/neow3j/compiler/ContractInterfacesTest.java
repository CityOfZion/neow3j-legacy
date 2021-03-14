package io.neow3j.compiler;

import static io.neow3j.TestProperties.neoTokenHash;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import io.neow3j.contract.NeoToken;
import io.neow3j.devpack.ContractInterface;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.annotations.ContractHash;
import io.neow3j.protocol.core.methods.response.NeoInvokeFunction;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContractInterfacesTest extends ContractTest {

    @BeforeClass
    public static void setUp() throws Throwable {
        setUp(ContractInterfacesTestContract.class.getName());
    }

    @Test
    public void callSymbolMethodOfCustomNeoContractInterface() throws IOException {
        NeoInvokeFunction response = callInvokeFunction();
        assertThat(response.getInvocationResult().getStack().get(0).getString(), is("NEO"));
    }

    @Test
    public void getScriptHashOfCustomNeoContractInterface() throws IOException {
        NeoInvokeFunction response = callInvokeFunction();
        assertThat(response.getInvocationResult().getStack().get(0).getHexString(),
                is(neoTokenHash()));
    }

    static class ContractInterfacesTestContract {

        public static String callSymbolMethodOfCustomNeoContractInterface() {
            return CustomNeoToken.symbol();
        }

        public static Hash160 getScriptHashOfCustomNeoContractInterface() {
            return CustomNeoToken.getHash();
        }

    }

    @ContractHash("ef4073a0f2b305a38ec4050e4d3d28bc40ea63f5") // NEO script hash
    static class CustomNeoToken extends ContractInterface {

        public static native String symbol();
    }
}
