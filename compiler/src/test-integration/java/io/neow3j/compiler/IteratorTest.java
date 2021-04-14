package io.neow3j.compiler;

import io.neow3j.devpack.Iterator;
import io.neow3j.devpack.Map;
import io.neow3j.devpack.Map.Entry;
import io.neow3j.protocol.core.methods.response.NeoInvokeFunction;
import io.neow3j.protocol.core.methods.response.StackItem;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.List;

import static io.neow3j.contract.ContractParameter.array;
import static io.neow3j.contract.ContractParameter.integer;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IteratorTest {

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static ContractTestRule ct = new ContractTestRule(
            IteratorTestContract.class.getName());

    @Test
    public void createIteratorFromArrayAndIterateThrough() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName, array(integer(0), integer(1),
                integer(2)));
        List<StackItem> arr = response.getInvocationResult().getStack().get(0).getList();
        assertThat(arr.size(), is(3));
        assertThat(arr.get(0).getInteger().intValue(), is(0));
        assertThat(arr.get(1).getInteger().intValue(), is(1));
        assertThat(arr.get(2).getInteger().intValue(), is(2));
    }

    @Test
    public void createIteratorFromMapAndIterateThrough() throws IOException {
        NeoInvokeFunction response = ct.callInvokeFunction(testName,
                array(integer(3), integer(6), integer(9)),
                array(integer(3), integer(4), integer(5)));

        List<StackItem> arr = response.getInvocationResult().getStack().get(0).getList();
        assertThat(arr.get(0).getInteger().intValue(), is(3));
        assertThat(arr.get(1).getInteger().intValue(), is(3));
        assertThat(arr.get(2).getInteger().intValue(), is(6));
        assertThat(arr.get(3).getInteger().intValue(), is(4));
        assertThat(arr.get(4).getInteger().intValue(), is(9));
        assertThat(arr.get(5).getInteger().intValue(), is(5));
    }

    static class IteratorTestContract {

        public static int[] createIteratorFromArrayAndIterateThrough(Integer[] ints) {
            Iterator<Integer> it = Iterator.create(ints);

            int[] values = new int[ints.length];
            int i = 0;
            while (it.next()) {
                values[i++] = it.get();
            }
            return values;
        }

        public static int[] createIteratorFromMapAndIterateThrough(Integer[] ints1,
                Integer[] ints2) {
            Map<Integer, Integer> map = new Map<>();
            for (int i = 0; i < ints1.length; i++) {
                map.put(ints1[i], ints2[i]);
            }
            Iterator<Entry<Integer, Integer>> it = Iterator.create(map);

            int[] keysAndValues = new int[ints1.length * 2];
            int i = 0;
            while (it.next()) {
                keysAndValues[i++] = it.get().key;
                keysAndValues[i++] = it.get().value;
            }
            return keysAndValues;
        }

    }

}
