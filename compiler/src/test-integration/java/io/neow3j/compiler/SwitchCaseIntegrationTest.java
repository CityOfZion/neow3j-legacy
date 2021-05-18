package io.neow3j.compiler;

import io.neow3j.devpack.Runtime;
import io.neow3j.protocol.core.methods.response.InvocationResult;
import io.neow3j.protocol.core.methods.response.StackItem;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.List;

import static io.neow3j.contract.ContractParameter.integer;
import static io.neow3j.contract.ContractParameter.string;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SwitchCaseIntegrationTest {

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static ContractTestRule ct = new ContractTestRule(
            SwitchCaseIntegrationTestContract.class.getName());

    @Test
    public void switchWithString() throws IOException {
        InvocationResult r = ct.callInvokeFunction(testName, string("world")).getInvocationResult();
        List<StackItem> retVal = r.getStack().get(0).getList();
        assertThat(retVal.get(0).getInteger().intValue(), is(2));
        assertThat(retVal.get(1).getString(), is("isWorld"));

        r = ct.callInvokeFunction(testName, string("defaultCase")).getInvocationResult();
        retVal = r.getStack().get(0).getList();
        assertThat(retVal.get(0).getInteger().intValue(), is(4));
        assertThat(retVal.get(1).getString(), is("isDefault"));
    }

    @Test
    public void switchWithInt() throws IOException {
        InvocationResult r = ct.callInvokeFunction(testName, integer(500)).getInvocationResult();
        List<StackItem> retVal = r.getStack().get(0).getList();
        assertThat(retVal.get(0).getInteger().intValue(), is(3));
        assertThat(retVal.get(1).getString(), is("isOtherCase"));

        r = ct.callInvokeFunction(testName, integer(-10)).getInvocationResult();
        retVal = r.getStack().get(0).getList();
        assertThat(retVal.get(0).getInteger().intValue(), is(4));
        assertThat(retVal.get(1).getString(), is("isDefault"));
    }

    @Test
    public void complexSwitch() throws IOException {
        InvocationResult r = ct.callInvokeFunction(testName, string("hello"), integer(100))
                .getInvocationResult();
        List<StackItem> retVal = r.getStack().get(0).getList();
        assertThat(retVal.get(0).getInteger().intValue(), is(100));
        assertThat(retVal.get(1).getString(), containsString("isHello"));

        r = ct.callInvokeFunction(testName, string("hello"), integer(-10)).getInvocationResult();
        retVal = r.getStack().get(0).getList();
        assertThat(retVal.get(0).getInteger().intValue(), is(1000));
        assertThat(retVal.get(1).getString(), is("exceptionCaught"));
    }

    static class SwitchCaseIntegrationTestContract {

        public static Object[] switchWithString(String s) {
            int localInt = 0;
            String localString = "not set";
            switch (s) {
                case "hello":
                    localInt = 1;
                    localString = "isHello";
                    break;
                case "world":
                    localInt = 2;
                    localString = "isWorld";
                    break;
                case "otherCase":
                    localInt = 3;
                    localString = "isOtherCase";
                    break;
                default:
                    localInt = 4;
                    localString = "isDefault";
            }
            return new Object[]{localInt, localString};
        }

        public static Object[] switchWithInt(int i) {
            int localInt = 0;
            String localString = "not set";
            switch (i) {
                case 1:
                    localInt = 1;
                    localString = "isHello";
                    break;
                case 10:
                    localInt = 2;
                    localString = "isWorld";
                    break;
                case 500:
                    localInt = 3;
                    localString = "isOtherCase";
                    break;
                default:
                    localInt = 4;
                    localString = "isDefault";
            }
            return new Object[]{localInt, localString};
        }

        public static Object[] complexSwitch(String s, int i) throws Exception {
            int localInt = 0;
            String localString = "not set";
            switch (s) {
                case "hello":
                    if (i == 10) {
                        localInt = 1;
                        localString = "isHello";
                    } else {
                        try {
                            switch (i) {
                                case 20:
                                    localInt = 20;
                                    localString = "is" + "Hello" + Runtime.getPlatform();
                                    break;
                                case 100:
                                    localInt = 100;
                                    localString = "is" + "Hello" +
                                            Runtime.getCallingScriptHash().asByteString().toString();
                                    break;
                                default:
                                    throw new Exception();
                            }
                        } catch (Exception e) {
                            localInt = 1000;
                            localString = "exceptionCaught";
                        }
                    }
                    break;
                case "world":
                    localInt = 2;
                    localString = "isWorld";
                    break;
                case "otherCase":
                    localInt = 3;
                    localString = "isOtherCase";
                    break;
                case "yetAnotherCase":
                    localInt = i * 3 + 5;
                    localString = "is" + "Yet" + "Another" + "Case";
                    break;
                default:
                    localInt = 4;
                    localString = "isDefault";
            }
            return new Object[]{localInt, localString};
        }

    }
}
