package io.neow3j.compiler;

import io.neow3j.compiler.utils.ContractTestRule;
import io.neow3j.protocol.core.methods.response.NeoInvokeFunction;
import io.neow3j.protocol.core.methods.response.StackItem;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.List;

import static io.neow3j.contract.ContractParameter.integer;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArithmeticOperatorsTest {

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static ContractTestRule ct = new ContractTestRule(
            ArithmeticOperatorsTestContract.class.getName());

    @Test
    public void allOperators() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, integer(-100),
                integer(30));

        List<StackItem> array = response.getInvocationResult().getStack().get(0).getList();
        assertThat(array.get(0).getInteger().intValue(), is(-70));
        assertThat(array.get(1).getInteger().intValue(), is(-130));
        assertThat(array.get(2).getInteger().intValue(), is(-3000));
        assertThat(array.get(3).getInteger().intValue(), is(-3));
        assertThat(array.get(4).getInteger().intValue(), is(-10));
    }

    @Test
    public void allAssignmentOperators() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, integer(-100));

        List<StackItem> array = response.getInvocationResult().getStack().get(0).getList();
        assertThat(array.get(0).getInteger().intValue(), is(-90));
        assertThat(array.get(1).getInteger().intValue(), is(110));
        assertThat(array.get(2).getInteger().intValue(), is(-1000));
        assertThat(array.get(3).getInteger().intValue(), is(0));
        assertThat(array.get(4).getInteger().intValue(), is(10));
    }

    @Test
    public void addAndIncrement() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, integer(-100),
                integer(100));

        List<StackItem> array = response.getInvocationResult().getStack().get(0).getList();
        assertThat(array.get(0).getInteger().intValue(), is(0));
        assertThat(array.get(1).getInteger().intValue(), is(-99));
        assertThat(array.get(2).getInteger().intValue(), is(100));
    }

    @Test
    public void incrementAndAdd() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, integer(-100),
                integer(100));

        List<StackItem> array = response.getInvocationResult().getStack().get(0).getList();
        assertThat(array.get(0).getInteger().intValue(), is(1));
        assertThat(array.get(1).getInteger().intValue(), is(-99));
        assertThat(array.get(2).getInteger().intValue(), is(100));
    }

    @Test
    public void subtractAndDecrement() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, integer(-100),
                integer(100));

        List<StackItem> array = response.getInvocationResult().getStack().get(0).getList();
        assertThat(array.get(0).getInteger().intValue(), is(-200));
        assertThat(array.get(1).getInteger().intValue(), is(-101));
        assertThat(array.get(2).getInteger().intValue(), is(100));
    }

    @Test
    public void decrementAndSubtract() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, integer(-100),
                integer(100));

        List<StackItem> array = response.getInvocationResult().getStack().get(0).getList();
        assertThat(array.get(0).getInteger().intValue(), is(-201));
        assertThat(array.get(1).getInteger().intValue(), is(-101));
        assertThat(array.get(2).getInteger().intValue(), is(100));
    }

    static class ArithmeticOperatorsTestContract {

        public static int[] allOperators(int i, int j) {
            int[] arr = new int[5];
            arr[0] = i + j;
            arr[1] = i - j;
            arr[2] = i * j;
            arr[3] = i / j;
            arr[4] = i % j;
            return arr;
        }

        public static int[] allAssignmentOperators(int i) {
            int[] arr = new int[]{10, 10, 10, 10, 10};
            arr[0] += i;
            arr[1] -= i;
            arr[2] *= i;
            arr[3] /= i;
            arr[4] %= i;
            return arr;
        }

        public static int[] addAndIncrement(int i1, int i2) {
            int i = i1++ + i2;
            return new int[]{i, i1, i2};
        }

        public static int[] incrementAndAdd(int i1, int i2) {
            int i = ++i1 + i2;
            return new int[]{i, i1, i2};
        }

        public static int[] subtractAndDecrement(int i1, int i2) {
            int i = i1-- - i2;
            return new int[]{i, i1, i2};
        }

        public static int[] decrementAndSubtract(int i1, int i2) {
            int i = --i1 - i2;
            return new int[]{i, i1, i2};
        }

    }

}